# cloud-itonami-isco-2421

Open Business Blueprint for **ISCO-08 2421**: Management and Organization Analysts — an ISCO
**Wave 0 (cognitive substrate)** occupation per ADR-2607121000:
pure-cognitive work, the LLM-first wave, **no robotics gate** —
eligible for actor implementation now.

**Maturity: `:implemented`** — ManagementOrganizationAnalystsAdvisor ⊣
ManagementOrganizationAnalystsGovernor as a langgraph StateGraph
(`intake → advise → govern → decide → commit/hold`, human-approval
interrupt), modeled on cloud-itonami-isco-4311's bookkeeping actor.
13 tests / 27 assertions green.

The consulting HARD invariants — set membership and arithmetic
ceiling, not editorial judgement:

1. **Metric-citation membership** — every metric cited by a
   recommendation must be a member of the engagement's registered
   baseline-metrics set (no fabricated evidence).
2. **Savings-claim ceiling** — the proposed claimed-savings-pct must
   not exceed the engagement's registered ceiling — savings-claim
   arithmetic is not a marketing decision.

Also HARD: unregistered/foreign engagement, unregistered organization,
non-`:propose` effect. Escalations (always human sign-off):
`:publish-recommendation` (external delivery to client leadership),
low confidence (< 0.6).



AGPL-3.0-or-later, forkable by any qualified operator. Part of the
[cloud-itonami](https://itonami.cloud) open business fleet.
