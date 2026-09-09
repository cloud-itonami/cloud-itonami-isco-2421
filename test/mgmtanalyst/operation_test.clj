(ns mgmtanalyst.operation-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [mgmtanalyst.operation :as op]))

(deftest supported-and-reserved-are-disjoint
  (testing "an op cannot be both proposable and reserved"
    (is (empty? (set/intersection (set (keys op/supported))
                                          (set (keys op/reserved)))))))

(deftest every-supported-op-declares-its-properties
  (doseq [[k v] op/supported]
    (testing (str k " declares :escalates? and :engagement-op? explicitly")
      (is (contains? v :escalates?) (str k " must state whether it escalates"))
      (is (contains? v :engagement-op?) (str k " must state whether it binds an engagement"))
      (is (boolean? (:escalates? v)))
      (is (boolean? (:engagement-op? v)))
      (is (string? (:summary v))))))

(deftest every-reserved-op-says-why
  (doseq [[k v] op/reserved]
    (is (string? (:reason v)) (str k " must carry a reason a refusal can quote"))
    (is (pos? (count (:reason v))))))

(deftest undeclared-ops-are-not-declared
  (testing "the ops measured as admitted on the pre-change tree are outside the vocabulary"
    (doseq [o [:restructure-everything :grant-partner-access :fabricate-anything nil]]
      (is (not (op/declared? o)) (str o " must not be declared"))
      (is (not (op/supported? o)))))
  (testing "a reserved op IS declared — that is what lets the refusal say why"
    (is (op/declared? :sign-client-contract))
    (is (not (op/supported? :sign-client-contract)))
    (is (op/reserved? :sign-client-contract))))

(deftest publish-binds-an-engagement
  (testing "the exemption this repo shipped with: external delivery must bind"
    (is (op/engagement-op? :publish-recommendation))
    (is (op/engagement-op? :approve-recommendation))
    (is (op/escalates? :publish-recommendation))
    (is (not (op/escalates? :approve-recommendation)))))

(deftest non-binding-ops-bind-nothing
  (is (not (op/engagement-op? :draft-recommendation)))
  (is (not (op/engagement-op? :flag-engagement-risk)))
  (testing "predicates are false — never an admission — for ops the governor blocks first"
    (is (not (op/engagement-op? :sign-client-contract)))
    (is (not (op/escalates? :restructure-everything)))
    (is (not (op/engagement-op? nil)))))

(deftest reserved-reason-is-available-for-every-reserved-op
  (doseq [k (keys op/reserved)]
    (is (string? (op/reserved-reason k))))
  (is (nil? (op/reserved-reason :approve-recommendation))))
