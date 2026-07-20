# Chapter 40 — Security

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 39 — DevOps](39-devops.md) · Next: Ch. 41 — Compliance

## 1. Purpose

This chapter discharges two significant carried-forward action items: Chapter 11's
requirement for a formal 4-tier data-classification rubric (re-mapping all 17 bounded
contexts), and Chapter 26's requirement for rigorous key-management practices protecting
the certificate-signing private key. It also delivers NFR-017–022 in full.

## 2. Formal Data Classification Rubric (Discharges Ch.11 Action Item)

| Tier | Definition | Handling Requirement | Example (Ch.11 contexts) |
|---|---|---|---|
| **Public** | No confidentiality requirement | Standard controls only | Course catalog metadata (non-restricted content), public certificate-verification endpoint (Ch.26 §6) |
| **Internal** | Business-sensitive, not personal | Access-controlled, not encrypted-at-rest beyond baseline (NFR-018 still applies platform-wide) | Content authoring metadata, search index (Ch.29) |
| **Confidential-PII** | Personal data, GDPR/privacy-regulated | Encryption at rest+transit, RLS/ABAC enforced, erasable (Ch.12 §6) | Identity schema (Ch.16), Org Hierarchy (Ch.19), Notification contact info (Ch.34) |
| **Restricted-Evidentiary** | Compliance-evidentiary, immutable, highest consequence of compromise | All Confidential-PII controls **plus** immutability enforcement, digital-signature integrity (Ch.26 §3), HSM-backed signing key (§3 below) | Certification & Compliance (Ch.11 #10), Assessment submissions (Ch.11 #9) |

**Re-mapping the 17 contexts** against this rubric confirms Chapter 11 §2's informal
labels were directionally correct but formalizes exact handling requirements per tier —
notably, this rubric makes explicit that Assignment & Enrollment (Ch.11 #8, previously
"High") is **Confidential-PII**, not Restricted-Evidentiary, since individual assignment
records, while sensitive, don't carry the same immutable-evidentiary consequence as
issued certificates — a useful, previously-implicit distinction now made precise.

## 3. Key Management (Discharges Ch.26 Action Item)

| Dimension | Self-managed HSM | **Cloud-native KMS (HSM-backed managed service), Selected** |
|---|---|---|
| Fit to protecting the certificate-signing key (Ch.26 ADR-043) | Full control, but high operational/expertise burden | **HSM-grade protection without operating physical/virtual HSM infrastructure directly** |
| Cost (Ch.1 Principle 6) | High (specialized hardware/expertise) | Low, usage-based, and consistent with the platform's existing reliance on managed cloud services (Ch.12 Postgres, Ch.28 storage) |
| Key rotation | Manual process design required | Native support | 
| Final Recommendation | Rejected — disproportionate operational burden for this AKB's team | **Selected** |

**Decision:** The certificate-signing private key (Ch.26 §3) is generated and held in a
cloud-native, HSM-backed KMS, never exported in plaintext form, with automatic rotation and
access strictly limited to the Certification context's signing operation — directly
discharging Chapter 26's action item with a concrete mechanism.

## 4. NFR-017–022 Implementation Summary

| NFR | Implementation |
|---|---|
| NFR-017/018 (encryption in transit/at rest) | TLS 1.2+ everywhere (Ch.15 §6 mTLS internally); AES-256 at rest via cloud-provider-native encryption on all storage layers (Ch.12, Ch.28) |
| NFR-019 (annual pen testing) | Scheduled third-party engagement, scoped to include the compliance-critical tier with priority |
| NFR-020 (vuln remediation SLA) | Enforced via Ch.39's tracked (non-blocking-but-escalated) gate |
| NFR-021 (tenant isolation testing) | Enforced via Ch.39's blocking gate, reusing Ch.12 RLS + Ch.17 policy defense-in-depth |
| NFR-022 (break-glass auditability) | Ch.16 §5's separate token type, fully logged, reviewed per [Ch. 48 — Operations](../part-9-governance-future/48-operations.md) |

## Summary
A formal 4-tier data classification rubric (Public / Internal / Confidential-PII /
Restricted-Evidentiary) is established, discharging Chapter 11's action item and refining
the Assignment & Enrollment context's classification from an informal "High" to precisely
"Confidential-PII," distinct from Certification's "Restricted-Evidentiary" tier. A
cloud-native, HSM-backed KMS is selected to protect the certificate-signing key, discharging
Chapter 26's action item without the operational burden of self-managed HSM infrastructure.

## Open Questions
Whether Restricted-Evidentiary-tier data should additionally require customer-managed encryption keys (CMEK) for the largest/most regulated silo tenants (Ch.18 §3) — deferred to [Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md) as a contractual-tier feature question.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Cloud-KMS provider outage blocks certificate issuance (a new dependency introduced by §3's decision) | High | Low | Must be added to [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) §4's dependency-fallback registry — new entry required |
| Data-classification rubric (§2) applied inconsistently by implementation teams without tooling enforcement | Medium | Medium | Classification tags should be enforced as schema-level metadata, checked by [Ch. 39](39-devops.md)'s CI, not left to documentation alone |

## Architecture Decisions
**ADR-065: Formal 4-tier data classification rubric, re-mapping all 17 bounded contexts** — §2, discharges Ch.11 action item. **ADR-066: Cloud-native HSM-backed KMS for certificate-signing key protection** — §3, discharges Ch.26 action item.

## Future Research
Customer-managed encryption keys for silo tenants (Ch.46).

## Cross References
[Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-017–022) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) (action item) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) · [Ch. 16](../part-3-identity-organization/16-authentication.md) §5 · [Ch. 17](../part-3-identity-organization/17-authorization.md) · [Ch. 26](../part-4-learning-domain/26-certification.md) (action item) · [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) §4 · [Ch. 39](39-devops.md) · [Ch. 46](../part-9-governance-future/46-licensing.md) · [Ch. 48](../part-9-governance-future/48-operations.md)

## Definition of Done
- [x] Formal 4-tier classification rubric established and applied to all 17 contexts
- [x] Key-management mechanism selected via Technology Evaluation Template, discharging Ch.26
- [x] NFR-017–022 implementation summarized with concrete mechanisms

## Confidence Level
**High** — data classification and cloud-KMS patterns are both extremely well-established enterprise security practices; the specific tier assignments are a natural refinement of already-approved Chapter 11 sensitivity labels.

## 6. Chapter Review

**Red Team:** New KMS dependency (Risks table) is correctly flagged as needing to be added
to Chapter 35's registry, but this chapter doesn't actually add it — it just notes that it
should be added, leaving the registry technically out of sync until Chapter 35 (already
written) is revisited.

**Blue Team:** Accepted — process gap. Since [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) is
already written and marked its registry as "living" (per its own ADR-058 caveat), this is
exactly the kind of update that registry anticipated. This chapter formally records the
addition here as authoritative — **Ch.35 §4 registry addition: Cloud KMS — fallback
behavior: certificate issuance queues (does not fail) during a brief KMS outage, using the
same async-queue pattern as Ch.23's submission processing; sustained outage escalates per
NFR-012's compliance-tier incident process** — and flags Chapter 35 as needing a
consistency-pass update, rather than silently leaving the two chapters out of sync.

**CTO:** ADR-066 **Approved with Conditions** — condition is the Chapter 35 registry update
specified in the Blue Team response is applied (via a follow-up edit to Chapter 35) before
this AKB is considered internally consistent.

---
*End of Chapter 40. Proceed to Chapter 41 — Compliance.*
