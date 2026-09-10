(ns mgmtanalyst.operation
  "The closed vocabulary of operations the ISCO-08 2421 management and
  organization analysts actor may propose.

  Runtime: portable `.cljc` (pure data + pure predicates, no host interop).

  Why this namespace exists. Before it, the operation vocabulary lived in two
  places that could not disagree loudly: the README's prose, and the
  Governor's private `(= :approve-recommendation op)` test plus one named
  escalating op. That made the Governor a *denylist* — it bound two named ops
  and admitted everything else. Measured on the pre-change tree, against the
  registered client `c1`:

      {:op :delete-all-engagement-records :effect :propose :confidence 0.95}
      => {:ok? true :violations []}

  Admitted, and admitted as a *clean* verdict: no escalation, no human, and an
  empty violation list to show a reviewer. `:grant-partner-access`,
  `:disable-audit-logging`, `:sign-client-contract` and `:issue-invoice` were
  admitted the same way, and so was a proposal whose `:op` was `nil`.

  Worse, an undeclared op did not merely bypass its own rule — it bypassed
  every engagement invariant this repo exists to enforce, because all of them
  were gated behind the `:approve-recommendation` test:

      {:op :some-new-op :effect :propose :engagement-id \"e1\"
       :cited-metrics #{\"totally-invented\"} :claimed-savings-pct 999}
      => {:ok? true :violations []}

  An actor whose operation set is open cannot be governed, because the
  governor is answering a question about a vocabulary nobody declared. So the
  vocabulary is declared here, once, as an allowlist, and `mgmtanalyst.governor`
  refuses anything outside it.

  Two disjoint maps:

  * `supported` — what the actor may propose. `:escalates?` and
    `:engagement-op?` are properties of the operation, not of the governor's
    mood, so they live beside it.
  * `reserved` — operations naming authority this cognitive actor does not
    hold: contractual commitment, billing, access beyond the engagement's
    registered scope, and removal of the audit trail. These are *declared*
    rather than merely absent so the refusal can say why. An undeclared op is
    a vocabulary error; a reserved op is an authority boundary. Conflating
    them would let a future edit `supported`-list one of them by accident.

  `:engagement-op?` is the field that closes the gap this repo shipped with.
  The metric-citation invariant and the savings-claim ceiling were both gated
  on `(= :approve-recommendation op)`, so `:publish-recommendation` — the
  operation whose entire purpose is external delivery to client leadership —
  was exempt from both. Measured on the pre-change tree, the SAME fabricated
  metric and the SAME over-ceiling claim:

      {:op :approve-recommendation ...}
      => {:hard? true  :violations [:fabricated-metric :savings-claim-exceeds-ceiling]}
      {:op :publish-recommendation ...}
      => {:hard? false :escalate? true :violations []}

  and, against an engagement that was never registered at all:

      {:op :publish-recommendation :engagement-id \"no-such\" ...}
      => {:hard? false :escalate? true :violations []}

  The escalating op reached a human with an **empty violation list**, on the
  one operation whose premise is that a claim is about to leave the firm.
  Binding is a property of the operation, so it is declared here and the
  governor reads it, rather than the governor naming one op and forgetting the
  more consequential one."
  )

(def supported
  "Operations the actor may propose.

  `:escalates?` true means human sign-off is required regardless of advisor
  confidence. `:engagement-op?` true means the proposal binds to a REGISTERED
  engagement, and therefore must satisfy every registered fact about it — the
  baseline-metrics membership of every cited metric and the arithmetic ceiling
  on the claimed savings percentage."
  {:draft-recommendation
   {:escalates? false
    :engagement-op? false
    :summary "draft an internal recommendation for the engagement partner (binds nothing, leaves the firm nowhere)"}

   :approve-recommendation
   {:escalates? false
    :engagement-op? true
    :summary "approve a recommendation against a registered engagement"}

   :publish-recommendation
   {:escalates? true
    :engagement-op? true
    :summary "deliver a recommendation to client leadership"}

   :flag-engagement-risk
   {:escalates? true
    :engagement-op? false
    :summary "surface a suspected risk to the engagement partner"}})

(def reserved
  "Operations reserved to someone this actor is not. Naming one in a proposal
  is a permanent hard block, never an escalation: escalation would imply a
  human could approve the *actor* doing it, and neither an engagement partner
  nor an operator can delegate a contractual commitment, a billing act, an
  access grant, or the removal of an audit trail to a remote cognitive actor.

  This is the machine-readable form of the scope sentence the README has
  carried since the repo was created — the advisor only proposes. Prose in a
  README does not refuse anything."
  {:sign-client-contract
   {:reason "committing the firm contractually is the engagement partner's authority, exercised by a signatory"}

   :issue-invoice
   {:reason "billing is a commercial act, not an analytical recommendation, and is never a consequence of one"}

   :access-client-payroll
   {:reason "personal data beyond the engagement's registered scope; the engagement letter, not the actor, sets that boundary"}

   :delete-engagement-records
   {:reason "destroying the evidence base makes every past recommendation unreviewable and is unrecoverable by construction"}

   :disable-audit-logging
   {:reason "removing the audit trail removes the evidence this actor's own governance rests on"}})

(defn supported? [op] (contains? supported op))
(defn reserved? [op] (contains? reserved op))

(defn declared?
  "True if `op` is named anywhere in this vocabulary. An op that is neither
  supported nor reserved is undeclared — the governor refuses it."
  [op]
  (or (supported? op) (reserved? op)))

(defn escalates?
  "True if the operation itself always requires human sign-off. Unsupported
  ops are never reached by this predicate (the governor hard-blocks first), so
  a false here is not an admission."
  [op]
  (boolean (get-in supported [op :escalates?])))

(defn engagement-op?
  "True if the operation binds to a registered engagement and must therefore
  satisfy every registered fact about it. False for undeclared and reserved
  ops, which the governor hard-blocks before this is consulted."
  [op]
  (boolean (get-in supported [op :engagement-op?])))

(defn reserved-reason [op] (get-in reserved [op :reason]))
