(ns mgmtanalyst.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.facts :as facts]))

(deftest client-provenance-asks-about-the-record-not-the-store
  (testing "the measured hole: {} registered under key nil resolved a request with no client-id"
    (is (= :client/no-requested-id (facts/client-defect {} nil)))
    (is (= :client/record-has-no-id (facts/client-defect {} "c1"))))
  (testing "a record that identifies a different client is not provenance"
    (is (= :client/id-mismatch (facts/client-defect {:client-id "other"} "c1"))))
  (is (= :client/unregistered (facts/client-defect nil "c1")))
  (is (= :client/not-a-record (facts/client-defect "not a map" "c1")))
  (is (nil? (facts/client-defect {:client-id "c1" :name "A"} "c1"))))

(def ^:private well-formed
  {:engagement-id "E-1" :client-id "c1"
   :baseline-metrics #{"opex"} :max-claimed-savings-pct 15})

(deftest engagement-must-be-comparable-against
  (is (nil? (facts/engagement-defect well-formed)))
  (testing "the measured NullPointerException: no registered ceiling"
    (is (= :engagement/no-savings-ceiling
           (facts/engagement-defect (dissoc well-formed :max-claimed-savings-pct)))))
  (testing "a ceiling off the percentage scale cannot bound a percentage"
    (is (= :engagement/savings-ceiling-out-of-range
           (facts/engagement-defect (assoc well-formed :max-claimed-savings-pct 400))))
    (is (= :engagement/savings-ceiling-out-of-range
           (facts/engagement-defect (assoc well-formed :max-claimed-savings-pct -1)))))
  (testing "an engagement with nothing registered to cite cannot support a citation check"
    (is (= :engagement/baseline-metrics-empty
           (facts/engagement-defect (assoc well-formed :baseline-metrics #{}))))
    (is (= :engagement/baseline-metrics-not-a-set
           (facts/engagement-defect (dissoc well-formed :baseline-metrics))))
    (is (= :engagement/baseline-metric-not-a-name
           (facts/engagement-defect (assoc well-formed :baseline-metrics #{"opex" ""})))))
  (is (= :engagement/unregistered (facts/engagement-defect nil)))
  (is (= :engagement/no-id (facts/engagement-defect (dissoc well-formed :engagement-id))))
  (is (= :engagement/no-client-id (facts/engagement-defect (dissoc well-formed :client-id)))))

(deftest confidence-must-be-on-the-scale-the-floor-is-stated-in
  (is (nil? (facts/confidence-defect 0.6)))
  (is (nil? (facts/confidence-defect 0)))
  (is (nil? (facts/confidence-defect 1)))
  (testing "the measured hole: 99.0 bought the advisor out of escalation"
    (is (= :confidence/out-of-range (facts/confidence-defect 99.0))))
  (is (= :confidence/out-of-range (facts/confidence-defect -5)))
  (is (= :confidence/not-a-number (facts/confidence-defect "high")))
  (is (= :confidence/not-a-number (facts/confidence-defect nil)))
  (is (= :confidence/not-a-number (facts/confidence-defect Double/NaN))))

(deftest a-claim-the-governor-cannot-compare-is-not-a-conforming-claim
  (is (nil? (facts/claimed-savings-defect 10)))
  (is (nil? (facts/claimed-savings-defect 0)))
  (testing "the measured hole: a non-number is under every ceiling"
    (is (= :savings/not-a-number (facts/claimed-savings-defect "900")))
    (is (= :savings/not-a-number (facts/claimed-savings-defect nil))))
  (is (= :savings/negative (facts/claimed-savings-defect -1)))
  (is (= :savings/not-a-number (facts/claimed-savings-defect Double/NaN))))

(deftest citing-nothing-is-not-citing-only-registered-metrics
  (is (nil? (facts/cited-metrics-defect #{"opex"})))
  (testing "the measured hole: an evidence-free recommendation passed vacuously"
    (is (= :metrics/none-cited (facts/cited-metrics-defect #{}))))
  (is (= :metrics/not-a-set (facts/cited-metrics-defect nil)))
  (is (= :metrics/not-a-set (facts/cited-metrics-defect ["opex"])))
  (is (= :metrics/not-a-name (facts/cited-metrics-defect #{"opex" "   "}))))
