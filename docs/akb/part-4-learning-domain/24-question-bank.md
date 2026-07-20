# Chapter 24 — Question Bank

> Part IV — Learning Domain · [Index](../00-index.md) · Previous: [Ch. 23 — Assessment Engine](23-assessment-engine.md) · Next: Ch. 25 — Assignment Engine

## 1. Purpose

Deliver FR-021 (reusable, tagged, randomized, difficulty-rated question bank) and, per
Chapter 23's Blue Team scoping, own per-question-type scoring logic — the piece Chapter 23
explicitly deferred here.

## 2. Question Types & Scoring Rules

| Question Type | Scoring Approach |
|---|---|
| Multiple choice / true-false | Deterministic auto-grading, immediate |
| Multiple response | Deterministic, configurable partial-credit rule (all-or-nothing vs. per-correct-option) |
| Fill-in-the-blank / short text | Pattern/rule-based auto-grading (exact match, regex, or configurable synonym list); routes to manual grading (Ch.23 §4) if no rule matches confidently |
| Drag-and-drop / ordering / matching | Deterministic auto-grading against a defined correct-state structure |
| Simulation / practical checklist | Always routes to manual grading (Ch.23 §4) — no auto-grading attempted, since these assess real-world judgment |
| Freeform/essay | Manual grading only, optionally AI-assisted first-pass scoring (see §4) with mandatory human confirmation before `AssessmentGraded` fires — never fully automated for compliance-critical assessments, per Ch.1 Principle 4 |

## 3. Reusability, Tagging & Randomization (Satisfies FR-021)

`Question` aggregates are tagged (topic, difficulty, competency mapping to Ch.20) and
versioned identically to course content (Ch.12 §7 hash-based, insert-only) — a question
used in a regulated assessment pins its exact version at submission time, extending Chapter
5 ADR-005's principle down to the question level, not just the course/path level. Random
question selection (per-learner shuffled question sets from a tagged pool) is supported at
assessment-configuration time.

## 4. AI-Assisted Grading Guardrail (Cross-Reference to Ch.31)

Per §2's freeform/essay row, any AI-assisted grading assistance is explicitly a
**first-pass suggestion only** — the `AssessmentGraded` event (Ch.23 §4) always requires a
human grader's confirmation for freeform content, directly enforcing Chapter 1 Principle 4
(AI additive, never load-bearing for compliance completion) at the question-scoring level,
not just the platform-feature level where it was originally stated.

## Summary
Question Bank defines per-type scoring rules ranging from deterministic auto-grading to
mandatory manual grading for judgment-based question types, with AI-assisted grading
permitted only as a human-confirmed first-pass suggestion for freeform content. Questions
are tagged, randomizable, and version-pinned using the same hash-based mechanism already
established for courses and content, extending Chapter 5 ADR-005's evidentiary principle
to the individual-question level.

## Open Questions
Whether formal psychometric item analysis (e.g., Item Response Theory difficulty calibration from real answer data) is warranted — flagged as a genuinely advanced capability, deferred to [Ch. 50 — Future Roadmap](../part-9-governance-future/50-future-roadmap.md) rather than assumed necessary for initial scope.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Pattern/regex-based short-text auto-grading produces false negatives, routing legitimate correct answers to unnecessary manual review | Medium | Medium | Configurable synonym lists and confidence thresholds; tune based on real usage data post-launch |
| AI-assisted grading guardrail (§4) circumvented under grading-backlog pressure (Ch.23 Risk: grader-queue backlog) | High (would violate Ch.1 Principle 4) | Low, if enforced at the event level | `AssessmentGraded` event schema structurally requires a human-grader identity field for freeform content — not optional, not bypassable by configuration |

## Architecture Decisions
**ADR-040: Question-type-specific scoring rules, ranging from deterministic to mandatory-manual, with AI grading restricted to human-confirmed first-pass suggestions only** — §2, §4.

## Future Research
Psychometric/IRT difficulty calibration (Ch.50).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) (Principle 4) · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) (ADR-005) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §7 · [Ch. 20](20-competency-management.md) · [Ch. 23](23-assessment-engine.md) · [Ch. 31](../part-5-media-discovery/31-ai-integration.md)

## Definition of Done
- [x] Per-question-type scoring rules specified (discharges Ch.23 scoping deferral)
- [x] Tagging/randomization/versioning specified against FR-021 and extended ADR-005
- [x] AI-assisted grading guardrail specified as structurally enforced, not policy-only

## Confidence Level
**High** — scoring-rule taxonomy is standard assessment-engineering practice; the version-pinning extension is a direct, low-risk application of an already-approved pattern.

## 5. Chapter Review

**Red Team:** The "structurally required human-grader identity field" guardrail (§4 Risk
mitigation) prevents *skipping* human confirmation, but doesn't prevent a human grader from
rubber-stamping an AI suggestion without genuine review — the guardrail addresses process
compliance, not review quality.

**Blue Team:** Accepted as a real, but different, problem — genuine review-quality
assurance (e.g., spot-auditing graders) is an operational/quality-management concern for
[Ch. 48 — Operations](../part-9-governance-future/48-operations.md), not something the data model can enforce. The
architectural guardrail correctly does what architecture can do (make the human step
mandatory and attributable); it was never claimed to guarantee review diligence, and the
distinction is now made explicit rather than implied.

**CTO:** ADR-040 **Approved**. Action item: [Ch. 48 — Operations](../part-9-governance-future/48-operations.md) should
consider grader spot-audit practices as an operational quality measure, distinct from this
chapter's structural guardrail.

---
*End of Chapter 24. Proceed to Chapter 25 — Assignment Engine.*
