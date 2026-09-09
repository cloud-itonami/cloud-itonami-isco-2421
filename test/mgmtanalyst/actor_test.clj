(ns mgmtanalyst.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.actor :as actor]
            [mgmtanalyst.ledger :as led]
            [mgmtanalyst.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-engagement! st {:engagement-id "E-1" :client-id "client-1"
                                    :name "ops-review-2026"
                                    :baseline-metrics #{"cycle-time" "opex"}
                                    :max-claimed-savings-pct 15})
    st))

(deftest commits-an-evidence-backed-in-ceiling-recommendation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-recommendation :stake :low
                 :engagement-id "E-1" :cited-metrics #{"opex"} :claimed-savings-pct 10}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-ceiling-recommendation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-recommendation :stake :low
                 :engagement-id "E-1" :cited-metrics #{"opex"} :claimed-savings-pct 50}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-publishes-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :publish-recommendation :stake :high
                 :engagement-id "E-1" :cited-metrics #{"opex"} :claimed-savings-pct 10}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))

(deftest the-ledger-distinguishes-a-human-approved-write-from-an-automatic-one
  ;; This is the property `mgmtanalyst.ledger` exists to provide, and it can
  ;; only be shown through the WIRED graph: the provenance is set by the
  ;; :request-approval node, which is reachable only by a human resuming an
  ;; interrupted thread. A unit test over `commit-entry` shows that the field
  ;; can hold :human, not that anything ever puts it there.
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        auto {:client-id "client-1" :op :approve-recommendation :stake :low
              :engagement-id "E-1" :cited-metrics #{"opex"} :claimed-savings-pct 10}
        escalated {:client-id "client-1" :op :publish-recommendation :stake :low
                   :engagement-id "E-1" :cited-metrics #{"opex"} :claimed-savings-pct 10}]
    (actor/run-request! graph auto {} "prov-auto")
    (actor/run-request! graph escalated {} "prov-human")
    (actor/approve! graph "prov-human")
    (let [entries (vec (store/ledger st))
          commits (filterv #(= :commit (:disposition %)) entries)]
      (is (= 2 (count commits)))
      (testing "the automatic write is recorded as :actor"
        (is (= :actor (:approved-by (first commits)))))
      (testing "the resumed write is recorded as :human"
        (is (= :human (:approved-by (second commits)))))
      (testing "the two are distinguishable — pre-change both were bare {:disposition :commit}"
        (is (not= (:approved-by (first commits)) (:approved-by (second commits)))))
      (testing "and the chain the actor wrote verifies"
        (is (:ok? (led/verify entries)))))))

(deftest a-held-run-leaves-a-verifying-chained-ledger-entry
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-recommendation :stake :low
                 :engagement-id "E-1" :cited-metrics #{"invented"} :claimed-savings-pct 10}]
    (actor/run-request! graph request {} "held-1")
    (let [entries (vec (store/ledger st))]
      (is (= 1 (count entries)))
      (is (= :hold (:disposition (first entries))))
      (is (= :none (:approved-by (first entries))))
      (testing "the refusal carries its violations, so the ledger explains itself"
        (is (seq (get-in (first entries) [:verdict :violations]))))
      (is (:ok? (led/verify entries))))))
