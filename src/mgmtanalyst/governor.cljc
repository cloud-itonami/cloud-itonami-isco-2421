(ns mgmtanalyst.governor
  "ManagementOrganizationAnalystsGovernor — the independent safety/
  traceability layer for the ISCO-08 2421 community management &
  organization analysts actor (itonami actor pattern,
  ADR-2607011000 / CLAUDE.md Actors section). Modeled on
  cloud-itonami-isco-4311's bookkeeping.governor. Consulting twist: a
  recommendation's cited metrics must all be members of the
  registered baseline-metrics set (no fabricated evidence), and a
  claimed savings percentage is arithmetic comparison against the
  registered ceiling — an aggressive savings pitch is not something
  the governor can second-guess analytically, but it CAN cap the
  claim at the registered ceiling.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose.
    3. engagement basis    — a recommendation approval must cite a
                           REGISTERED engagement belonging to this
                           client.
    4. metric-citation membership — every metric cited by the
                           recommendation must be a member of the
                           engagement's registered :baseline-metrics
                           set (no fabricated evidence).
    5. savings-claim ceiling — the proposed claimed-savings-pct must
                           not exceed the engagement's registered
                           :max-claimed-savings-pct (arithmetic, not
                           a marketing decision).
  ESCALATION invariants (:escalate? true, human sign-off):
    6. :op :publish-recommendation (external delivery to client
                           leadership).
    7. low confidence (< `confidence-floor`)."
  (:require [clojure.set :as set]
            [mgmtanalyst.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [{:keys [request proposal]} client-record e]
  (let [{:keys [op cited-metrics claimed-savings-pct]} proposal
        approve? (= :approve-recommendation op)
        invented (when e (set/difference (set cited-metrics) (:baseline-metrics e)))]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and approve? (nil? e))
      (conj {:rule :unknown-engagement :detail "未登録 engagement への提言承認は不可"})

      (and approve? e (not= (:client-id e) (:client-id request)))
      (conj {:rule :engagement-wrong-client :detail "engagement が別 client のもの"})

      (and approve? e (seq invented))
      (conj {:rule :fabricated-metric
             :detail (str "未登録指標を引用 " (vec invented)
                          "（証拠の捏造禁止 — 登録済み baseline-metrics のみ引用可）")})

      (and approve? e (number? claimed-savings-pct)
           (> claimed-savings-pct (:max-claimed-savings-pct e)))
      (conj {:rule :savings-claim-exceeds-ceiling
             :detail (str "削減率主張 " claimed-savings-pct "% > 登録済み上限 "
                          (:max-claimed-savings-pct e) "%（削減率算術は営業判断ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `mgmtanalyst.store/Store`. Pure — never mutates
  the store."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        e (some->> (:engagement-id proposal) (store/engagement store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record e)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (= :publish-recommendation (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
