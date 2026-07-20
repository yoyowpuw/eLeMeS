# Chapter 21 — Learning Paths

> Part IV — Learning Domain · [Index](../00-index.md) · Previous: [Ch. 20 — Competency Management](20-competency-management.md) · Next: Ch. 22 — Course Management

## 1. Purpose

Deliver FR-012 (paths composed of ordered/unordered, mixed-modality content), define path
versioning consistent with Chapter 5's ADR-005/Chapter 12 §7 content-version-pinning
mechanism, and establish the branching/adaptive-path model that Chapter 30's
recommendation tier will later plug into.

## 2. Path Structure

| Aggregate (Ch.11 #6) | Key Attributes |
|---|---|
| `LearningPath` | name, target competency/role (links to Ch.20 `RoleProfile`), path version |
| `PathStep` | ordering (strict sequence, unordered set, or conditional branch), reference to a `Course`/`Assessment`/ILT session (Ch.22, Ch.23), completion criteria |
| `PathProgress` | per-learner (or per-cohort, see §4) progress through steps — a read-model projection off Enrollment events (Ch.11 #8), not a duplicate source of truth |

`PathStep` ordering supports three modes: **strict sequence** (must complete in order,
common for safety-critical training), **unordered set** (complete all, any order), and
**conditional branch** (next step determined by prior assessment score or a rule — e.g.,
skip remedial content if pre-assessment passed). This directly satisfies FR-012's
"ordered/unordered mixed-modality" requirement plus adds branching as a superset capability
justified by the widely-cited "one-size-fits-all path" complaint against academic-style LMS
content models (Ch.8 §3.1–3.2 patterns).

## 3. Path Versioning (Consistent with Ch.5 ADR-005 / Ch.12 §7)

A `LearningPath` version is pinned at `Enrollment` time — a learner enrolled in Path v1
completes Path v1 even if the path is edited mid-consumption, **unless** an admin explicitly
triggers a re-enrollment into the new version. This mirrors Chapter 5's still-open
mid-consumption content-update question (assigned to Chapter 22) by adopting the more
conservative default at the path level now, while leaving Chapter 22 free to define
step-level content-update behavior independently.

## 4. Cohort/Team Path Layer (Cross-Reference, Not Owned Here)

Per Chapter 5/6's FR-017 assignment, the cohort/team-level state model is owned by
[Ch. 22](22-course-management.md) and [Ch. 25](25-assignment-engine.md), not this chapter.
`PathProgress` is designed to be aggregatable by either an individual learner or a cohort
reference (per Ch.11 #8's `Cohort` aggregate) without structural change — this chapter
ensures the path data model doesn't preclude that design, without re-solving it here.

## Summary
Learning Paths support strict-sequence, unordered, and conditional-branch step ordering,
satisfying FR-012 and extending it with a branching capability justified by benchmark
findings. Path versions are pinned at enrollment time by default, consistent with — though
independently decided from — Chapter 5's still-open mid-consumption content-update
question. The data model is deliberately cohort-aggregatable without owning that logic,
deferring correctly to Chapters 22/25 per the existing action-item assignment.

## Open Questions
Whether conditional-branch logic should be expressible by tenant admins via a simple rule builder or require engineering involvement per path — a UX/tooling question for implementation phase, not core architecture.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Enrollment-time version pinning conflicts with Ch.22's eventual mid-consumption content-update resolution if Ch.22 chooses a different default | Medium | Medium | Explicit cross-reference recorded here; [Ch. 22](22-course-management.md) must reconcile or explicitly override this chapter's default, not silently diverge |

## Architecture Decisions
**ADR-034: LearningPath versions pinned at enrollment time by default; path structure supports strict/unordered/conditional-branch step ordering** — §2–3.

## Future Research
Rule-builder tooling for conditional branches (implementation phase).

## Cross References
[Ch. 5](../part-1-foundations/05-learning-lifecycle.md) (ADR-005, mid-consumption question) · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-012, FR-017) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #6 · [Ch. 20](20-competency-management.md) · [Ch. 22](22-course-management.md) · [Ch. 25](25-assignment-engine.md)

## Definition of Done
- [x] Path structure specified (ordered/unordered/branching), satisfying FR-012
- [x] Versioning default specified, cross-referenced to Ch.5's open mid-consumption question
- [x] Cohort-aggregatable design confirmed without re-owning Ch.22/25's assignment

## Confidence Level
**High** — path structure is a natural extension of already-established lifecycle (Ch.5) and content-versioning (Ch.12) decisions.

## 5. Chapter Review

**Red Team:** Conditional branching based on assessment score creates a subtle interaction
with Chapter 5 ADR-005 (certificate version-pinning) — if a branch skips remedial content
based on a pre-assessment, does the resulting certificate need to reflect which branch was
taken, not just which path version? This isn't addressed.

**Blue Team:** Accepted — valid and directly analogous to the content-version-pinning
principle already established. Addendum: `CertificateIssued` events (Ch.5 §3.6, Ch.12 §7)
must reference not just the `LearningPath` version but the **specific sequence of
`PathStep`s actually completed** (the realized branch path), for full audit defensibility —
this is now a binding requirement carried to [Ch. 26 — Certification](26-certification.md).

**CTO:** ADR-034 **Approved with Conditions** — condition is
[Ch. 26 — Certification](26-certification.md) must extend certificate data to capture the
realized branch/step sequence, per the Blue Team addendum, not just path version.

---
*End of Chapter 21. Proceed to Chapter 22 — Course Management.*
