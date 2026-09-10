(ns mgmtanalyst.ledger-test
  (:require [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.ledger :as led]))

(defn- built []
  (-> []
      (led/append (led/commit-entry {:op :approve-recommendation} :actor))
      (led/append (led/hold-entry {:hard? true :violations [{:rule :fabricated-metric}]}))
      (led/append (led/commit-entry {:op :publish-recommendation} :human))))

(deftest a-clean-chain-verifies
  (let [l (built)]
    (is (= 3 (count l)))
    (is (= {:ok? true :length 3} (led/verify l)))
    (is (= [0 1 2] (mapv :ledger/seq l)))
    (is (= 0 (:ledger/prev (first l))))
    (testing "each entry commits to the previous hash"
      (is (= (:ledger/hash (nth l 0)) (:ledger/prev (nth l 1))))
      (is (= (:ledger/hash (nth l 1)) (:ledger/prev (nth l 2)))))))

(deftest an-edited-entry-is-detected
  (testing "the property an unchained vector could not have"
    (let [l (built)
          tampered (assoc-in l [1 :verdict :hard?] false)
          v (led/verify tampered)]
      (is (not (:ok? v)))
      (is (= 1 (:broken-at v)))
      (is (= :hash-mismatch (:reason v))))))

(deftest a-reordered-ledger-is-detected
  (let [l (built)
        swapped (assoc l 0 (nth l 1) 1 (nth l 0))
        v (led/verify swapped)]
    (is (not (:ok? v)))
    (is (= :seq-mismatch (:reason v)))))

(deftest a-dropped-middle-entry-is-detected
  (let [l (built)
        dropped (vec (concat [(nth l 0)] [(nth l 2)]))
        v (led/verify dropped)]
    (is (not (:ok? v)))
    (is (= 1 (:broken-at v)))
    (is (= :seq-mismatch (:reason v)))))

(deftest truncation-at-the-END-is-NOT-detected
  (testing "stated as a limit rather than left for a reader to discover:
            a chain cannot detect entries it never saw"
    (let [l (built)
          truncated (vec (butlast l))]
      (is (:ok? (led/verify truncated)))
      (is (= 2 (:length (led/verify truncated)))))))

(deftest an-empty-ledger-verifies
  (is (= {:ok? true :length 0} (led/verify []))))

(deftest approval-provenance-is-recorded-on-both-shapes
  (testing "an automatic write records :actor explicitly rather than omitting the key,
            so an absent field cannot be mistaken for an unaudited one"
    (is (= :actor (:approved-by (led/commit-entry {} :actor))))
    (is (= :human (:approved-by (led/commit-entry {} :human))))
    (is (= :none (:approved-by (led/hold-entry {}))))
    (is (contains? (led/commit-entry {} :actor) :approved-by))))

(deftest the-hash-is-deterministic-and-position-dependent
  (is (= (led/chain-hash 0 {:a 1}) (led/chain-hash 0 {:a 1})))
  (is (not= (led/chain-hash 0 {:a 1}) (led/chain-hash 1 {:a 1})))
  (is (not= (led/chain-hash 0 {:a 1}) (led/chain-hash 0 {:a 2})))
  (testing "stays inside the exactly-representable integer range on both hosts"
    (doseq [h [(led/chain-hash 0 {:a 1}) (led/chain-hash 2147483646 {:b "x"})]]
      (is (<= 0 h 2147483646)))))

(deftest summary-names-who-approved-each-write
  (let [s (led/summary (built))]
    (is (re-find #"approved-by=actor" s))
    (is (re-find #"approved-by=human" s))
    (is (re-find #"approved-by=none" s))))
