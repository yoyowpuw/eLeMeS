# Chapter 31 — AI Integration

> Part V — Media & Discovery · [Index](../00-index.md) · Previous: [Ch. 30 — Recommendation Engine](30-recommendation-engine.md) · Next: Part VI, Ch. 32 — Reporting

## 1. Purpose

This chapter closes Part V by discharging FR-030 (AI-assisted authoring), FR-031
(conversational assistant, lowest-confidence tier), and — most structurally significant —
FR-032/Chapter 5's action item requiring a discrete snapshot/version mechanism for
AI-generated content to remain compatible with Chapter 5 ADR-005 and Chapter 12 §7.

## 2. Foundation Model Access — Technology Evaluation

| Dimension | Build/train custom LLM | **Integrate third-party foundation model API, Selected** |
|---|---|---|
| Fit to Ch.1 §2.2 / Ch.1 Principle 6 (TCO, integrate generic capability) | Training/operating a foundation model is far outside this platform's differentiating core (Ch.10 §3 doesn't even classify AI as its own subdomain — it's a capability layered on Core/Supporting contexts) | **Consistent — foundation models are a commodity capability accessed via API, analogous to Ch.16's CIAM buy decision** |
| Cost | Extremely high (training infra, ongoing model ops) | Usage-based, predictable | 
| Time-to-value | Very slow | Fast |
| Final Recommendation | Rejected | **Selected** |

**Decision:** Integrate a third-party foundation model API (vendor selection deferred to
[Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md)) for both FR-030 (authoring assistance) and FR-031
(conversational assistant), with model access abstracted behind an internal interface (an
Anti-Corruption Layer, Ch.10 §4) so the specific vendor is swappable without touching
domain logic — preserving Ch.1 Principle 5's exit-strategy discipline for what could
otherwise become the platform's least portable dependency.

## 3. Dynamic Content Snapshot Mechanism (Discharges Ch.5/Ch.6 Action Item — FR-032)

Any AI-generated content that a learner consumes as part of a compliance-tracked
interaction (e.g., an AI-tutor's explanation counted toward a learning objective, or
AI-assisted-authored content published for consumption) is **snapshotted and hash-versioned
at the moment of publish or consumption**, using the identical mechanism Chapter 12 §7
already established for conventional content — no new versioning system is built. This is
the concrete answer Chapter 5 deferred: dynamic/AI content is not exempt from ADR-005; it
is made compliant by snapshotting it into the same immutable content-version model the
instant it becomes something a certificate could reference.

**Purely exploratory AI interactions** (e.g., a learner asking an open-ended question to an
AI tutor that isn't tied to a specific graded objective) are explicitly **not** snapshotted
under this mechanism — only interactions that could plausibly feed a `CertificateIssued`
event require it, keeping the compliance-versioning overhead scoped to where it's actually
needed.

## 4. Guardrails (Confirms Ch.1 Principle 4, Ch.24 §4)

| Guardrail | Mechanism |
|---|---|
| AI never load-bearing for compliance completion (Ch.1 Principle 4) | Core enrollment/assessment/certification flows (Ch.23/25/26) have zero runtime dependency on the AI integration layer — confirmed architecturally via Ch.15 ADR-025's dependency-direction discipline, extended here |
| AI-assisted grading always human-confirmed (Ch.24 §4) | Already structurally enforced at the `AssessmentGraded` event schema level — this chapter adds no new mechanism, only confirms consistency |
| Kill criteria for FR-031 (Ch.6 Open Question) | Deferred to [Ch. 50 — Future Roadmap](../part-9-governance-future/50-future-roadmap.md) as originally assigned — not resolved here, since kill criteria are a product/roadmap governance decision, not an architecture one |

## Summary
A third-party foundation model API is integrated (not built/trained in-house) behind an
Anti-Corruption Layer, consistent with the platform's established Generic-capability
buy-and-abstract pattern. The long-outstanding Chapter 5/6 action item is discharged: any
compliance-relevant AI-generated content is snapshotted using Chapter 12 §7's existing
immutable-versioning mechanism, scoped only to interactions that could feed a certificate —
purely exploratory AI use is exempt. Chapter 1 Principle 4 and Chapter 24's grading
guardrail are both confirmed architecturally consistent, with FR-031's kill criteria
correctly left to Chapter 50 rather than decided here.

## Open Questions
Foundation model vendor selection (Ch.46). Whether the ACL abstraction (§2) should support simultaneous multi-vendor use (e.g., different models for different tasks) — deferred to implementation phase.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Foundation model vendor outage affects authoring/assistant features | Low (by design, per §4's non-load-bearing guardrail) | Low-Medium | Covered by NFR-051/NFR-016; no new mitigation needed beyond what's already specified |
| Snapshot-scoping judgment call (§3, "plausibly feed a certificate") is ambiguous in edge cases, risking inconsistent application | Medium | Medium | [Ch. 22](../part-4-learning-domain/22-course-management.md)/[Ch. 26](../part-4-learning-domain/26-certification.md) implementation teams should maintain a concrete, reviewed checklist of which interaction types require snapshotting, not rely on case-by-case judgment |

## Architecture Decisions
**ADR-050: Integrate third-party foundation model API behind an internal ACL; do not build/train custom models** — §2. **ADR-051: AI-generated compliance-relevant content is snapshotted via the existing Ch.12 §7 mechanism at publish/consumption time; purely exploratory AI use is exempt** — §3, discharges the Ch.5/Ch.6 action item.

## Future Research
Vendor selection (Ch.46); FR-031 kill criteria (Ch.50, already assigned).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) (Principle 4) · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) (ADR-005, action item) · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-030–032) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §7 · [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) (ADR-025) · [Ch. 24](../part-4-learning-domain/24-question-bank.md) §4 · [Ch. 46](../part-9-governance-future/46-licensing.md) · [Ch. 50](../part-9-governance-future/50-future-roadmap.md)

## Definition of Done
- [x] Foundation model access approach selected via Technology Evaluation Template
- [x] Dynamic-content snapshot mechanism specified, discharging the long-standing Ch.5/Ch.6 action item
- [x] Non-load-bearing and human-confirmation guardrails confirmed architecturally consistent
- [x] FR-031 kill-criteria question correctly left to Ch.50, not answered prematurely

## Confidence Level
**High** — this chapter's central mechanism (§3) reuses an already-approved pattern rather than inventing new infrastructure, and the buy-behind-ACL decision is consistent with every prior Generic-subdomain decision in this AKB.

## 6. Chapter Review

**Red Team:** The "plausibly feed a certificate" snapshot-scoping test (§3, also flagged in
Risks) is genuinely soft — this is the kind of ambiguous boundary that tends to be
interpreted inconsistently by different implementation teams over a 7-10 year horizon,
risking exactly the compliance gap ADR-005 exists to prevent.

**Blue Team:** Accepted — the Risk entry already names this, but the mitigation (a
"concrete, reviewed checklist") is underspecified as stated. Strengthened addendum: the
checklist must be a **versioned, governed artifact owned by [Ch. 41 — Compliance](../part-8-operations/41-compliance.md)**
(not an informal team habit), reviewed whenever a new AI-powered interaction type is
introduced — elevating this from an implementation nicety to a binding governance
requirement.

**CTO:** ADR-050 **Approved**. ADR-051 **Approved with Conditions** — condition is
[Ch. 41 — Compliance](../part-8-operations/41-compliance.md) must own and formally govern the
snapshot-scoping checklist per the strengthened Blue Team addendum, not leave it to
ad hoc engineering judgment.

---
*End of Chapter 31. This closes Part V — Media & Discovery. Proceed to Part VI — Insight,
beginning with Chapter 32 — Reporting.*
