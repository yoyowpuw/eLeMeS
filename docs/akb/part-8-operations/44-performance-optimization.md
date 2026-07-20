# Chapter 44 — Performance Optimization

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 43 — Scalability](43-scalability.md) · Next: Ch. 45 — Cost Optimization

## 1. Purpose

This chapter discharges the load-testing methodology obligation Chapter 7 assigned here for
NFR-001–011, plus three specific validation items deferred by name: Chapter 12's RLS-
overhead test, Chapter 13's bulk-batch sizing, and Chapter 17's policy-evaluation caching
question.

## 2. Load-Testing Methodology (Discharges Ch.7 Action Item)

| Test Type | Methodology | Validates |
|---|---|---|
| Steady-state load test | Sustained synthetic traffic at BR-010/011 concurrency targets for ≥1 hour | NFR-001, 002, 004, 006, 007 |
| Spike/burst test | Sudden ramp to BR-011 peak within minutes, matching the known compliance-deadline pattern (Ch.1 §3) | NFR-011 (5-minute autoscale response), Ch.43 §3's pre-warming benefit specifically (compare pre-warmed vs. cold-start spike performance) |
| Soak test | Extended (72h+) moderate load | Memory leaks, connection-pool exhaustion, gradual degradation invisible to short tests |
| Chaos/failure-injection test | Deliberately kill instances, induce network partition, exercise Ch.42's failover | NFR-013/014 RTO/RPO, Ch.42 §6's "demonstrated not just designed" distinction |

All four test types run in a dedicated, production-scale-representative staging
environment before each major release, with results tracked over time (regression
detection), not just pass/fail at a point in time.

## 3. RLS Overhead Validation (Discharges Ch.12 Action Item)

Load-tested specifically: pooled-cluster query performance with `tenant_id`-leading
composite indexes (Ch.15 §8) under realistic multi-tenant concurrent query load, comparing
RLS-enabled vs. a theoretical RLS-disabled baseline to quantify actual overhead against
NFR-001's P95<300ms budget — confirming whether Chapter 12's defense-in-depth choice
carries an acceptable cost, with the specific numeric finding to be recorded as an
addendum to Chapter 12 once measured (implementation-phase artifact, not fabricated here).

## 4. Bulk-Batch Sizing Validation (Discharges Ch.13 Open Question)

The 10,000-record batch size assumed in Chapter 13 §4 is validated against NFR-009's
1M-learner/15-minute target under real database write-throughput testing; batch size is
tuned empirically rather than left at the original planning-stage assumption.

## 5. Policy-Evaluation Caching (Discharges Ch.17 Open Question)

Per-request-session caching of OPA policy-sidecar evaluation results (Ch.17 §2) is
determined empirically: if sidecar evaluation overhead measured under BR-011 peak
concurrency load testing is negligible relative to NFR-001's budget, no caching is added
(avoiding unnecessary complexity, per Ch.1 Principle 6); if measurable, a short-TTL
per-session cache is added following the same pattern already established in Chapter 19 §3.

## Summary
A four-part load-testing methodology (steady-state, spike/burst, soak, chaos/failure-
injection) is established as the standing validation mechanism for every NFR-001–011
numeric target set earlier in this AKB, run before every major release with regression
tracking over time. This chapter also specifies exactly how three previously-deferred
validation questions — RLS overhead, bulk-batch sizing, and policy-evaluation caching — are
to be empirically resolved, rather than resolving them with further untested assumptions.

## Open Questions
Actual measured results for RLS overhead, batch sizing, and policy caching are implementation-phase artifacts — this chapter specifies the method, not the numbers, consistent with Chapter 7's own honesty about unvalidated targets.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Staging environment not being truly production-scale-representative, producing misleading test results | High | Medium | Staging must use production-equivalent data volume (synthetic data at BR-007/008 scale), not a scaled-down approximation |
| Load-testing treated as a one-time pre-launch activity rather than continuous regression tracking | Medium | Medium | Explicit standing requirement (§2) that this runs before every major release, integrated into [Ch. 39](39-devops.md)'s pipeline as a (non-blocking but tracked) stage |

## Architecture Decisions
**ADR-074: Four-part standing load-testing methodology (steady-state, spike, soak, chaos) run before every major release** — §2, discharges Ch.7 action item. **ADR-075: RLS overhead, bulk-batch sizing, and policy-caching are resolved empirically via this methodology, not further assumption** — §3–5.

## Future Research
Recording actual measured results as addenda to Chapters 12, 13, and 17 once available (implementation phase).

## Cross References
[Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-001–011, action item) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) (action item) · [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) §4 (Open Question) · [Ch. 17](../part-3-identity-organization/17-authorization.md) §2 (Open Question) · [Ch. 39](39-devops.md) · [Ch. 42](42-disaster-recovery.md) §6 · [Ch. 43](43-scalability.md) §3

## Definition of Done
- [x] Load-testing methodology specified across 4 test types, discharging Ch.7 action item
- [x] RLS overhead, bulk-batch sizing, and policy-caching validation methods specified, discharging 3 prior Open Questions
- [x] Standing (not one-time) execution cadence specified

## Confidence Level
**High** — this chapter correctly specifies *method* rather than fabricating *results*, which is the intellectually honest position consistent with every prior chapter's treatment of unvalidated numeric targets.

## 6. Chapter Review

**Red Team:** This chapter is entirely about *how* to validate, with zero actual validation
performed — for an AKB whose stated mission is to let a team "build the system without
performing additional architectural research," leaving every single performance number
genuinely unverified until implementation is a real gap, even if honestly disclosed.

**Blue Team:** This is a fair characterization, but it's an inherent limit of what a
pre-implementation AKB can honestly claim, not a defect in this chapter's reasoning — no
architecture document can produce empirically-measured performance results for a system
that doesn't exist yet. The chapter's contribution (a rigorous, standing methodology) is
the correct and complete deliverable for this stage; fabricating specific numbers would be
actively worse (false confidence) than honestly specifying the method. This limitation is
now stated plainly here rather than left as an implicit reading of the Confidence Level
section.

**CTO:** ADR-074/075 **Approved**. Explicit acknowledgment: this chapter's Definition of
Done is methodological completeness, not empirical completeness — consistent with honest
architecture-stage deliverables, not a shortcoming to be corrected within this AKB's scope.

---
*End of Chapter 44. Proceed to Chapter 45 — Cost Optimization.*
