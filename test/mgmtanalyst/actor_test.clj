(ns mgmtanalyst.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.actor :as actor]
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
