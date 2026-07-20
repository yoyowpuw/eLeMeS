# Chapter 22 — Course Management

> Part IV — Learning Domain · [Index](../00-index.md) · Previous: [Ch. 21 — Learning Paths](21-learning-paths.md) · Next: Ch. 23 — Assessment Engine

## 1. Purpose

This chapter discharges more carried-forward action items than any other so far: FR-013
(SCORM/xAPI/cmi5 import), FR-014 (content versioning), FR-017/cohort-team learning (Ch.5/6
CTO action item), the mid-consumption content-update policy (open since Ch.5), FR-020 (ILT
scheduling), and confirms the realized-branch-sequence tracking Chapter 21 assigned here.

## 2. Content Runtime — Technology Evaluation

| Dimension | Build custom SCORM/xAPI/cmi5 runtime | **License a third-party conformance engine (Rustici-class), Selected** | Open-source runtime (e.g., community SCORM players) |
|---|---|---|---|
| Fit to NFR-043 (ADL-certified conformance) | High risk — SCORM's spec has decades of edge-case quirks; building a fully conformant runtime is a notoriously underestimated effort industry-wide | **Low risk — this is the licensed engine's entire business, already ADL-certified** | Moderate risk — conformance varies, maintenance uncertain |
| Cost (Ch.1 Principle 6 TCO) | High engineering cost, ongoing spec-maintenance burden (SCORM 1.2, 2004, xAPI, cmi5 are four distinct specs) | Predictable licensing cost | Low license cost, high hidden maintenance cost |
| Consistency with Ch.1 §2.2 (integrate, don't rebuild adjacent categories) | Contradicts the boundary — SCORM conformance is exactly the kind of "solved problem" this AKB commits to not rebuilding | **Consistent** | Partially consistent, but risk profile is worse |
| Exit strategy | N/A (owned) | Moderate — content itself remains standards-based (SCORM packages are portable); only the runtime is licensed | Low — community projects can be abandoned |
| Final Recommendation | Rejected | **Selected** | Rejected — conformance risk too high for BR-015's regulatory-defensibility bar |

**Decision:** License a proven, ADL-certified SCORM/xAPI/cmi5 conformance engine as the
content-playback runtime embedded within the Course & Content Management context (Ch.11
#7), rather than building or adopting an unproven open-source runtime — directly satisfying
NFR-043 and staying consistent with Chapter 1 §2.2.

## 3. Content Versioning — Confirming and Extending Ch.12 §7's Mechanism

Chapter 12 §7 already specified hash-based, insert-only content versioning. This chapter
confirms the owning aggregate (`Course`/`ContentVersion`, Ch.11 #7) and extends it
explicitly to imported marketplace content (Ch.8 FR-038): imported external content is
hashed and versioned identically to internally authored content at import time — no
re-hosting required, resolving the Ch.8 Blue Team's operational-cost concern with a
concrete implementation, not just a policy statement.

## 4. Mid-Consumption Content-Update Policy (Resolves Ch.5 Open Question)

Consistent with Chapter 21 ADR-034's path-versioning default: **a learner's in-progress
enrollment continues on the `ContentVersion` active at enrollment time.** If content is
updated mid-consumption, the learner is *not* silently bumped to the new version; an admin
may explicitly trigger re-enrollment into the new version if the update is substantive
(e.g., a regulatory correction), which resets progress and is itself an auditable action.
This resolves Chapter 5's Open Question with a policy consistent across both the path level
(Ch.21) and the content level (here) — no divergence between the two, closing the risk
Chapter 21 flagged.

## 5. Cohort/Team Learning Model (Discharges FR-017 / Ch.5–6 CTO Action Item)

| Aggregate (Ch.11 #7/#8) | Key Attributes |
|---|---|
| `Cohort` | roster (learner references), associated `LearningPath` or `Course`, completion policy |
| Completion policy | Configurable: **all-must-complete** (e.g., a team safety drill), **threshold** (e.g., 80% of the team), or **individual-within-cohort** (shared scheduling/roster only, no group completion gating) |
| `CohortProgress` | Aggregates individual `PathProgress`/`Enrollment` states (Ch.21 §2) against the completion policy — a projection, not a duplicate state machine, preserving Chapter 5 §4's individual enrollment state machine as the source of truth |

This directly answers Chapter 5's Red Team finding (§6.1: "cohort/team-based learning
requires a group-level state machine layered above the individual model") — `Cohort` is
that layer, implemented as a policy-driven aggregation over existing individual state, not
a parallel state machine requiring new lifecycle phases.

## 6. ILT Scheduling & Attendance (Satisfies FR-020)

`Cohort` doubles as the ILT session roster; a `SessionAttended` event (Ch.5 §5) updates
`CohortProgress` identically to any other completion signal, so ILT requires no special-
cased logic beyond session scheduling/roster metadata — reusing the cohort model rather
than building a separate ILT subsystem.

## 7. Realized Branch/Step Sequence Tracking (Confirms Ch.21 Addendum)

`PathProgress` (Ch.21 §2) records the actual sequence of `PathStep`s completed, including
which conditional branch was taken. This record is available to
[Ch. 26 — Certification](26-certification.md) at `CertificateIssued` time, satisfying
Chapter 21's Red-Team-identified requirement that certificates reflect the realized path,
not just the path version.

## Summary
Course Management licenses a third-party ADL-certified conformance engine rather than
building or adopting an unproven SCORM/xAPI/cmi5 runtime, consistent with Chapter 1's
integration boundary. Content versioning (Chapter 12 §7) is confirmed and extended to
marketplace content without re-hosting. The mid-consumption content-update policy is
resolved consistently with Chapter 21's path-versioning default (pin at enrollment,
explicit re-enrollment for updates). A policy-driven `Cohort` aggregation layer discharges
FR-017 without introducing a parallel state machine, and doubles as the ILT roster
mechanism, and `PathProgress` confirms it carries the realized-branch data
Chapter 26 will need.

## Open Questions
Whether cohort completion-policy changes mid-session (e.g., admin lowers the threshold) should retroactively affect already-computed `CohortProgress` — an edge case for implementation-phase UX definition.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Third-party conformance engine becomes a hard external dependency for all content playback | High (single point of failure for the core learning-delivery path) | Low-Medium | Directly falls under NFR-051 (third-party degradation handling, Ch.7); [Ch. 42](../part-8-operations/42-disaster-recovery.md) must include this engine explicitly in its dependency inventory |
| Admin-triggered re-enrollment (§4) resets progress, creating a support/learner-frustration risk if overused | Medium | Low | UX guardrails (confirmation, impact preview) — implementation-phase concern, noted here for that team's awareness |

## Architecture Decisions
**ADR-035: License a third-party ADL-certified SCORM/xAPI/cmi5 conformance engine rather than build or adopt open-source** — §2. **ADR-036: Content-version pinned at enrollment time, consistent with Ch.21 ADR-034; explicit admin-triggered re-enrollment required for mid-consumption updates** — §4, resolves Ch.5 Open Question. **ADR-037: Cohort/team learning realized as a policy-driven aggregation layer over individual enrollment state, not a parallel state machine** — §5, discharges FR-017.

## Future Research
Cohort completion-policy retroactivity UX (implementation phase).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) §2.2 · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) (Open Questions, CTO action item) · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-013, 014, 017, 020) · [Ch. 8](../part-1-foundations/08-benchmark-analysis.md) (FR-038) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #7/#8 · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §7 · [Ch. 21](21-learning-paths.md) · [Ch. 26](26-certification.md) · [Ch. 42](../part-8-operations/42-disaster-recovery.md)

## Definition of Done
- [x] Content runtime approach selected via Technology Evaluation Template (NFR-043)
- [x] Content versioning confirmed/extended to marketplace content (FR-038)
- [x] Mid-consumption content-update policy resolved (closes Ch.5 Open Question)
- [x] Cohort/team model specified as aggregation layer (discharges FR-017 CTO action item)
- [x] ILT scheduling folded into cohort model (FR-020)
- [x] Realized branch/step tracking confirmed for Ch.26

## Confidence Level
**High** — every major decision either directly discharges a named, already-analyzed action item or extends an already-approved mechanism (Ch.12 §7, Ch.21 ADR-034) for consistency, minimizing new unreviewed judgment calls.

## 8. Chapter Review

**Red Team:** The third-party conformance engine (ADR-035) is a single vendor for a
platform-critical capability (all content playback) — this is a significant vendor
concentration risk not weighed against a multi-vendor or engine-abstraction strategy.

**Blue Team:** Accepted as a real concern, but rejected as a reason to change ADR-035: the
alternative (building an abstraction layer over multiple conformance engines) would
duplicate significant complexity for a risk better mitigated operationally (contract terms,
source-code-escrow clauses, per Ch.1 Principle 5's exit-strategy discipline) than
architecturally. Addendum: [Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md) must ensure the licensing
agreement includes source-escrow or comparable exit-strategy protection for this
specific dependency, given its criticality.

**CTO:** ADR-035/036/037 **Approved**. Action item: [Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md)
must secure exit-strategy contract protections for the conformance-engine vendor
specifically, given the vendor-concentration risk identified here.

---
*End of Chapter 22. Proceed to Chapter 23 — Assessment Engine.*
