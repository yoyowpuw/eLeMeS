# Chapter 48 — Operations

> Part IX — Governance & Future · [Index](../00-index.md) · Previous: [Ch. 47 — Governance](47-governance.md) · Next: Ch. 49 — Maintenance

## 1. Purpose

This chapter discharges the largest remaining cluster of "assign to Ch.48" deferrals:
the support-impersonation access model (Ch.4/16), tenant-IdP-outage communication (Ch.16),
grader spot-audit practices (Ch.24), break-glass access review (Ch.16 §5/NFR-022), and the
quarterly DR drill program (Ch.42 §6).

## 2. Support-Impersonation Access Model (Discharges Ch.4/Ch.16 Action Items)

| Aspect | Policy |
|---|---|
| Access grant | Time-boxed (max 4-hour window), requires tenant-admin consent notification (not silent), per-incident justification logged |
| Scope | Read/diagnostic access by default; write actions require a separate, more restrictive elevated-access grant with additional approval |
| Audit | Every action taken under impersonation is tagged distinctly in the audit log (NFR-022), reviewable by the tenant, not just internally |
| Quarterly access review | All break-glass grants from the preceding quarter are reviewed for pattern anomalies (e.g., a support engineer with unusually high impersonation frequency) — closes the loop Chapter 16 flagged |

This gives FR-008 (Ch.6) and NFR-022 (Ch.7) their full operational specification, not just
the architectural logging mechanism Chapter 16 established.

## 3. Tenant-IdP-Outage Communication (Discharges Ch.16 Action Item)

A distinct status-page category for "tenant-specific identity provider issue" (separate
from platform-wide incidents) is maintained, with the observability stack's per-tenant
dependency-health tagging (Ch.38 §4) automatically detecting and surfacing IdP-specific
auth failure spikes scoped to a single tenant — giving support the data to correctly
attribute the issue to the tenant's own IdP rather than this platform, closing Chapter 16's
gap.

## 4. Grader Spot-Audit Practice (Discharges Ch.24 Action Item)

A sample of human-graded `AssessmentGraded` events (Ch.23/24) is periodically reviewed by a
second qualified reviewer for grading-quality consistency — an operational quality
practice, not an architectural control, exactly as Chapter 24's Blue Team distinguished.
Sampling rate is risk-weighted (higher sampling for BR-015 regulated-vertical content).

## 5. Quarterly DR Drill Program (Discharges Ch.42 Action Item)

Executes Chapter 42 §5's full dependency-fallback inventory as a live (not tabletop-only)
exercise each quarter, rotating through: a regional failover drill, a critical-dependency
simulated outage (rotating through the Ch.35 §4 registry), and a compliance-tier
incident-response drill matching NFR-012's stricter on-call thresholds (Ch.15 §4) — this is
the mechanism that converts Chapter 42's RTO/RPO targets from "designed to achieve" to
"demonstrated," per that chapter's own CTO condition.

## Summary
This chapter delivers the full operational specification behind five previously
architecture-only mechanisms: a time-boxed, audited, quarterly-reviewed support-
impersonation model; tenant-IdP-outage attribution tooling; risk-weighted grader spot-
audits; and — most significantly — a live quarterly DR drill program that is the actual
validation mechanism Chapter 42 committed to needing.

## Open Questions
Specific drill rotation schedule and sampling rates — implementation-phase operational tuning, informed by real incident-frequency data.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Quarterly DR drills (§5) are operationally expensive and could be deprioritized under feature-delivery pressure, exactly as Chapter 41 warned for the certification program | High | Medium | [Ch. 47 — Governance](47-governance.md)'s quarterly review cadence should explicitly include DR-drill completion as a tracked governance item, not an easily-skipped engineering task |
| Support-impersonation quarterly review (§2) is a manual process without automated anomaly-detection tooling specified | Medium | Medium | Candidate for future automation once real usage-pattern data exists; acceptable as a manual process initially |

## Architecture Decisions
**ADR-083: Time-boxed, tenant-notified, quarterly-reviewed support-impersonation access model** — §2, discharges Ch.4/16 action items. **ADR-084: Live (not tabletop-only) quarterly DR drill program executing Ch.42's full dependency inventory** — §5, discharges Ch.42 action item and provides its validation mechanism.

## Future Research
Drill rotation schedule and audit sampling-rate tuning (implementation phase); automated impersonation anomaly-detection (future).

## Cross References
[Ch. 4](../part-1-foundations/04-user-personas.md) (support-impersonation persona) · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-008) · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-022) · [Ch. 16](../part-3-identity-organization/16-authentication.md) §5–6 · [Ch. 23](../part-4-learning-domain/23-assessment-engine.md), [Ch. 24](../part-4-learning-domain/24-question-bank.md) §5 · [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) §4 · [Ch. 38](../part-8-operations/38-observability.md) §4 · [Ch. 42](../part-8-operations/42-disaster-recovery.md) §6 · [Ch. 47](47-governance.md)

## Definition of Done
- [x] Support-impersonation access model fully specified, discharging Ch.4/16 action items
- [x] Tenant-IdP-outage communication process specified, discharging Ch.16 action item
- [x] Grader spot-audit practice specified, discharging Ch.24 action item
- [x] Quarterly DR drill program specified as Ch.42's validation mechanism

## Confidence Level
**High** — this chapter provides concrete operational specification for mechanisms whose architecture was already soundly established in prior chapters; low new-architectural-risk surface area.

## 6. Chapter Review

**Red Team:** The DR drill program (§5) rotates through scenarios quarterly, meaning any
single dependency or failure mode is only actually tested roughly once a year at best,
given the number of items in the Ch.35 §4 registry (7 entries) competing for quarterly
rotation slots — this could leave gaps of nearly a year between tests of any specific
dependency's failover behavior.

**Blue Team:** Accepted — valid scheduling-math concern. Addendum: the compliance-critical-
tier regional-failover drill (the single highest-consequence scenario, directly testing
NFR-012's 99.95% commitment) is elevated to every-quarter-guaranteed, not part of the
rotation; only the individual dependency-specific outage simulations rotate through the
remaining registry entries — ensuring the highest-stakes scenario is never more than one
quarter stale, while accepting slower cadence for lower-consequence dependency-specific
tests.

**CTO:** ADR-084 **Approved with Conditions** — condition is the Blue Team's
elevated-cadence carve-out for the compliance-tier regional-failover drill is binding,
ensuring the platform's core SLA commitment is never tested less than quarterly even as
other scenarios rotate more slowly.

---
*End of Chapter 48. Proceed to Chapter 49 — Maintenance.*
