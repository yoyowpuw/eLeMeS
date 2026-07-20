# Chapter 38 — Observability

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 37 — Offline Learning](../part-7-platform-integration/37-offline-learning.md) · Next: Ch. 39 — DevOps

## 1. Purpose

Deliver NFR-039–042 (MTTD/MTTR, tracing coverage, per-tenant visibility) and consolidate
the numerous observability obligations accumulated across Parts II–VII: cache-staleness
alerting (Ch.19, Ch.29, Ch.32), grader-queue backlog (Ch.23), delivery-log tracking
(Ch.34), and dependency health (Ch.35's fallback registry).

## 2. Observability Stack — Technology Evaluation

| Dimension | Commercial APM (Datadog/New Relic-class) | **Open-source stack (OpenTelemetry + Prometheus + Grafana + Jaeger/Tempo-class), Selected** |
|---|---|---|
| Fit to NFR-041 (100% cross-context tracing) | Excellent, turnkey | Excellent — OpenTelemetry is the vendor-neutral industry standard specifically for this | 
| Cost at BR-007/BR-008 scale (very high telemetry volume) | Very high — commercial APM per-host/per-span pricing scales poorly at this volume | Moderate — infrastructure cost scales with usage but without per-span licensing markup |
| Vendor lock-in / exit strategy (Ch.1 Principle 5) | High — proprietary agents/query languages | **Low — OpenTelemetry is instrumentation-standard; backend (Prometheus/Grafana/Jaeger) is swappable** |
| Operational burden (running the stack itself) | Low (vendor-managed) | Higher — requires genuine platform-ops investment | 
| Per-tenant visibility (NFR-042) | Achievable via tagging | Achievable via tagging, same effort either way |
| Final Recommendation | Rejected — cost and lock-in risk both too high at this AKB's scale/horizon | **Selected** |

**Decision:** OpenTelemetry as the universal instrumentation standard across all 17
bounded-context services (enforced via the shared service template, Ch.15 §2), with a
self-managed or managed-open-source backend (Prometheus/Grafana for metrics, Jaeger/Tempo-
class for traces) — avoiding the per-span commercial-APM cost curve at BR-007/008 scale
while keeping the exit-strategy discipline (Ch.1 Principle 5) that a proprietary agent
would compromise.

## 3. Consolidated Observability Obligations Registry

| Obligation | Source Chapter | Implementation |
|---|---|---|
| Cache-staleness alerting | Ch.19 §3 (Assignment/Org Hierarchy cache), Ch.29 (search CDC lag), Ch.32 (reporting read-model lag) | Standard "staleness" metric type emitted by any context maintaining a cache/read-model, alertable on a per-context threshold |
| Grader-queue backlog | Ch.23 Risk | Queue-depth metric, alertable, surfaced to Ch.32 reporting dashboards |
| Delivery-log tracking | Ch.34 §3 | Standard delivery-attempt metric per channel |
| Dependency health | Ch.35 §4 registry | Health-check metric per external dependency, feeding both alerting and the tenant-facing status-page concept (Ch.16 §6) |
| Compliance-tier SLA tracking | Ch.7 NFR-012, Ch.15 §4 | Separate dashboard/alerting thresholds per tier (99.95% vs 99.9%), matching the dual-tier isolation already architected |

This registry is the observability-side counterpart to Chapter 35's dependency-fallback
registry — both are living documents that future chapters append to rather than
re-solving from scratch.

## 4. Per-Tenant Visibility (Satisfies NFR-042)

Every metric/trace/log is tagged with `tenant_id` at emission (enforced by the shared
service template), enabling Customer Success (Ch.3) to filter to a single tenant's data
without querying or exposing other tenants' telemetry — extending the tenant-isolation
discipline established at the data layer (Ch.12) into the observability layer.

## Summary
An OpenTelemetry-instrumented, open-source-backed observability stack is selected over
commercial APM, chosen specifically to avoid per-span cost scaling and vendor lock-in at
this AKB's scale and 7-10-year horizon. This chapter consolidates five previously-scattered
observability obligations (cache staleness, grader backlog, delivery tracking, dependency
health, dual-tier SLA tracking) into one registry, and extends tenant-isolation discipline
into telemetry tagging to satisfy NFR-042.

## Open Questions
Self-managed vs. managed-open-source (e.g., Grafana Cloud) hosting of the backend — deferred to [Ch. 45 — Cost Optimization](45-cost-optimization.md) TCO modeling.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Self-managed observability stack itself becomes an operational burden requiring dedicated ownership | Medium | Medium | Explicit platform-ops staffing consideration for [Ch. 48 — Operations](../part-9-governance-future/48-operations.md), not assumed to run itself |
| Telemetry volume at BR-011 peak concurrency could itself become a scaling problem | Medium | Medium | [Ch. 43 — Scalability](43-scalability.md) should include telemetry pipeline capacity in its planning |

## Architecture Decisions
**ADR-061: OpenTelemetry-instrumented open-source observability stack, not commercial APM** — §2. **ADR-062: Consolidated observability obligations registry, tenant-tagged at emission** — §3–4.

## Future Research
Self-managed vs. managed-hosting decision (Ch.45).

## Cross References
[Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-039–042) · [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) §2 · [Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md), [Ch. 23](../part-4-learning-domain/23-assessment-engine.md), [Ch. 29](../part-5-media-discovery/29-search.md), [Ch. 32](../part-6-insight/32-reporting.md), [Ch. 34](../part-6-insight/34-notification-system.md), [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) (registry sources) · [Ch. 43](43-scalability.md) · [Ch. 45](45-cost-optimization.md) · [Ch. 48](../part-9-governance-future/48-operations.md)

## Definition of Done
- [x] Observability stack selected via Technology Evaluation Template
- [x] Consolidated obligations registry compiled from 5 prior chapters
- [x] Per-tenant tagging specified against NFR-042

## Confidence Level
**High** — OpenTelemetry is the clear, low-risk industry-standard choice at this point in its maturity; the consolidation work is administrative, not speculative.

## 5. Chapter Review

**Red Team:** Choosing self-managed open-source over commercial APM specifically to save
cost (§2) trades a well-known cost problem for a less-visible operational-burden problem
(Risks table already names this) — the chapter doesn't quantify whether the TCO comparison
actually favors open-source once realistic platform-ops staffing cost is included, it just
asserts "moderate" vs. "very high" cost.

**Blue Team:** Accepted as a fair incompleteness — the decision's directional logic (avoid
per-span commercial pricing at this data volume) is sound, but true TCO parity is
legitimately unverified here. Addendum: this decision is marked provisional pending
[Ch. 45 — Cost Optimization](45-cost-optimization.md)'s full TCO comparison, consistent
with how this chapter already deferred the hosting-model Open Question to Ch.45 — the
principle (avoid commercial APM lock-in) is retained even if Ch.45 later favors a
managed-open-source middle ground over fully self-managed.

**CTO:** ADR-061 **Approved with Conditions** — condition is explicit: this is a
directionally-sound but not yet fully TCO-verified decision, to be confirmed or refined by
[Ch. 45](45-cost-optimization.md)'s dedicated cost analysis.

---
*End of Chapter 38. Proceed to Chapter 39 — DevOps.*
