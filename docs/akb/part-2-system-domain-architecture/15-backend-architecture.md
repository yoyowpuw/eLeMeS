# Chapter 15 — Backend Architecture

> Part II — System & Domain Architecture · [Index](../00-index.md) · Previous: [Ch. 14 — Frontend Architecture](14-frontend-architecture.md) · Next: Part III, Ch. 16 — Authentication

## 1. Purpose

This chapter closes out Part II by discharging five outstanding action items pointed at
it: the backend language/runtime decision, the concrete mechanism for Chapter 7's ADR-009
dual-tier availability split, Chapter 9's per-tenant scaling concern, Chapter 12's RLS
composite-indexing guidance, and Chapter 13's "BFFs are dumb" rule and idempotency-key
store specification.

## 2. Backend Language/Runtime — Technology Evaluation

| Dimension | Kotlin/JVM (Selected) | Go | Node.js/TypeScript | Python |
|---|---|---|---|---|
| Fit to rich DDD domain modeling (Ch.10 §4 aggregates/value objects) | Excellent — strong typing, mature OOP+functional hybrid | Weaker — historically thinner generics/domain-modeling ergonomics | Good with TypeScript, but domain-modeling ecosystem less mature | Weaker — dynamic typing risk for compliance-critical logic |
| Event-sourcing/CQRS framework maturity (Ch.12 ADR-017) | Mature (Axon Framework and JVM ecosystem broadly) | Immature | Immature | Immature |
| Hiring pool at enterprise scale (Ch.1 Principle 6, 7-10yr horizon) | Very large (Java heritage + modern Kotlin appeal) | Large, growing | Very large | Large |
| Observability/APM ecosystem maturity (NFR-039-041) | Excellent, most mature enterprise APM support | Good | Good | Good |
| Performance/throughput (NFR-008 50k submissions/min) | Excellent (JVM JIT, mature tuning practice) | Excellent, lower memory footprint | Good | Weaker for CPU-bound paths (GIL) |
| Complexity (1-10) | 6 | 4 | 5 | 4 |
| Final recommendation | **Selected as default platform-wide** | **Permitted by exception** for narrowly-scoped, high-throughput infra-adjacent services (e.g., search-indexing pipeline) | Rejected as backend default (frontend-only, Ch.14) | Rejected for core services; remains viable for isolated data-science/ML workloads feeding [Ch. 31 — AI Integration](../part-5-media-discovery/31-ai-integration.md) |

**Decision:** Kotlin/JVM is the default backend language across the 17 bounded-context
services, chosen specifically for its event-sourcing/CQRS ecosystem maturity (directly
serving Ch.12 ADR-017's compliance-tier requirement) and enterprise hiring-pool depth (Ch.1
Principle 6). Go is permitted only by explicit per-service ADR exception, to prevent the
polyglot sprawl named as an anti-pattern in Ch.8 §5 (Cornerstone).

## 3. Event Bus — Technology Evaluation

| Dimension | Kafka (Selected) | Cloud-native queues (SNS/SQS-class) | Pulsar | NATS |
|---|---|---|---|---|
| Fit to event sourcing (durable, replayable, ordered log, Ch.12 ADR-017) | Excellent — this *is* the log-based pattern | Poor — no replay/log semantics natively | Excellent, technically comparable to Kafka | Weaker durability guarantees for compliance events |
| Throughput (BR-012, NFR-008) | Excellent, industry-proven at this scale | Good but service-quota-bound | Excellent | Good |
| Hiring pool / operational maturity | Very large | Large (fully managed reduces ops burden) | Small | Small |
| Multi-region (Ch.1 §3) | Requires deliberate cluster topology (MirrorMaker or managed equivalent) | Native to cloud provider | Native (geo-replication) | Native |
| Final recommendation | **Selected**, as a managed offering (e.g., MSK/Confluent Cloud) to offset operational burden | Rejected as primary — replay semantics gap disqualifies it for the compliance-critical tier's event-sourcing needs | Rejected — technically comparable but smaller talent pool fails Ch.1 Principle 6's 7-10yr hiring-cost lens | Rejected — durability profile too weak for compliance events |

**Decision:** Managed Kafka as the single logical event bus (Ch.9 §3), carrying Chapter 5's
domain event inventory, doubling as the event-sourcing log for the 3 compliance-critical
contexts (Ch.12 ADR-017) — one technology serves both the "event bus" and "event store"
roles, avoiding two separate systems for adjacent concerns.

## 4. Compliance-Tier Isolation — Concrete Mechanism (Resolves Ch.7 ADR-009 / Ch.9 OQ)

| Aspect | Compliance-Critical Tier (Assignment, Assessment, Certification) | Standard Tier |
|---|---|---|
| Deployment topology | Dedicated Kubernetes namespace/cluster with reserved capacity (no shared node pool with standard tier) | Shared node pool, standard autoscaling |
| On-call rotation | Dedicated rotation, tighter paging thresholds matching NFR-012's 99.95% target | Standard rotation, 99.9% target |
| Autoscaling policy | Pre-warmed capacity ahead of known compliance-deadline traffic patterns (NFR-011), not purely reactive | Reactive autoscaling only |
| Database | Dedicated connection pools and, for the largest silo tenants (Ch.12 §2), dedicated read replicas | Shared pooled connections |
| Dependency policy | Compliance-tier services MAY NOT take a synchronous runtime dependency on any standard-tier service — only async (event bus) — so a standard-tier outage cannot cascade into a compliance-tier SLA breach | No such restriction |

This is the concrete answer Chapter 7 (ADR-009) and Chapter 9 (Open Question) both deferred
to this chapter: isolation is realized at the **deployment topology, on-call, and
dependency-direction** level — not a different database technology (Chapter 12 already
established single-technology PostgreSQL platform-wide).

## 5. Per-Tenant Scaling (Resolves Ch.9 Action Item)

Distinguished explicitly from *service-count* scaling (Ch.9's topology): per-tenant load is
absorbed via (a) horizontal replica autoscaling within each service, keyed to aggregate
load, and (b) for silo tenants (Ch.12 §2), dedicated compute reservations sized to that
tenant's contracted peak (informed by BR-010). Pooled tenants share the standard autoscaling
pool with per-tenant rate-limiting (Ch.13's API Gateway) as the noisy-neighbor safeguard.

## 6. Service-to-Service Security

mTLS via service mesh for all internal gRPC calls (Ch.13 §2); the Identity & Auth context
(Ch.11 #1) issues short-lived service identity tokens consumed by the mesh — no long-lived
shared secrets between services, satisfying NFR-017/018 for internal traffic, not just
external.

## 7. BFF-Thin Rule (Discharges Ch.13 Action Item) & Idempotency Store (Discharges Ch.13 Risk)

- **BFF-thin rule:** BFF services (Ch.13 §6) may perform request aggregation, response
  shaping, and persona-specific filtering only. Any conditional business logic (e.g.,
  assignment-eligibility rules) MUST live in the owning bounded-context service. Enforced
  via code-ownership rules in [Ch. 39](../part-8-operations/39-devops.md) CI (a BFF directory may not import
  domain-rule packages directly).
- **Idempotency store:** a dedicated, highly-available PostgreSQL table (`idempotency_keys`,
  tenant-scoped, TTL-cleaned) — not a bespoke service — satisfying Chapter 13's flagged
  single-point-of-failure risk by reusing the already-proven Chapter 12 database platform
  rather than introducing a new technology.

## 8. RLS Performance Guidance (Discharges Ch.12 Action Item)

All pooled-cluster tables (Ch.12 §2) require a composite index with `tenant_id` as the
leading column on every query path used by NFR-001-gated endpoints; this is enforced as a
mandatory schema-review checklist item in [Ch. 39](../part-8-operations/39-devops.md)'s CI, discharging Chapter
12's Red-Team-flagged RLS-overhead risk with a concrete engineering practice rather than
leaving it as an unmanaged risk.

## Summary
Kotlin/JVM is selected as the default backend language (Go by exception only), and managed
Kafka serves as both event bus and event store, directly serving Chapter 12's event-sourcing
decision. This chapter delivers concrete mechanisms for five previously-deferred items:
compliance-tier isolation (deployment topology + dependency-direction rule, not a different
DB), per-tenant scaling (distinct from service-count scaling), the BFF-thin enforcement
rule, a reused-Postgres idempotency store, and mandatory `tenant_id`-leading composite
indexing to protect NFR-001 under RLS.

## Open Questions
Service mesh product selection (e.g., Istio/Linkerd-class) left to implementation-phase evaluation, not architecturally load-bearing enough to warrant a dedicated AKB chapter.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Compliance-tier's "no synchronous dependency on standard tier" rule violated informally over time | High | Medium | Enforce via automated dependency-graph linting in CI, not code review alone |
| Kotlin/JVM chosen platform-wide reduces the option to use Go's simplicity where genuinely warranted | Low-Medium | Low | Explicit per-service ADR-exception process (§2) preserves flexibility without sprawl |

## Architecture Decisions
**ADR-023: Kotlin/JVM as default backend language, Go by exception** — §2. **ADR-024: Managed Kafka as unified event bus + event store** — §3. **ADR-025: Compliance-tier isolation via deployment topology and one-way dependency rule, not separate database technology** — §4, resolves Ch.7/Ch.9.

## Future Research
Service mesh product selection at implementation phase.

## Cross References
[Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (ADR-009) · [Ch. 9](09-product-architecture.md) · [Ch. 11](11-bounded-contexts.md) · [Ch. 12](12-database-architecture.md) · [Ch. 13](13-api-strategy.md) · [Ch. 39](../part-8-operations/39-devops.md)

## Definition of Done
- [x] Backend language selected via full Technology Evaluation Template
- [x] Event bus selected via full Technology Evaluation Template
- [x] Compliance-tier isolation mechanism concretely specified (resolves Ch.7/Ch.9)
- [x] Per-tenant scaling distinguished and specified (resolves Ch.9)
- [x] BFF-thin rule and idempotency store specified (resolves Ch.13)
- [x] RLS composite-indexing practice mandated (resolves Ch.12)

## Confidence Level
**High** — every decision in this chapter closes a specific, previously-identified gap with a concrete mechanism rather than introducing new unreviewed scope; Kotlin/JVM and Kafka are both extremely well-proven at this scale.

## 9. Chapter Review

**Red Team:** The "no synchronous dependency on standard tier" rule (§4) is a strong claim
that may be impractical in practice — e.g., does Assignment (compliance-tier) really never
need a synchronous read from Org Hierarchy (standard-tier, Ch.11 #4) during enrollment?
Chapter 11's context map shows exactly this dependency (`ORG -->|Customer-Supplier|
ASSIGN`), which appears to contradict §4's rule.

**Blue Team:** Valid catch — this is a genuine contradiction, not a matter of interpretation.
Correction: §4's rule is narrowed to *"no synchronous runtime dependency on a
standard-tier service for the hot path of certificate issuance/assessment grading
specifically"* — Assignment's read of Org Hierarchy for eligibility computation is
acceptable (with caching/fallback to last-known-good org data to bound the blast radius of
an Org Hierarchy outage), but Certification's issuance path itself must not add new
synchronous standard-tier dependencies. This is now the precise, non-contradictory rule.

**CTO:** ADR-023/024/025 **Approved**; ADR-025 specifically **Approved with the Red-Team-
identified correction applied** — the narrowed rule is now the binding version. Action item:
[Ch. 19 — Organization Hierarchy](../part-3-identity-organization/19-organization-hierarchy.md) must specify the
cached/fallback behavior for Org Hierarchy reads consumed by Assignment, per the corrected
rule.

---
*End of Chapter 15. This closes Part II — System & Domain Architecture. Proceed to Part III
— Identity & Organization, beginning with Chapter 16 — Authentication.*
