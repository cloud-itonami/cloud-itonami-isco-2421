(ns mgmtanalyst.governor
  "ManagementOrganizationAnalystsGovernor — the independent safety/
  traceability layer for the ISCO-08 2421 community management &
  organization analysts actor (itonami actor pattern, ADR-2607011000 /
  CLAUDE.md Actors section). Consulting twist: a recommendation's cited
  metrics must all be members of the registered baseline-metrics set (no
  fabricated evidence), and a claimed savings percentage is arithmetic
  comparison against the registered ceiling — an aggressive savings pitch is
  not something the governor can second-guess analytically, but it CAN cap the
  claim at the registered ceiling.

  The operation vocabulary lives in `mgmtanalyst.operation` and the
  well-formedness of the values lives in `mgmtanalyst.facts`. This namespace
  decides; it does not also define what there is to decide about. Before that
  split it did both, and the result was a denylist: two named ops were bound
  and everything else — including `nil` — was admitted clean. The measurements
  are in those two docstrings.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. declared operation  — the proposal's :op must be in the declared
                           vocabulary. Undeclared is a vocabulary error.
    2. reserved operation  — a declared-but-reserved op names authority this
                           actor does not hold. Never escalatable.
    3. client provenance   — the record the store returned must identify the
                           client the request asked for.
    4. no-actuation        — proposal :effect must be :propose.
    5. usable confidence   — :confidence must be a number on [0,1], because
                           the escalation floor only bounds a quantity on the
                           scale the floor is stated in.
    6. engagement basis    — an engagement-bound op must cite a REGISTERED,
                           WELL-FORMED engagement, and that engagement must
                           belong to the client the request names.
    7. metric-citation membership — every metric cited by an engagement-bound
                           op must be a member of the engagement's registered
                           :baseline-metrics set, and it must cite at least
                           one (no fabricated and no absent evidence).
    8. savings-claim ceiling — the proposed claimed-savings-pct must be
                           readable AND must not exceed the engagement's
                           registered :max-claimed-savings-pct.
  ESCALATION invariants (:escalate? true, human sign-off):
    9. an operation declared `:escalates?` (external delivery, risk flags).
   10. low confidence (< `confidence-floor`).

  Invariants 6-8 apply to every engagement-bound operation, which is the fix
  for the exemption measured in `mgmtanalyst.operation`: they used to be gated
  on `(= :approve-recommendation op)`, so `:publish-recommendation` — external
  delivery to client leadership — reached a human carrying a fabricated metric
  and a 900% savings claim with an empty violation list."
  (:require [clojure.set :as set]
            [mgmtanalyst.facts :as facts]
            [mgmtanalyst.operation :as operation]
            [mgmtanalyst.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [request proposal client-record e]
  (let [{:keys [op cited-metrics claimed-savings-pct]} proposal
        declared?   (operation/declared? op)
        reserved?   (operation/reserved? op)
        bound?      (operation/engagement-op? op)
        client-bad  (facts/client-defect client-record (:client-id request))
        conf-bad    (facts/confidence-defect (:confidence proposal))
        e-bad       (when bound? (facts/engagement-defect e))
        ;; Ownership is a relation between the engagement and the request, so
        ;; it is not a well-formedness fact and does not live in
        ;; `mgmtanalyst.facts`. It is checked for every engagement-bound op,
        ;; not just approvals — pre-change it was gated on
        ;; `:approve-recommendation`, so delivering against another client's
        ;; engagement escalated to a human with an empty violation list.
        wrong-client? (and bound? (nil? e-bad)
                           (not= (:client-id e) (:client-id request)))
        metrics-bad (when (and bound? (nil? e-bad)) (facts/cited-metrics-defect cited-metrics))
        savings-bad (when (and bound? (nil? e-bad)) (facts/claimed-savings-defect claimed-savings-pct))
        ;; Only computed once the engagement and the citation set are both
        ;; known well-formed, so a membership answer is never derived from a
        ;; set nobody registered.
        invented    (when (and bound? (nil? e-bad) (nil? metrics-bad))
                      (set/difference (set cited-metrics) (:baseline-metrics e)))]
    (cond-> []
      (not declared?)
      (conj {:rule :undeclared-operation
             :detail (str "宣言されていない操作 " (pr-str op)
                          "（語彙外の op は統治できない — mgmtanalyst.operation を参照）")})

      reserved?
      (conj {:rule :reserved-operation
             :detail (str "この actor が持たない権限 " (pr-str op) " — "
                          (operation/reserved-reason op))})

      client-bad
      (conj {:rule client-bad :detail "client provenance: 要求された client を同定できない"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      conf-bad
      (conj {:rule conf-bad
             :detail (str "confidence が比較不能 " (pr-str (:confidence proposal))
                          "（[0,1] の数でなければ escalation floor は床にならない）")})

      e-bad
      (conj {:rule e-bad :detail "engagement が未登録、または統治できる形をしていない"})

      metrics-bad
      (conj {:rule metrics-bad :detail "引用指標の集合が検査可能な形をしていない（証拠ゼロの提言を含む）"})

      savings-bad
      (conj {:rule savings-bad
             :detail (str "削減率主張が比較不能 " (pr-str claimed-savings-pct)
                          "（数でないものは全ての上限を下回ってしまう）")})

      wrong-client?
      (conj {:rule :engagement-wrong-client
             :detail (str "engagement " (pr-str (:engagement-id e))
                          " が別 client のもの（" (pr-str (:client-id e)) "）")})

      (seq invented)
      (conj {:rule :fabricated-metric
             :detail (str "未登録指標を引用 " (vec invented)
                          "（証拠の捏造禁止 — 登録済み baseline-metrics のみ引用可）")})

      (and bound? (nil? e-bad) (nil? savings-bad)
           (> claimed-savings-pct (:max-claimed-savings-pct e)))
      (conj {:rule :savings-claim-exceeds-ceiling
             :detail (str "削減率主張 " claimed-savings-pct "% > 登録済み上限 "
                          (:max-claimed-savings-pct e) "%（削減率算術は営業判断ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `mgmtanalyst.store/Store`. Pure — never mutates the store."
  ;; `_context` is unused: every fact this governor consults comes from the
  ;; request, the proposal, or the store. It stays in the signature because
  ;; the actor pattern's check arity is fixed across this fleet.
  [request _context proposal store]
  (let [client-record (store/client store (:client-id request))
        e (some->> (:engagement-id proposal) (store/engagement store))
        hard (hard-violations request proposal client-record e)
        hard? (boolean (seq hard))
        conf (:confidence proposal)
        ;; A confidence that is not comparable is already a hard violation, so
        ;; it must not ALSO be read as "below the floor" — that would report an
        ;; unreadable value as a merely-cautious one.
        low? (and (nil? (facts/confidence-defect conf)) (< conf confidence-floor))
        risky-op? (operation/escalates? (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
