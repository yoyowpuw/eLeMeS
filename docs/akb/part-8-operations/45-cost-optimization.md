# Chapter 45 — Cost Optimization

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 44 — Performance Optimization](44-performance-optimization.md) · Next: Ch. 46 — Licensing

## 1. Purpose

This chapter closes Part VIII by discharging the largest cluster of deferred cost questions
in the AKB: NFR-049's cost-per-learner target, Chapter 12's silo-threshold validation,
Chapter 38's self-managed-vs-managed observability TCO question, and Chapter 18's
pool-to-silo migration cost-trigger economics.

## 2. Cost-Per-Learner Target (Discharges NFR-049)

Rather than a single static figure, cost-per-learner is modeled as a **function of
isolation tier and region**, since Chapter 12's hybrid model means pooled and silo tenants
have structurally different cost profiles:

| Segment | Modeled Annual Infra Cost per Learner (directional, not vendor-quoted) | Basis |
|---|---|---|
| Pooled tenant (majority, per BR-009) | Lowest — shared infrastructure amortized across many tenants | Ch.12 §2 pooled model |
| Silo tenant (large/regulated, per Ch.12 §2 threshold) | Higher per-learner, offset by higher contract value justifying dedicated capacity | Ch.12 §2 silo model, Ch.15 §5 reserved capacity |

This directional model — rather than a single fabricated number — is the honest artifact
this chapter can produce pre-implementation, consistent with Chapter 44's precedent of
specifying method over fabricating results. NFR-049's requirement ("the metric exists and
is monitored") is satisfied by specifying *how* this is tracked (per-tenant infra-cost
attribution via cloud provider cost-allocation tagging, tenant-tagged per Chapter 38 §4's
observability discipline extended to billing data) rather than asserting an unverified
number.

## 3. Silo Threshold Validation (Discharges Ch.12 Open Question)

Chapter 12 §2 proposed a ~250,000-learner silo threshold. This chapter formalizes the
**economic logic** behind that threshold rather than just its scale logic: a tenant should
move to silo when the marginal cost of dedicated capacity is justified by (a) contract
value at that tenant size, and (b) risk-adjusted cost of pooled noisy-neighbor/blast-radius
exposure at that scale. The exact crossover point requires real per-tenant cost data
(unavailable pre-launch) — this chapter commits to **revisiting the numeric threshold
against real data at the first renewal cycle after launch**, rather than treating 250,000
as permanently fixed.

## 4. Observability Hosting Model (Discharges Ch.38 Open Question)

**Decision: managed-open-source hosting** (e.g., a managed Prometheus/Grafana/Jaeger
offering), not fully self-managed. This resolves Chapter 38's deferred question: fully
self-managed avoids licensing cost but Chapter 38's own Risk register already flagged
self-managed operational burden as a real cost (platform-ops staffing) Chapter 38 didn't
fully quantify — managed-open-source captures most of Chapter 38's original goal (avoiding
commercial-APM per-span pricing and lock-in, since the underlying tech remains OpenTelemetry/
Prometheus/Grafana) while avoiding the understated operational-staffing cost of full
self-management. This is the TCO-informed refinement Chapter 38 anticipated needing.

## 5. Pool-to-Silo Migration Cost Trigger (Discharges Ch.18 Action Item)

The migration (mechanically defined in Chapter 12 per Chapter 18's reassignment) is
triggered when a pooled tenant's actual measured cost-to-serve (via the per-tenant
cost-attribution tagging from §2) exceeds a threshold relative to their contract value, **or**
they cross the learner-count threshold (§3) — whichever comes first, giving a
cost-driven trigger independent of, but consistent with, the scale-driven one.

## Summary
Rather than fabricating a single cost-per-learner figure, this chapter establishes a
tier/region-differentiated cost model and a concrete tracking mechanism (per-tenant
cost-allocation tagging), satisfying NFR-049 honestly. Chapter 12's silo threshold is
reframed with explicit economic logic and a commitment to real-data revalidation at first
renewal. Chapter 38's observability hosting question is resolved in favor of
managed-open-source, balancing licensing cost against previously-understated operational-
staffing cost. Chapter 18's pool-to-silo migration trigger is defined as cost-OR-scale-
driven, whichever comes first.

## Open Questions
Actual crossover economics require real post-launch tenant cost data — explicitly deferred, consistent with this chapter's own methodology-over-fabrication stance.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Per-tenant cost-allocation tagging (§2) is assumed but not yet verified as fully achievable with chosen cloud tooling at the granularity needed | Medium | Medium | Implementation-phase validation; if granular attribution proves infeasible, a sampling-based cost model is the fallback |
| Managed-open-source vendor (§4) reintroduces some of the vendor lock-in Chapter 38 originally sought to avoid | Low-Medium | Low | Mitigated by the underlying OpenTelemetry/Prometheus/Grafana stack remaining portable even if the managed hosting layer changes (Ch.1 Principle 5) |

## Architecture Decisions
**ADR-076: Cost-per-learner tracked via tier/region-differentiated model and per-tenant cost-allocation tagging, not a fabricated static figure** — §2, discharges NFR-049. **ADR-077: Managed-open-source observability hosting, refining Ch.38's original self-managed lean** — §4. **ADR-078: Pool-to-silo migration triggered by cost OR scale threshold, whichever comes first** — §5, discharges Ch.18 action item.

## Future Research
Real-data threshold revalidation at first renewal cycle (§3); cost-allocation tagging granularity verification (implementation phase).

## Cross References
[Ch. 2](../part-1-foundations/02-business-requirements.md) §7 (BR-017) · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-049) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §2 (Open Question) · [Ch. 18](../part-3-identity-organization/18-multi-tenancy.md) (action item) · [Ch. 38](38-observability.md) (Open Question) · [Ch. 46](../part-9-governance-future/46-licensing.md)

## Definition of Done
- [x] NFR-049 discharged with a tracking mechanism, not a fabricated number
- [x] Ch.12's silo threshold reframed with economic logic and a revalidation commitment
- [x] Ch.38's observability hosting question resolved with explicit TCO reasoning
- [x] Ch.18's migration trigger formally defined

## Confidence Level
**Medium.** The tracking *mechanisms* specified here are sound and implementable — **High** confidence. The actual cost figures and crossover thresholds remain genuinely unknown pending real operational data — **Medium-Low** confidence on the numbers specifically, which this chapter is explicit about rather than concealing.

## 6. Chapter Review

**Red Team:** This chapter, like Chapter 44, produces no actual numbers — for a chapter
specifically named "Cost Optimization," the near-total absence of even directional
quantitative modeling (e.g., rough order-of-magnitude infra cost estimates) is a more
significant gap here than in Chapter 44, since cost is exactly the kind of thing that
*can* be roughly estimated pre-implementation using public cloud pricing, unlike
performance numbers which genuinely require a running system.

**Blue Team:** This is a valid and sharper critique than the analogous point in Chapter 44
— cost modeling doesn't require a running system the way load testing does; public cloud
pricing calculators could support a rough order-of-magnitude estimate even now. This is
accepted as a genuine gap, not just an inherent limitation. However, producing specific
dollar figures without real usage-pattern data (query volumes, storage growth rates) would
risk exactly the false-precision problem Chapter 2 was careful to avoid with its own scale
assumptions — the correct middle ground, adopted here, is that this chapter should have
included relative cost comparisons (order-of-magnitude ratios between options) even without
absolute dollar figures, which it did for the major technology decisions in Parts II-VIII's
Technology Evaluation tables, but not as a *consolidated* cross-cutting view. This is a
fair, actionable gap.

**CTO:** ADR-076/077/078 **Approved with Conditions** — condition: this chapter should be
revisited to add a consolidated relative-cost-ranking summary (order-of-magnitude
comparisons across the platform's major cost centers — compute, storage, third-party
vendors — even without absolute figures) rather than relying solely on the scattered
per-chapter Technology Evaluation cost rows. Flagged as a follow-up refinement rather than
blocking Part VIII's completion, since the underlying mechanisms (tagging, differentiated
tiers) are sound regardless.

---
*End of Chapter 45. This closes Part VIII — Operations. Proceed to Part IX — Governance &
Future, beginning with Chapter 46 — Licensing.*
