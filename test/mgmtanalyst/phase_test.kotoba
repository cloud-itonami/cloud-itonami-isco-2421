(ns mgmtanalyst.phase-test
  (:require [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.phase :as phase]))

(deftest hard-outranks-escalate
  (testing "a verdict carrying BOTH flags must hold — a human cannot approve a hard block"
    (is (= :hold (phase/of-verdict {:hard? true :escalate? true}))))
  (is (= :hold (phase/of-verdict {:hard? true})))
  (is (= :request-approval (phase/of-verdict {:escalate? true})))
  (is (= :commit (phase/of-verdict {})))
  (is (= :commit (phase/of-verdict {:hard? false :escalate? false}))))

(deftest only-commit-writes
  (is (phase/writes? :commit))
  (is (not (phase/writes? :hold)))
  (testing ":request-approval is a refusal to act without a human, not an approval-in-waiting"
    (is (not (phase/writes? :request-approval)))
    (is (phase/refusal? :request-approval))
    (is (phase/human-required? :request-approval)))
  (is (phase/refusal? :hold))
  (is (not (phase/refusal? :commit))))

(deftest terminality-is-declared
  (is (phase/terminal? :hold))
  (is (phase/terminal? :commit))
  (testing ":request-approval is not terminal — the thread resumes into :commit"
    (is (not (phase/terminal? :request-approval)))))

(deftest an-unknown-phase-claims-nothing
  (testing "unknown phases must not read as writing or as human-approved"
    (is (not (phase/writes? :no-phase)))
    (is (not (phase/human-required? :no-phase)))
    (is (not (phase/terminal? :no-phase)))))

(deftest approval-provenance-is-recoverable
  (testing "the ledger could not tell a human-approved write from an automatic one"
    (is (phase/approved-commit? :request-approval))
    (is (not (phase/approved-commit? :commit)))
    (is (not (phase/approved-commit? :hold)))))
