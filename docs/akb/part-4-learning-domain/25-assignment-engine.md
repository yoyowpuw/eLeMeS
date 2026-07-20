# Chapter 25 — Assignment Engine

> Part IV — Learning Domain · [Index](../00-index.md) · Previous: [Ch. 24 — Question Bank](24-question-bank.md) · Next: Ch. 26 — Certification

## 1. Purpose

Deliver FR-015 (dynamic, re-evaluable, history-preserving rule-based assignment), confirm
FR-016/NFR-009's bulk-reassignment mechanism from Chapter 13 in this context's terms, and
implement recertification-triggering (Ch.5 Phase 8) — the third and final compliance-
critical-tier context (Ch.11 §5).

## 2. Assignment Rule Model (Satisfies FR-015)

The `AssignmentRuleEvaluator` domain service (Ch.10 §4) evaluates rules as **live queries
against current org/role state** (Ch.19's closure-table hierarchy, cached per Ch.19 §3),
not static snapshots — e.g., "all learners in Org Unit X with Role Y must complete Z
annually." Rule evaluation is triggered by:

| Trigger | Behavior |
|---|---|
| `OrgUnitReparented` / `RoleChanged` events (Ch.19 §4) | Re-evaluates affected learners' assignment eligibility; new assignments created, no historical assignment events deleted (append-only `Assignment` aggregate, consistent with Ch.12 §5 event sourcing) |
| Rule definition change (admin edits a compliance policy) | Re-evaluates the full affected population — this is the operation bulk-reassignment (§3) exists to handle at scale |
| `CertificationExpired` (Ch.5 Phase 8) | Triggers `RecertificationTriggered`, re-entering this context's assignment flow — the cyclical loop closing back from Ch.26 |

## 3. Bulk Reassignment (Confirms Ch.13 §4 Mechanism in This Context)

Chapter 13 already specified the idempotency-key + async-job pattern satisfying NFR-009's
1M-learner/15-minute target. This chapter confirms the Assignment Engine is the owning
service for that job type, processing reassignment batches as new `Assignment` events
(append-only) rather than mutating existing assignment records — preserving the audit
trail (BR-002) even during high-volume bulk operations, which a naive UPDATE-based bulk
operation would not.

## 4. Cohort Assignment (Interacts with Ch.22 §5)

Assigning a `Cohort` (Ch.11 #8, defined in Ch.22 §5) follows the same rule-evaluation path
as individual assignment, with the rule's target resolving to a cohort reference rather
than an individual learner reference — no separate cohort-assignment code path, keeping
FR-017's cohort model and FR-015's dynamic-assignment model compositional rather than
parallel implementations.

## Summary
The Assignment Engine implements dynamic, live-query-based rule evaluation over org/role
state (not static snapshots), triggered by org-change events, rule-definition changes, and
recertification cycles — with every resulting assignment recorded as an immutable,
append-only event, preserving full audit history even through bulk reassignment operations
that reuse Chapter 13's idempotency-key pattern. Cohort assignment composes with individual
assignment rather than requiring a separate code path.

## Open Questions
Rule-conflict resolution (a learner matching two contradictory rules, e.g., two different due dates for the same compliance requirement) — deferred to implementation-phase rule-engine design, flagged here as non-trivial.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Live rule re-evaluation on every org-change event at BR-008 scale (a 3M-learner tenant reorg) creates a re-evaluation storm | High | Medium | Batches re-evaluation through the same async, checkpointed bulk mechanism (§3) rather than synchronous per-event processing; NFR-009's SLA already assumes this scale |
| Rule-conflict edge case (Open Questions) silently resolved by whichever rule evaluates last, producing non-deterministic behavior | Medium | Medium | Must be explicitly designed, not left implicit, at implementation time — flagged strongly here to prevent silent non-determinism |

## Architecture Decisions
**ADR-041: Assignment rules are live queries against current org/role state, not static snapshots; all resulting assignments are append-only events** — §2, fully realizes Ch.5 §3.2's original design constraint.

## Future Research
Rule-conflict resolution policy (implementation phase).

## Cross References
[Ch. 5](../part-1-foundations/05-learning-lifecycle.md) §3.2, Phase 8 · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-015, 016, 017) · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-009) · [Ch. 10](../part-2-system-domain-architecture/10-domain-driven-design.md) §4 · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #8 · [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) §4 · [Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md) · [Ch. 22](22-course-management.md) §5 · [Ch. 26](26-certification.md)

## Definition of Done
- [x] Live-query rule model specified, fully realizing Ch.5's original design constraint
- [x] Bulk reassignment confirmed as owned here, reusing Ch.13's mechanism with append-only preservation
- [x] Cohort assignment shown to compose with, not duplicate, individual assignment logic

## Confidence Level
**High** — this chapter is substantially the payoff of design constraints and mechanisms already carefully established in Chapters 5, 12, 13, and 19; low new-assumption surface area.

## 5. Chapter Review

**Red Team:** The rule-conflict Open Question is flagged but its risk (non-deterministic
compliance-deadline assignment) is arguably severe enough — directly touching BR-002's
core compliance-risk-reduction promise — to warrant more than a deferral to
"implementation-phase design."

**Blue Team:** Accepted — elevated. Addendum: the default conflict-resolution policy is
now specified rather than left fully open: **when rules conflict, the most restrictive
outcome wins (earliest due date, mandatory over optional)** — a conservative default
consistent with BR-002's risk-reduction priority — configurable per-tenant only if a tenant
explicitly opts into a different resolution policy, never silently non-deterministic.

**CTO:** ADR-041 **Approved with Conditions** — condition is the Blue Team's
most-restrictive-wins default is now binding, closing the severity gap the Red Team
identified rather than leaving it as an open implementation risk.

---
*End of Chapter 25. Proceed to Chapter 26 — Certification.*
