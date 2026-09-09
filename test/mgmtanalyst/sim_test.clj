(ns mgmtanalyst.sim-test
  (:require [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.phase :as phase]
            [mgmtanalyst.sim :as sim]))

(deftest the-scenario-table-passes
  (let [r (sim/run)]
    (is (:ok? r) (sim/report r))
    (is (empty? (:mismatches r)))
    (is (empty? (:wrote-anyway r)))
    (is (empty? (:ledger-breaks r)))))

(deftest the-table-demonstrates-refusals
  (testing "a harness that refuses nothing has demonstrated nothing"
    (is (pos? (:refusals (sim/run))))))

(deftest the-table-also-demonstrates-an-admission
  (testing "a governor that refuses everything is not governing either"
    (let [r (sim/run)
          admitted (filter #(= :commit (:actual %)) (:results r))]
      (is (pos? (count admitted))))))

(deftest no-refusal-ever-wrote-a-record
  (let [r (sim/run)]
    (doseq [s (:results r)]
      (when (:refusal? s)
        (is (not (:wrote? s))
            (str (:name s) " refused but a record was written anyway"))))))

(deftest every-ledger-left-behind-verifies
  (doseq [s (:results (sim/run))]
    (is (:ok? (:ledger-verify s)) (str (:name s) " left a broken ledger"))))

(deftest every-scenario-carries-a-reason
  (doseq [s sim/scenarios]
    (is (string? (:why s)) (str (:name s) " must say why"))
    (is (contains? #{:hold :request-approval :commit} (:expect s))
        (str (:name s) " must expect a declared phase"))))

(deftest scenario-names-are-unique
  (let [ns- (map :name sim/scenarios)]
    (is (= (count ns-) (count (set ns-))))))

(deftest ok?-requires-a-demonstrated-refusal
  ;; `run` computes :ok? itself, so testing `report` on a hand-built map does
  ;; not cover the conjunct that makes a refusal-free table a failure. Run the
  ;; harness on a table containing only admissions: every other column is
  ;; clean, and the result must still not be ok.
  (let [clean-only (filterv #(= :commit (:expect %)) sim/scenarios)
        r (sim/run clean-only)]
    (is (pos? (count clean-only)) "the fixture itself must be non-empty")
    (is (zero? (:refusals r)))
    (is (empty? (:mismatches r)) "the scenarios still reach the phase they expect")
    (is (empty? (:wrote-anyway r)))
    (is (empty? (:ledger-breaks r)))
    (testing "clean on every other axis, and still not a pass"
      (is (not (:ok? r))))
    (is (re-find #"REFUSING TO REPORT A PASS" (sim/report r)))))

(deftest report-refuses-to-pass-an-empty-table
  (testing "zero refusals is reported as a defect in the table, not as a pass"
    (let [txt (sim/report {:results [] :refusals 0 :mismatches []
                           :wrote-anyway [] :ledger-breaks [] :ok? false})]
      (is (re-find #"REFUSING TO REPORT A PASS" txt)))))

(deftest expected-refusals-really-are-refusals
  (doseq [s sim/scenarios
          :when (not= :commit (:expect s))]
    (is (phase/refusal? (:expect s))
        (str (:name s) " expects a phase that writes"))))
