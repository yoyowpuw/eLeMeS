# Chapter 41 — Compliance

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 40 — Security](40-security.md) · Next: Ch. 42 — Disaster Recovery

## 1. Purpose

This chapter has accumulated more deferred action items than any other in the AKB. It
discharges: the trust-center deliverable (Ch.3), certificate-revocation governance
(Ch.26), the AI-content snapshot-scoping checklist ownership (Ch.31), and formalizes
NFR-023/024/025 (residency, erasure, retention) as auditable processes rather than
architectural properties alone.

## 2. Certification Program & Trust Center (Discharges Ch.3 Action Item)

| Target Certification | Rationale |
|---|---|
| SOC 2 Type II | Table-stakes for enterprise SaaS procurement (Ch.3 VRM stakeholder) |
| ISO 27001 | Expected by EU/international enterprise buyers |
| WCAG 2.1 AA attestation (VPAT) | Supports NFR-028 and government-sector procurement (BR-015) |

**Continuous compliance tooling — Technology Evaluation:**

| Dimension | Manual audit-prep process | **Continuous compliance automation platform (Vanta/Drata-class), Selected** |
|---|---|---|
| Fit to maintaining SOC 2 Type II year-round (not just at audit time) | High manual burden, evidence-collection gaps | **Automated evidence collection from Ch.39's CI/CD gates and Ch.38's observability stack directly** |
| Cost (Ch.1 Principle 6) | High (dedicated compliance-ops headcount) | Moderate — tooling cost offset by reduced manual labor |
| Final Recommendation | Rejected | **Selected** |

**Trust Center:** a public-facing security/compliance documentation page (certifications,
sub-processor list, data-flow diagrams derived from Chapter 1 §5's system context) is
published and kept current via the same continuous-compliance tooling — directly
discharging Chapter 3's action item to accelerate VRM/Third-Party Risk review cycles.

## 3. Certificate-Revocation Governance (Discharges Ch.26 Action Item)

| Aspect | Policy |
|---|---|
| Who can trigger | Admin Aisha role, requires a documented reason (structured reason-code field, not freeform) |
| Evidentiary standard | Revocation does not delete the original `CertificateIssued` event (immutability preserved per Ch.12 §5) — it appends a `CertificateRevoked` event with reason, creating a full, auditable before/after record |
| Downstream effect | Revocation triggers `RecertificationTriggered` (Ch.5 §3.8) if the underlying requirement is still active |
| Audit visibility | Revocations are surfaced distinctly (not hidden) in Auditor Alex's lookup view (Ch.26 §4) — a revoked certificate's history is itself compliance evidence |

## 4. AI-Content Snapshot-Scoping Checklist (Discharges Ch.31 Action Item)

This chapter owns and version-controls the checklist determining which AI-generated
interaction types require Chapter 12 §7-style snapshotting (per Chapter 31 ADR-051),
reviewed whenever [Ch. 31](../part-5-media-discovery/31-ai-integration.md) or its consuming contexts introduce a new
AI-powered interaction type — a standing governance process, not a one-time document.

## 5. Residency, Erasure, Retention — Auditable Process Layer

| NFR | Architectural Mechanism (already built) | Compliance Process Layer (added here) |
|---|---|---|
| NFR-023 (residency) | Ch.12 §4 region-pinning | Periodic automated residency-audit reports, evidenced to the Trust Center |
| NFR-024 (erasure) | Ch.12 §6 identity/evidence separability | Documented, timestamped erasure-request workflow with a 30-day SLA tracker, auditable end-to-end |
| NFR-025 (retention) | Ch.12 §6, Ch.26 §5 | Per-regulatory-profile retention-schedule registry (BR-015), reviewed periodically as regulations change |
| Minimum-cohort-size suppression (Ch.33 §6) | Ch.33's view-generation-time enforcement | This chapter confirms the policy default (5 learners) and owns any future threshold changes as a governed policy, not an ad hoc engineering parameter |

## Summary
A SOC 2 Type II / ISO 27001 / WCAG-VPAT certification program, supported by continuous
compliance automation tooling, delivers a public Trust Center that discharges Chapter 3's
VRM action item. Certificate revocation is formalized as an append-only, reason-coded,
fully auditable governance process rather than a bare technical event. This chapter takes
ownership of the AI-content snapshot-scoping checklist Chapter 31 required, and adds an
auditable process layer — periodic residency audits, a tracked erasure-request SLA, and a
regulatory retention-schedule registry — on top of the already-built architectural
mechanisms from Chapter 12.

## Open Questions
Specific continuous-compliance tooling vendor selection deferred to Ch.46 Licensing.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Certification program (§2) is a multi-month undertaking that could be deprioritized against feature delivery pressure | High (BR-002/BR-015 depend on it for regulated-vertical sales) | Medium | [Ch. 47 — Governance](../part-9-governance-future/47-governance.md) should establish certification milestones as a tracked organizational priority, not solely an engineering backlog item |
| Retention-schedule registry (§5) drifts out of date as regulations change across jurisdictions | Medium | Medium | Periodic (at minimum annual) legal review cadence, coordinated with Legal (vendor-side, Ch.3 §4) |

## Architecture Decisions
**ADR-067: Continuous compliance automation platform, not manual audit-prep process, supporting a public Trust Center** — §2, discharges Ch.3 action item. **ADR-068: Certificate revocation as an append-only, reason-coded, auditable governance process** — §3, discharges Ch.26 action item.

## Future Research
Continuous-compliance tooling vendor selection (Ch.46); annual retention-schedule legal review cadence establishment (Ch.47).

## Cross References
[Ch. 3](../part-1-foundations/03-stakeholders.md) (Trust Center action item) · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) Phase 10 · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §4, §6 · [Ch. 26](../part-4-learning-domain/26-certification.md) (revocation action item) · [Ch. 31](../part-5-media-discovery/31-ai-integration.md) (checklist action item) · [Ch. 33](../part-6-insight/33-analytics.md) §6 · [Ch. 39](39-devops.md) · [Ch. 46](../part-9-governance-future/46-licensing.md) · [Ch. 47](../part-9-governance-future/47-governance.md)

## Definition of Done
- [x] Certification program and Trust Center specified, discharging Ch.3 action item
- [x] Certificate-revocation governance formalized, discharging Ch.26 action item
- [x] AI snapshot-scoping checklist ownership established, discharging Ch.31 action item
- [x] Auditable process layer added on top of Ch.12's residency/erasure/retention mechanisms

## Confidence Level
**High** — this chapter is primarily governance-process formalization on top of already-approved architectural mechanisms, which is inherently lower-risk than net-new architecture decisions.

## 6. Chapter Review

**Red Team:** The certification program (§2) lists target certifications and tooling, but
doesn't address the genuinely hard part: SOC 2 Type II specifically requires demonstrating
controls operated effectively over a period (typically 6-12 months) *before* certification
is achievable — this has a lead-time implication for go-to-market timing that isn't
acknowledged.

**Blue Team:** Accepted — a real and important gap. Addendum: SOC 2 Type II's
observation-period requirement means the certification timeline must begin as early as
possible relative to the platform's technical readiness, not after — this is now flagged
as a binding sequencing constraint for [Ch. 47 — Governance](../part-9-governance-future/47-governance.md) and
[Ch. 50 — Future Roadmap](../part-9-governance-future/50-future-roadmap.md) to reflect in program sequencing, since it
affects when regulated-vertical sales (BR-015) can realistically begin.

**CTO:** ADR-067 **Approved with Conditions** — condition is
[Ch. 47](../part-9-governance-future/47-governance.md)/[Ch. 50](../part-9-governance-future/50-future-roadmap.md) must explicitly sequence the SOC
2 Type II observation-period lead time into program planning, per the Blue Team addendum.
ADR-068 **Approved**.

---
*End of Chapter 41. Proceed to Chapter 42 — Disaster Recovery.*
