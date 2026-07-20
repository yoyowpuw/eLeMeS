# Chapter 49 — Maintenance

> Part IX — Governance & Future · [Index](../00-index.md) · Previous: [Ch. 48 — Operations](48-operations.md) · Next: Ch. 50 — Future Roadmap

## 1. Purpose

Nearly every Technology Evaluation in this AKB cited a "7-10 year viability" horizon
(Ch.1 Principle 6, NFR-036) as a selection criterion without specifying how that horizon is
actually *maintained*, not just assumed at selection time. This chapter delivers that
maintenance discipline, plus React Native dependency upkeep (Ch.36 Risk) and API
deprecation lifecycle enforcement (NFR-037).

## 2. Technology-Viability Review Cadence (Operationalizes NFR-036)

Every major technology decision recorded as an ADR in this AKB (PostgreSQL, Kotlin/JVM,
Kafka, React/React Native, OpenTelemetry stack, etc.) is re-evaluated **every 2 years**
against its original Technology Evaluation Template criteria — not to reflexively replace
technology, but to catch early warning signs (declining hiring-pool health, maintenance
stagnation, a materially better alternative emerging) while there is still runway to plan a
migration deliberately, rather than reactively, consistent with Ch.1 Principle 6's original
intent. This is the concrete mechanism giving NFR-036 teeth beyond a one-time selection
criterion.

## 3. Dependency Maintenance (Discharges Ch.36 Risk)

React Native's native-module bridge dependency risk (flagged in Ch.36) is managed via a
standing quarterly dependency-update cycle (OS-version compatibility, security patches),
tracked as a scheduled maintenance obligation, not ad hoc reactive updates — extending the
same discipline to the platform's other key dependencies (the conformance engine, CIAM SDK,
foundation model API versions).

## 4. API Deprecation Lifecycle (Enforces NFR-037)

Chapter 13 specified the 12-month parallel-support policy; this chapter operationalizes
its enforcement: a deprecation tracker with automated `Sunset`-header-based monitoring of
actual integrator (Ch.4, Integrator Ivan) migration progress, escalating outreach as the
12-month window closes — ensuring the policy is actively managed, not just documented.

## 5. Documentation & Glossary Upkeep (Extends Ch.10 §2)

The Ubiquitous Language glossary (Ch.10 §2) and this AKB itself are treated as living
artifacts: any implementation-phase decision that introduces new domain vocabulary or
deviates from a recorded ADR must update the source chapter or record a superseding ADR —
preventing the documentation-implementation drift risk implicit in any architecture
document that stops being maintained the moment implementation begins.

## Summary
This chapter gives the "7-10 year viability" criterion cited throughout the AKB an actual
enforcement mechanism: a 2-year technology-viability review cadence re-applying each major
ADR's original Technology Evaluation Template. It also operationalizes React Native's
dependency-maintenance risk (Ch.36), enforces the API deprecation policy's actual
monitoring (not just its existence, Ch.13), and commits this AKB and its glossary (Ch.10)
to remaining living documents rather than a one-time deliverable.

## Open Questions
Specific technical-debt tracking tooling/process — implementation-phase decision, not architecturally load-bearing enough for a dedicated evaluation.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| 2-year review cadence (§2) becomes a check-box exercise rather than genuine reconsideration, echoing Chapter 47's governance-ceremony risk | Medium | Medium | Should feed the same [Ch. 47 — Governance](47-governance.md) quarterly-cadence accountability trail, not run as an isolated, disconnected process |
| Documentation drift (§5) still occurs in practice despite the stated policy, since enforcement depends on implementation-team discipline this document cannot compel | Medium | Medium-High | Honestly acknowledged rather than assumed away — the same organizational-commitment caveat Chapter 47's Red Team raised applies here too |

## Architecture Decisions
**ADR-085: 2-year technology-viability review cadence, re-applying each ADR's original Technology Evaluation Template** — §2, operationalizes NFR-036 platform-wide. **ADR-086: This AKB and its Ch.10 glossary are living documents requiring active maintenance through implementation** — §5.

## Future Research
Technical-debt tracking tooling selection (implementation phase).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) Principle 6 · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-036, 037) · [Ch. 10](../part-2-system-domain-architecture/10-domain-driven-design.md) §2 · [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) §3 · [Ch. 36](../part-7-platform-integration/36-mobile-strategy.md) (Risk) · [Ch. 47](47-governance.md)

## Definition of Done
- [x] Technology-viability review cadence specified, giving NFR-036 an enforcement mechanism
- [x] React Native dependency maintenance discharged (Ch.36 Risk)
- [x] API deprecation lifecycle enforcement specified beyond policy statement
- [x] AKB/glossary living-document commitment made explicit

## Confidence Level
**Medium-High.** The mechanisms specified are sound and directly extend already-established patterns — **High** confidence on design. Real-world adherence depends on organizational discipline this document cannot enforce by itself — **Medium** confidence on outcome, honestly reflected in the Risks table rather than concealed.

## 6. Chapter Review

**Red Team:** The 2-year review cadence (§2) doesn't specify what happens if a review
concludes a technology *should* be replaced — there's no migration-decision process
specified, only a detection mechanism. Detecting that PostgreSQL or Kotlin/JVM should be
reconsidered without a defined process for what happens next is only half a solution.

**Blue Team:** Accepted — genuine gap. Addendum: a technology flagged for replacement by
the 2-year review triggers the **same Red Team/Blue Team/CTO ADR process this entire AKB
used** (per Chapter 47 ADR-081's institutionalization) to evaluate a migration decision —
this chapter doesn't need to invent a new process, since Chapter 47 already provides the
right one; this chapter's job was only to specify the *trigger* (§2's cadence), and Chapter
47 supplies the *response* mechanism. The connection is now made explicit.

**CTO:** ADR-085 **Approved with Conditions** — condition is the explicit linkage to
Chapter 47's institutionalized review process (per the Blue Team response) is binding,
ensuring a detected technology-viability concern has a defined path to resolution, not just
detection.

---
*End of Chapter 49. Proceed to Chapter 50 — Future Roadmap.*
