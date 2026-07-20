# Chapter 30 — Recommendation Engine

> Part V — Media & Discovery · [Index](../00-index.md) · Previous: [Ch. 29 — Search](29-search.md) · Next: Ch. 31 — AI Integration

## 1. Purpose

This chapter finally resolves the CISO/DPO-vs-CLO tension named as a standing design
constraint since Chapter 3 ADR-003 and re-flagged at every subsequent touchpoint (Ch.5
Phase 3, Ch.11 #12) — delivering FR-029 (personalized recommendations, data-minimization-
compatible) and taking ownership of the ranking-policy governance Chapter 29 assigned here.

## 2. Resolving Ch.3 ADR-003 — A Two-Tier Recommendation Model

| Tier | Data Used | Privacy Posture | Default? |
|---|---|---|---|
| **Tier 1: Content/competency-based (default, all tenants)** | Competency gaps (Ch.20), role profile, explicit learner actions already collected for legitimate compliance/development purposes (bookmarks, completed content, assigned paths) — **no new behavioral tracking** | Fully consistent with data-minimization (satisfies DPO/CISO) | **Yes — enabled by default for every tenant** |
| **Tier 2: Collaborative/behavioral (opt-in per tenant)** | Consumption patterns, dwell time, cross-learner similarity signals — genuinely richer personalization (satisfies CLO's feature-richness goal) | Requires additional behavioral data collection; gated behind explicit tenant-level opt-in, disclosed in tenant privacy configuration | No — tenant must explicitly enable |

This is the concrete resolution the AKB has deferred since Chapter 3: **the tension isn't
resolved by picking a side — it's resolved by making privacy posture a tenant-configurable
axis**, with a genuinely useful, privacy-respecting default (Tier 1) that doesn't wait on
Tier 2's opt-in to deliver value, and a richer opt-in tier for tenants (and their DPOs) who
explicitly consent to the trade-off. This closes the standing tension from Ch.3 ADR-003 for
this specific context, as that ADR anticipated.

## 3. Technology Evaluation

| Dimension | Build custom ML recommendation infra | **Tier 1: lightweight in-house content/competency-based rules; Tier 2: managed ML recommendation service (opt-in), Selected** | Managed ML service for everything |
|---|---|---|---|
| Fit to BR-016 tier-2 priority (medium confidence demand, Ch.2 §5) | Overinvestment for a medium-confidence-demand capability | **Right-sized — cheap default, opt-in investment only where demand is proven per-tenant** | Also overinvestment if applied platform-wide regardless of opt-in |
| Fit to Ch.3 ADR-003 resolution (§2) | Doesn't inherently map to the two-tier privacy model | **Directly implements it — Tier 1 needs no ML infra at all** | Would need to gate a single ML system behind opt-in anyway, added complexity for no benefit |
| Cost (Ch.1 Principle 6) | High, unconditional | Low baseline cost, cost scales only with actual Tier-2 adoption | Moderate baseline cost regardless of adoption |
| Final Recommendation | Rejected | **Selected** | Rejected — same end-state achievable more cheaply via the two-tier split |

## 4. Ranking-Policy Governance (Discharges Ch.29 Assignment)

Per Chapter 29's Red Team finding, ranking policy (e.g., boosting mandatory/compliance
content above popular-but-optional content) is owned here as a tenant-configurable policy
layer sitting above both Search (Ch.29) and Tier 1/2 recommendation results — a small,
explicit rule set (e.g., "compliance-due content always ranks above discovery content"),
not left to raw relevance scoring alone, directly preventing the Ch.29 Red Team's named
failure mode (compliance content buried by popularity).

## Summary
The standing CISO/DPO-vs-CLO tension from Chapter 3 ADR-003 is resolved with a two-tier
model: a privacy-respecting, no-new-tracking content/competency-based default (Tier 1,
always on) and an opt-in behavioral/collaborative-filtering tier (Tier 2, tenant-consented)
using a managed ML service only where adopted — right-sizing investment to BR-016's
medium-confidence demand tier. A tenant-configurable ranking-policy layer, owned here,
discharges Chapter 29's finding that raw popularity ranking could bury compliance-critical
content.

## Open Questions
Specific managed ML service vendor for Tier 2 — deferred to Ch.46 Licensing, contingent on real tenant opt-in demand materializing post-launch.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Tenant opt-in consent UX for Tier 2 is unclear/buried, resulting in uninformed consent | High (privacy/compliance exposure) | Medium | Explicit, standalone consent flow (not a buried settings toggle) — implementation-phase UX requirement, flagged strongly here |
| Ranking-policy rules (§4) become a dumping ground for ad hoc tenant-specific overrides over time | Low-Medium | Medium | Keep the rule set intentionally small and reviewed, per [Ch. 47 — Governance](../part-9-governance-future/47-governance.md) |

## Architecture Decisions
**ADR-048: Two-tier recommendation model — always-on privacy-respecting content/competency-based default, opt-in behavioral/ML tier — resolving the standing Ch.3 ADR-003 tension** — §2–3. **ADR-049: Tenant-configurable ranking-policy layer above raw relevance scoring, ensuring compliance content is never buried** — §4.

## Future Research
Managed ML vendor selection contingent on Tier-2 adoption data (Ch.46).

## Cross References
[Ch. 2](../part-1-foundations/02-business-requirements.md) (BR-016) · [Ch. 3](../part-1-foundations/03-stakeholders.md) (ADR-003) · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) Phase 3 · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-029) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #12 · [Ch. 29](29-search.md) · [Ch. 47](../part-9-governance-future/47-governance.md)

## Definition of Done
- [x] Ch.3 ADR-003's standing tension explicitly and concretely resolved
- [x] Technology approach right-sized to BR-016's confidence tier via Technology Evaluation Template
- [x] Ranking-policy governance discharged per Ch.29's assignment

## Confidence Level
**Medium-High.** The two-tier resolution mechanism is architecturally sound and directly traceable to Chapter 3's original framing — **High** confidence. Actual Tier-2 tenant adoption/demand remains unvalidated (consistent with Ch.2's own honesty about BR-016 being analyst-triangulated, not measured) — **Medium** confidence on the business case, not the architecture.

## 5. Chapter Review

**Red Team:** Tier 1's "no new behavioral tracking" claim should be scrutinized — even
"explicit learner actions already collected" (bookmarks, completions) is still behavioral
data in a meaningful sense; the Works Council concern (Ch.3 §8.2 addendum) about
individual-level surveillance could still apply if Tier 1 recommendations are computed
per-individual rather than per-cohort.

**Blue Team:** Accepted as a precise and valid distinction. Addendum: Tier 1
recommendations must be computed from data already legitimately processed for its original
purpose (compliance tracking, competency management) under the same individual-level
processing basis already justified for those purposes elsewhere in the AKB — this is
narrower than "any behavioral data," and the chapter's language is corrected to be
explicit about that boundary rather than the looser original phrasing.

**CTO:** ADR-048 **Approved with Conditions** — condition is the corrected, narrower
data-use boundary from the Blue Team response is binding: Tier 1 may only reuse data whose
individual-level processing is already justified elsewhere (Ch.20, Ch.25), not collect or
repurpose data solely for recommendation purposes.

---
*End of Chapter 30. Proceed to Chapter 31 — AI Integration.*
