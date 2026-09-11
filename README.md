# cloud-itonami-isco-2421

Open Business Blueprint for **ISCO-08 2421**: Management and Organization Analysts — an ISCO
**Wave 0 (cognitive substrate)** occupation per ADR-2607121000:
pure-cognitive work, the LLM-first wave, **no robotics gate** —
eligible for actor implementation now.

**Maturity: `:implemented`** — ManagementOrganizationAnalystsAdvisor ⊣
ManagementOrganizationAnalystsGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
51 tests / 320 assertions green, plus a 26-scenario governed run that
demonstrates 24 refusals.

## The operation vocabulary is closed

`mgmtanalyst.operation` declares every op this actor may propose, and
every op it is reserved from proposing. Anything outside that vocabulary
is refused. Before it existed the governor bound two named ops and
admitted everything else — `:delete-all-engagement-records`,
`:sign-client-contract` and a proposal whose `:op` was `nil` were all
admitted as *clean* verdicts, with an empty violation list to show a
reviewer, and an undeclared op bypassed every engagement invariant below.
The measurements are in that namespace's docstring.

## HARD invariants (always `:hold`, never overridable)

Set membership and arithmetic, not editorial judgement:

1. **Declared operation** — the `:op` is in the vocabulary.
2. **Reserved operation** — an op naming authority this actor does not
   hold (contractual commitment, billing, access beyond the engagement's
   registered scope, removal of the audit trail) is a permanent block,
   never an escalation.
3. **Client provenance** — the record the store returned identifies the
   client the request named.
4. **No actuation** — `:effect` must be `:propose`.
5. **Usable confidence** — a number on [0,1], because a floor only
   bounds a quantity on the scale the floor is stated in.
6. **Engagement basis** — an engagement-bound op cites a registered,
   well-formed engagement belonging to this client.
7. **Metric-citation membership** — every metric cited is a member of
   the engagement's registered baseline-metrics set, and at least one is
   cited (no fabricated *and* no absent evidence).
8. **Savings-claim ceiling** — the claimed-savings-pct is readable and
   does not exceed the engagement's registered ceiling. Savings-claim
   arithmetic is not a marketing decision.

Invariants 6–8 apply to **every** engagement-bound operation. They used
to be gated on `:approve-recommendation`, so `:publish-recommendation` —
the operation whose entire purpose is external delivery to client
leadership — was exempt from both headline checks and reached a human
carrying a fabricated metric and a 900% savings claim with an empty
violation list.

**Escalations** (human sign-off): `:publish-recommendation`,
`:flag-engagement-risk`, and confidence below 0.6.

## Audit trail

`mgmtanalyst.ledger` hash-chains every entry, so a dropped or reordered
entry is detectable rather than merely disallowed by a code path. Each
write records whether it was approved by a `:human` (resumed from the
interrupt) or by the `:actor`; previously both left an indistinguishable
`{:disposition :commit ...}`. End-truncation is *not* detectable without
an external anchor, and `verify` says so rather than claiming otherwise.

## Operator quickstart

```bash
kbb -M:test                  # 51 tests / 320 assertions
kbb -M -m mgmtanalyst.sim    # 26 scenarios; exits non-zero on 0 refusals
kbb -M:lint                  # clj-kondo, errors fail
```

`mgmtanalyst.sim` runs the table through the **wired graph**, not the
pure `check` function, and refuses to report a pass if it demonstrated no
refusal — a governed actor's claim is not that it acts, but that there
exist actions it refuses.

AGPL-3.0-or-later, forkable by any qualified operator. Part of the
[cloud-itonami](https://itonami.cloud) open business fleet.
