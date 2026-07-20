# Chapter 26 — Certification

> Part IV — Learning Domain · [Index](../00-index.md) · Previous: [Ch. 25 — Assignment Engine](25-assignment-engine.md) · Next: Part V, Ch. 27 — Video Streaming

## 1. Purpose

Certification is the chapter every other Part IV chapter (and several Part I/II chapters)
has pointed to: Chapter 5 ADR-005 (version-pinning), Chapter 21's realized-branch-sequence
requirement, Chapter 6's NFR-026 export-integrity action item, FR-024/025/026/027, and
NFR-025 retention all converge here. This closes out Part IV.

## 2. Certificate Data Model (Fully Realizes Ch.5 ADR-005 + Ch.21 Addendum)

| Field | Source | Immutability |
|---|---|---|
| `learner_ref` (pseudonymized) | Ch.12 §6 | Fixed at issuance |
| `content_version_id` / `path_version_id` | Ch.12 §7, Ch.21 §3 | Fixed at issuance |
| **Realized step/branch sequence** | Ch.21 §7, Ch.22 §7 `PathProgress` | Fixed at issuance — discharges Ch.21's Red-Team-identified requirement |
| `score` / `grading_result` | Ch.23 `AssessmentGraded` event | Fixed at issuance |
| `grader_identity` + e-signature (where BR-015 healthcare-row applies) | Ch.23 §4, Ch.24 §2 | Fixed at issuance |
| `org_context` (tenant, org unit at time of issuance) | Ch.19 | Fixed at issuance |
| `issued_at` timestamp | System | Fixed at issuance |
| `expires_at` (nullable) | Ch.20 competency/role policy | Drives Ch.5 Phase 8 recertification loop |

Consistent with Chapter 12 §5, `Certificate` is an event-sourced aggregate — the
`CertificateIssued` event *is* the record; there is no separate mutable "certificate row"
to accidentally edit.

## 3. Export Integrity — Technology Evaluation (Discharges NFR-026 / Ch.6 Action Item)

| Dimension | Plain export (checksum only) | **Digital signature (asymmetric PKI), Selected** | Blockchain/distributed ledger |
|---|---|---|---|
| Proves integrity (not tampered) | Yes | Yes | Yes |
| Proves authenticity (genuinely issued by this platform) | **No — a checksum alone doesn't prove origin** | **Yes — signed with the platform's private key, verifiable with a published public key** | Yes |
| Fit to FR-007/FR-024 tension (Ch.6) | Insufficient — doesn't resolve the tension | **Resolves it — an exported, signed certificate remains verifiably authentic even outside the platform's control, satisfying both tenant export (FR-007) and evidentiary immutability (FR-024) simultaneously** | Resolves it, but see cost row |
| Cost / operational complexity (Ch.1 Principle 6 TCO) | Low | **Low-moderate — standard PKI infrastructure, well-understood** | High — introduces an entire new operational paradigm (distributed ledger infrastructure, consensus, key management at a different scale) for a benefit no greater than PKI signing already provides here |
| Hiring pool / 7-10yr viability | High | **High — PKI/digital signatures are foundational, decades-proven** | Lower — blockchain-specialist skillset, and enterprise-buyer skepticism of the technology for this use case is well-documented |
| Final Recommendation | Rejected — insufficient | **Selected** | Rejected — classic over-engineering trap for this problem shape; PKI signing achieves the same authenticity guarantee at far lower TCO |

**Decision:** Every issued certificate (and any export bundle, FR-007) is digitally signed
with a platform-held private key at issuance time; a publicly verifiable endpoint exposes
the corresponding public key so Auditor Alex (or any third party) can independently verify
authenticity of an exported record without needing platform access — directly satisfying
NFR-026 and fully resolving the FR-007/FR-024 tension Chapter 6's Red Team identified.

## 4. Auditor-Facing Lookup/Export (Satisfies FR-027 / NFR-034)

A dedicated, self-explanatory lookup-and-export UI (per Chapter 4's low-frequency/high-
consequence design principle) exposes signed PDF/CSV export directly from the
`Certificate` event log — no admin-tool detour required, satisfying NFR-034's <5%
task-failure usability target for a persona who uses this feature once a year.

## 5. Retention & Recertification (Satisfies NFR-025, Closes Ch.5 Phase 8 Loop)

Certificates are retained per the applicable regulatory profile's minimum (BR-015) even
post-offboarding (Ch.18 §5), enforced by retention policy at the `compliance_evidence`
schema level (Ch.12 §6) — not a manual process. `expires_at` firing triggers
`CertificationExpiring`/`CertificationExpired` → `RecertificationTriggered` (Ch.5 §3.8),
closing the loop back into Chapter 25's Assignment Engine.

## Summary
The Certificate aggregate is event-sourced and captures every field Chapter 5's ADR-005
and Chapter 21's realized-branch-sequence addendum require. Digital signature (PKI), not a
plain checksum or blockchain, is selected to resolve Chapter 6's FR-007/FR-024
export-integrity tension — publicly verifiable, low-TCO, and decades-proven, deliberately
rejecting blockchain as an over-engineered fit for this problem. A dedicated auditor
lookup/export UI satisfies FR-027/NFR-034, and retention/recertification close the loop
back into Chapter 25, completing Part IV's core compliance narrative.

## Open Questions
Certificate-revocation process (rare, e.g., issued in error, per Ch.5 §3.6's `CertificateRevoked` event) — the *event* exists, but the governance process for who can trigger it and under what evidentiary standard isn't specified here; deferred to [Ch. 41 — Compliance](../part-8-operations/41-compliance.md).

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Private signing key compromise would undermine the authenticity guarantee for all past and future certificates | Very High | Low | [Ch. 40 — Security](../part-8-operations/40-security.md) must specify key-management practices (HSM-backed key storage, rotation policy) at a rigor matching this consequence |
| `CertificateRevoked` governance gap (Open Questions) could allow inconsistent/ungoverned revocation | Medium | Low-Medium | Explicitly assigned to [Ch. 41](../part-8-operations/41-compliance.md), not silently dropped |

## Architecture Decisions
**ADR-042: Certificate captures realized branch/step sequence, grader identity, and e-signature alongside content/path version, as an event-sourced immutable record** — §2. **ADR-043: PKI digital signatures (not checksums or blockchain) for export integrity, resolving the FR-007/FR-024 tension** — §3.

## Future Research
Certificate-revocation governance process (Ch.41).

## Cross References
[Ch. 4](../part-1-foundations/04-user-personas.md) (Auditor Alex, NFR-034) · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) (ADR-005, Phase 8) · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-024–027) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §5–7 · [Ch. 18](../part-3-identity-organization/18-multi-tenancy.md) §5 · [Ch. 21](21-learning-paths.md) · [Ch. 25](25-assignment-engine.md) · [Ch. 40](../part-8-operations/40-security.md) · [Ch. 41](../part-8-operations/41-compliance.md)

## Definition of Done
- [x] Certificate data model fully realizes Ch.5 ADR-005 and Ch.21's branch-sequence addendum
- [x] Export-integrity mechanism selected via Technology Evaluation Template, resolving Ch.6's flagged tension
- [x] Auditor-facing UX specified against FR-027/NFR-034
- [x] Retention/recertification loop closed back to Ch.25

## Confidence Level
**High** — this chapter is the deliberate convergence point for numerous already-carefully-reasoned prior decisions; the one genuinely new decision (PKI vs. blockchain) is a well-established, low-risk industry-standard choice.

## 6. Chapter Review

**Red Team:** Public-key verification (§3) assumes a third party (an external auditor, a
regulator) knows to check the platform's public-key endpoint — if this verification step
isn't itself well-documented and discoverable, the authenticity guarantee is
technically real but practically unused, undermining the whole point of NFR-026.

**Blue Team:** Accepted — valid adoption-risk concern, distinct from the technical
soundness of the mechanism itself. Addendum: the exported certificate/record must include
**human-readable verification instructions and the verification URL directly in the export
artifact itself** (e.g., in the PDF footer), not only in separate developer documentation —
now a binding requirement on this chapter's export design, not left to chance discoverability.

**CTO:** ADR-042/043 **Approved with Conditions** — condition is the Blue Team's
in-artifact verification-instructions requirement is binding, ensuring NFR-026's
authenticity guarantee is actually discoverable and usable by Auditor Alex or a third-party
regulator, not merely theoretically available.

---
*End of Chapter 26. This closes Part IV — Learning Domain. Proceed to Part V — Media &
Discovery, beginning with Chapter 27 — Video Streaming.*
