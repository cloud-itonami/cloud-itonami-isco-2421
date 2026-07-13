(ns mgmtanalyst.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.store :as store]
            [mgmtanalyst.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Trade"})
    (store/register-engagement! st {:engagement-id "E-1" :client-id "client-1"
                                    :name "ops-review-2026"
                                    :baseline-metrics #{"cycle-time" "headcount" "opex"}
                                    :max-claimed-savings-pct 15})
    st))

(defn- rec [metrics savings]
  {:op :approve-recommendation :effect :propose :engagement-id "E-1"
   :cited-metrics metrics :claimed-savings-pct savings
   :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-registered-metrics-and-within-ceiling
  (let [st (fresh-store)
        v (governor/check req {} (rec #{"cycle-time" "opex"} 10) st)]
    (is (:ok? v))))

(deftest ok-at-exact-ceiling
  (testing "a savings claim exactly at the ceiling is within margin"
    (let [st (fresh-store)
          v (governor/check req {} (rec #{"opex"} 15) st)]
      (is (:ok? v)))))

(deftest hard-on-fabricated-metric
  (testing "evidence fabrication is not permitted"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (rec #{"cycle-time" "customer-nps"} 10)
                                          :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :fabricated-metric (:rule %)) (:violations v))))))

(deftest hard-on-savings-claim-exceeds-ceiling
  (testing "savings-claim arithmetic is not a marketing decision"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (rec #{"opex"} 40) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :savings-claim-exceeds-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-unknown-engagement
  (let [st (fresh-store)
        v (governor/check req {} (assoc (rec #{"opex"} 10) :engagement-id "E-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-engagement (:rule %)) (:violations v)))))

(deftest hard-on-foreign-engagement
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (rec #{"opex"} 10) st)]
      (is (:hard? v))
      (is (some #(= :engagement-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (rec #{"opex"} 10) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (rec #{"opex"} 10) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-recommendation-publication
  (let [st (fresh-store)
        v (governor/check req {} {:op :publish-recommendation :effect :propose
                                  :engagement-id "E-1" :cited-metrics #{"opex"}
                                  :claimed-savings-pct 10 :confidence 0.9 :stake :high} st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (rec #{"opex"} 10) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
