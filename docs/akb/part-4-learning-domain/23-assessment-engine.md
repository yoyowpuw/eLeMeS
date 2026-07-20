# Chapter 23 — Assessment Engine

> Part IV — Learning Domain · [Index](../00-index.md) · Previous: [Ch. 22 — Course Management](22-course-management.md) · Next: Ch. 24 — Question Bank

## 1. Purpose

Deliver FR-022 (configurable assessment types), FR-023 (manual grading workflow with audit
trail), FR-025 (e-signature), and the throughput/latency targets NFR-005/NFR-008, as a Core
subdomain (Ch.10 §3) — this is one of the three compliance-critical-tier contexts (Ch.11
§5, Ch.15 §4).

## 2. Assessment Submission Architecture

Consistent with Chapter 12 §5's event-sourcing decision for this context: `AssessmentStarted
→ AssessmentSubmitted → AssessmentGraded → AssessmentPassed/Failed` (Ch.5 §3.5) is the
event log itself, not a side effect of a CRUD update — grading state is fully
reconstructable from the log, which is precisely the auditability BR-002/BR-015 require.

**Submission processing (satisfies NFR-005, P99 < 1s acknowledge):** submission is
acknowledged synchronously (write to the event log) but grading (especially auto-grading of
complex question types or queued manual grading) proceeds asynchronously — decoupling
"the learner's answer is safely recorded" from "the learner's answer is scored,"
satisfying NFR-005 without requiring grading itself to be instant.

**Peak throughput (satisfies NFR-008, 50k submissions/min):** submissions are queued
through the compliance-tier's pre-warmed capacity (Ch.15 §4) rather than processed
purely reactively, matching the known bursty pattern (Ch.1 §3) of synchronized cohort
assessment events.

## 3. Assessment Types & Grading — Technology Evaluation

| Dimension | Build custom auto-grading only | **Build custom auto-grading + integrate third-party proctoring, Selected** | Build custom proctoring in-house |
|---|---|---|---|
| Fit to FR-022 (practical/observed checklist types) | Partial — auto-grading alone can't cover practical/checklist assessment types requiring human judgment | **Full — manual grading workflow (FR-023) covers practical/checklist; proctoring integration covers high-stakes exam integrity** | Full, but at very high cost |
| Fit to BR-015 (regulated exam integrity, e.g., FINRA continuing-ed exams) | Weak — no answer to exam-integrity requirements some regulated exams carry | **Strong — proctoring is a well-established Generic-subdomain integration (identity verification, environment monitoring), consistent with Ch.1 §2.2** | Strong, but rebuilds a solved, legally sensitive problem (biometric/privacy regulation exposure) |
| Cost / TCO (Ch.1 Principle 6) | Low | Moderate (integration + licensing) | Very high, plus new regulatory surface area (biometric data handling) |
| Final Recommendation | Rejected — insufficient for BR-015's highest-stakes exam scenarios | **Selected** | Rejected — proctoring is exactly the kind of Generic, legally-sensitive capability Ch.1 §2.2 says to integrate, not build |

**Decision:** Build the core assessment/grading engine (question delivery, scoring,
manual-grading workflow) natively as Core-subdomain investment; integrate a third-party
proctoring vendor for the subset of high-stakes assessments that require it, via the
Integration Gateway context (Ch.11 #17) and its Anti-Corruption Layer discipline (Ch.10 §4)
— proctoring data never leaks into the core `Submission` aggregate's model directly.

## 4. Manual Grading Workflow (Satisfies FR-023)

`Submission`s requiring manual grading (practical/checklist/freeform types) enter a grader
queue, with grader-assignment, grading audit trail (who graded, when, any
comments/rationale), and — for BR-015 healthcare-row content — mandatory e-signature
(FR-025) captured as part of the `AssessmentGraded` event itself, not a separate,
detachable record, ensuring the signature cannot be repudiated after the fact.

## Summary
The Assessment Engine is event-sourced (per Chapter 12 §5), decoupling synchronous
submission acknowledgment (NFR-005) from asynchronous grading to meet peak-throughput
targets (NFR-008) via pre-warmed compliance-tier capacity. Auto-grading and manual-grading
workflows are built natively as Core-subdomain investment; proctoring for high-stakes
regulated exams is integrated from a third-party vendor rather than built, consistent with
Chapter 1's Generic-subdomain integration boundary, with e-signature captured as an
inseparable part of the grading event to satisfy FR-025's non-repudiation need.

## Open Questions
Specific proctoring vendor selection deferred to implementation-phase procurement, evaluated against Ch.46 Licensing.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Proctoring vendor integration introduces exactly the kind of external dependency NFR-051 warns about, on a compliance-critical path | High | Low-Medium | Proctoring applies only to a subset of high-stakes assessments (not the general enrollment/completion path), bounding blast radius; still must be included in [Ch. 42](../part-8-operations/42-disaster-recovery.md)'s dependency inventory per the existing NFR-051 obligation |
| Manual grading queue backlog during peak periods delays certification issuance, indirectly affecting BR-002 compliance deadlines | Medium | Medium | [Ch. 32 — Reporting](../part-6-insight/32-reporting.md) should surface grader-queue backlog as an operational metric, not just a hidden internal detail |

## Architecture Decisions
**ADR-038: Event-sourced Assessment context with async grading decoupled from sync submission acknowledgment** — §2. **ADR-039: Build core grading engine natively; integrate third-party proctoring for high-stakes exams via ACL, do not build proctoring in-house** — §3.

## Future Research
Proctoring vendor selection (Ch.46).

## Cross References
[Ch. 5](../part-1-foundations/05-learning-lifecycle.md) §3.5 · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-022, 023, 025) · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-005, 008, 051) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #9 · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §5 · [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) §4 · [Ch. 24](24-question-bank.md) · [Ch. 42](../part-8-operations/42-disaster-recovery.md)

## Definition of Done
- [x] Submission/grading architecture specified against NFR-005/008
- [x] Assessment-type/proctoring approach selected via Technology Evaluation Template
- [x] Manual grading workflow specified with e-signature non-repudiation

## Confidence Level
**High** — event-sourcing choice directly inherits Chapter 12's already-approved decision; proctoring build-vs-integrate follows Chapter 1's established boundary consistently.

## 6. Chapter Review

**Red Team:** No mention of how auto-grading handles partial credit or complex question
types (e.g., drag-and-drop, simulations) at a technical level — "auto-grading" is treated
as a black box.

**Blue Team:** Accepted — fair, but correctly scoped out: specific question-type grading
logic is a [Ch. 24 — Question Bank](24-question-bank.md) concern (question types and their
scoring rules are defined there), not an Assessment Engine architecture concern — this
chapter's job is the submission/grading *workflow*, not per-question-type scoring logic.
Cross-reference made explicit rather than left implicit.

**CTO:** ADR-038/039 **Approved**. Note: per-question-type scoring logic is explicitly
assigned to [Ch. 24](24-question-bank.md), not missing from this chapter's scope.

---
*End of Chapter 23. Proceed to Chapter 24 — Question Bank.*
