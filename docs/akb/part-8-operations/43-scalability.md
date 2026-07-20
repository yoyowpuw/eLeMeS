# Chapter 43 — Scalability

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 42 — Disaster Recovery](42-disaster-recovery.md) · Next: Ch. 44 — Performance Optimization

## 1. Purpose

Deliver NFR-006/007/011 (concurrency, autoscaling response) platform-wide, resolve Chapter
29's search cluster-sizing Open Question, and consolidate the burst-volume modeling
obligations flagged in Chapters 34 and 38.

## 2. Scaling Model — Distinguishing Three Independent Axes

Consistent with Chapter 9 §2 and Chapter 15 §5's already-established distinction:

| Axis | What Scales | Mechanism |
|---|---|---|
| Service-count (Ch.9) | Number of bounded-context services | Fixed at ~17 (Ch.11), not a runtime scaling concern |
| Horizontal replica scaling (Ch.15 §5) | Instances per service, responding to aggregate load | Reactive autoscaling, standard-tier default |
| Per-tenant capacity (Ch.15 §5, Ch.18 §3) | Reserved compute for silo tenants; rate-limiting for pooled tenants | Contractual sizing (silo) or fair-share throttling (pooled) |

This chapter's job is specifying the **triggers and pre-warming policy** across all three
axes, particularly for the bursty, deadline-driven load pattern named since Chapter 1 §3.

## 3. Pre-Warming Strategy (Satisfies NFR-011, Extends Ch.15 §4/Ch.23 §2)

Compliance-deadline patterns (quarterly/annual recertification windows, Ch.5 Phase 8) are
**predictable in advance** — known from the `CertificationExpiring` event schedule
(Ch.26) and assignment due-dates (Ch.25). This chapter specifies that the compliance-tier
autoscaler consumes this schedule directly: capacity is pre-warmed ahead of a known
concentration of due dates, rather than purely reacting to load after it begins — a
meaningfully stronger guarantee than generic reactive autoscaling, because it is informed
by domain knowledge (the assignment/certification data itself), not just infrastructure
metrics.

## 4. Resolving Chapter 29's Search Cluster-Sizing Question

**Decision:** shared search cluster for pooled tenants (with tenant-scoped index
partitions, not fully separate clusters — consistent with Ch.12 §2's pool model), dedicated
search cluster capacity for silo tenants — directly mirroring the database isolation model
(Ch.12 §2) rather than inventing a separate sizing philosophy for search specifically. This
resolves Chapter 29's Open Question by extending an already-approved pattern instead of
introducing new architecture.

## 5. Burst-Volume Modeling (Discharges Ch.34/Ch.38 Obligations)

| System | Peak Trigger | Mitigation |
|---|---|---|
| Notifications (Ch.34) | Synchronized deadline reminders at BR-011 scale | Batched/staggered send windows (working with Ch.34's frequency-capping) rather than instantaneous fan-out, smoothing vendor-rate-limit risk |
| Telemetry pipeline (Ch.38) | Correlated with all of the above peaks simultaneously | Telemetry ingestion sized to BR-011 peak concurrency directly, not average load, consistent with how every other capacity decision in this chapter is made |

## Summary
Scalability is governed by three independently-managed axes (service-count, horizontal
replica, per-tenant capacity), with the compliance tier's autoscaling made proactively
schedule-aware (pre-warmed ahead of known certification-expiry concentrations) rather than
purely reactive. Chapter 29's search cluster-sizing question is resolved by mirroring the
already-approved database isolation model rather than inventing new philosophy. Notification
and telemetry burst-volume obligations from Chapters 34 and 38 are both sized to BR-011 peak
concurrency, consistent with this chapter's general principle of designing to peak, not
average, load.

## Open Questions
Exact pre-warming lead time (how far in advance of a known deadline concentration to begin scaling up) — implementation-phase tuning informed by real autoscaler warm-up latency.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Schedule-aware pre-warming (§3) requires the autoscaler to query domain data (assignment due-dates) — a new coupling between infrastructure tooling and business data not present in typical reactive autoscaling | Medium | Medium | Implement as a scheduled batch job publishing a capacity-forecast signal, not a live query dependency, keeping the autoscaler's core operation decoupled from domain-service availability |

## Architecture Decisions
**ADR-072: Three-axis scaling model with schedule-aware, domain-informed pre-warming for the compliance tier** — §2–3. **ADR-073: Search cluster sizing mirrors the Ch.12 §2 database isolation model (shared partitions for pooled, dedicated for silo)** — §4, resolves Ch.29 Open Question.

## Future Research
Pre-warming lead-time tuning (implementation phase).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) §3 · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-006, 007, 011) · [Ch. 9](../part-2-system-domain-architecture/09-product-architecture.md), [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) §5, [Ch. 18](../part-3-identity-organization/18-multi-tenancy.md) §3 (three-axis sources) · [Ch. 25](../part-4-learning-domain/25-assignment-engine.md), [Ch. 26](../part-4-learning-domain/26-certification.md) (schedule source) · [Ch. 29](../part-5-media-discovery/29-search.md) (Open Question) · [Ch. 34](../part-6-insight/34-notification-system.md), [Ch. 38](38-observability.md) (burst obligations)

## Definition of Done
- [x] Three scaling axes explicitly distinguished and specified
- [x] Compliance-tier pre-warming made schedule-aware, not purely reactive
- [x] Ch.29's search cluster-sizing Open Question resolved
- [x] Notification/telemetry burst-volume obligations discharged

## Confidence Level
**Medium-High.** The three-axis model and search-sizing decision directly extend already-approved patterns — **High** confidence. The schedule-aware pre-warming mechanism (§3) is a genuinely new architectural coupling, not yet load-tested — **Medium** confidence, appropriately flagged.

## 6. Chapter Review

**Red Team:** The "capacity-forecast signal" decoupling mechanism (Risk mitigation) is a
reasonable design intent but is asserted without detail — how far ahead is the forecast
generated, and what happens if the forecast itself is wrong (e.g., a last-minute bulk
reassignment, Ch.25 §3, creates a due-date concentration the forecast didn't predict)?

**Blue Team:** Accepted — valid edge case. Addendum: schedule-aware pre-warming is a
**supplement to, not a replacement for, standard reactive autoscaling** (§2's horizontal
replica axis) — an unforecast surge still triggers normal reactive scaling; pre-warming
only improves responsiveness for the *predictable* portion of load, and this chapter's
claims should be read as "better than purely reactive," not "eliminates the need for
reactive scaling." This nuance is now explicit.

**CTO:** ADR-072 **Approved with Conditions** — condition is the Blue Team's
"supplement, not replacement" framing is binding, avoiding over-reliance on forecast
accuracy. ADR-073 **Approved**.

---
*End of Chapter 43. Proceed to Chapter 44 — Performance Optimization.*
