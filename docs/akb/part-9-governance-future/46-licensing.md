# Chapter 46 — Licensing

> Part IX — Governance & Future · [Index](../00-index.md) · Previous: [Ch. 45 — Cost Optimization](../part-8-operations/45-cost-optimization.md) · Next: Ch. 47 — Governance

## 1. Purpose

This chapter has accumulated more vendor-selection deferrals than any other: CIAM (Ch.16),
conformance-engine exit protections (Ch.22), proctoring (Ch.23), video (Ch.27), foundation
model (Ch.31), continuous-compliance tooling (Ch.41), managed-open-source observability
(Ch.45), plus storage, OLAP, and notification vendors. Rather than picking specific
vendors (a procurement activity outside architectural authority, consistent with Chapter
1 §7's scope boundary), this chapter delivers the **standard vendor-evaluation framework
and contract-term requirements** every one of those selections must satisfy, plus the
platform's own commercial licensing model to tenants.

## 2. Platform Licensing Model (To Tenants)

Consistent with Chapter 45's tier/region-differentiated cost model: **per-learner
subscription pricing, tiered by isolation level** (pooled vs. silo, Ch.12 §2) and by
regulated-vertical feature bundle (BR-015 capabilities — e-signature, proctoring
integration, dedicated compliance reporting — as an add-on tier, not bundled into every
subscription, keeping the base product price competitive for tenants who don't need
BR-015's heaviest capabilities).

## 3. Standard Vendor Contract-Term Requirements

Every vendor selection deferred to this chapter by a prior chapter must satisfy these
baseline terms before selection is finalized:

| Requirement | Rationale | Source |
|---|---|---|
| Exit-strategy protection (source-escrow, data-export guarantee, or standards-based portability) | Ch.1 Principle 5 | Applied with extra rigor to the conformance engine (Ch.22 §8) given its criticality |
| Regional processing/storage capability where NFR-023 applies | Data residency | Explicitly gating for the video vendor (Ch.27 §5) and any vendor touching Confidential-PII/Restricted-Evidentiary data (Ch.40 §2) |
| SLA terms matching or exceeding this platform's own NFR-012 tier commitments | Can't promise a tenant 99.95% while depending on a vendor with a weaker SLA, for compliance-critical-tier dependencies specifically (Ch.15 §4) | New — flagged explicitly here as a gating criterion, not previously stated this precisely |
| Security/compliance certification parity (SOC 2 at minimum) for any vendor touching Confidential-PII or Restricted-Evidentiary data | Ch.40 §2 rubric | Consistent with this platform's own certification posture (Ch.41 §2) |

## 4. Silo-Tenant SLA Contract Terms (Discharges Ch.18 Open Question)

Silo tenants receive a **distinct, longer provisioning SLA** (vs. NFR-010's 4-hour pooled
target) reflecting dedicated-cluster provisioning time (Ch.12 §2), disclosed as an explicit
contract term rather than an undifferentiated blanket promise — resolving Chapter 18's
Open Question with a commercial (contract-language), not architectural, answer, consistent
with that Open Question's original framing.

## Summary
Rather than selecting specific vendors (a procurement activity outside this AKB's
architectural authority), this chapter delivers the standard contract-term framework —
exit-strategy protection, regional-processing capability, SLA parity with this platform's
own tier commitments, and security-certification parity — that governs every vendor
selection deferred here from Chapters 16 through 45. The platform's own tenant-facing
licensing model is tiered by isolation level and regulated-vertical feature bundle,
consistent with Chapter 45's cost model, and Chapter 18's silo-tenant provisioning-SLA
question is resolved as a contract-term distinction.

## Open Questions
None new — this chapter is explicitly a framework/consolidation chapter; actual vendor selections remain implementation-phase procurement activities outside architectural scope, consistent with Ch.1 §7.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Procurement teams select vendors without applying this chapter's framework, since it isn't a technical control | High | Medium | Should be codified as a formal procurement checklist owned by [Ch. 47 — Governance](47-governance.md), not left as an AKB reference alone |
| SLA-parity requirement (§3) may be commercially unrealistic for some vendor categories (e.g., a small proctoring vendor may not offer 99.95%) | Medium | Medium | Where unavailable, the platform's own dependency-fallback registry (Ch.35 §4) becomes the primary mitigation instead of vendor SLA — explicitly acceptable when documented |

## Architecture Decisions
**ADR-079: Standard vendor contract-term framework (exit-strategy, residency, SLA-parity, certification-parity) applied to every deferred vendor selection** — §3. **ADR-080: Tenant licensing tiered by isolation level and regulated-vertical feature bundle** — §2.

## Future Research
Formal procurement checklist adoption (Ch.47).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) §7, Principle 5 · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §2 · [Ch. 16](../part-3-identity-organization/16-authentication.md), [Ch. 22](../part-4-learning-domain/22-course-management.md), [Ch. 23](../part-4-learning-domain/23-assessment-engine.md), [Ch. 27](../part-5-media-discovery/27-video-streaming.md), [Ch. 31](../part-5-media-discovery/31-ai-integration.md), [Ch. 41](../part-8-operations/41-compliance.md), [Ch. 45](../part-8-operations/45-cost-optimization.md) (deferred vendor sources) · [Ch. 18](../part-3-identity-organization/18-multi-tenancy.md) (Open Question) · [Ch. 47](47-governance.md)

## Definition of Done
- [x] Tenant-facing licensing model specified, consistent with Ch.45's cost model
- [x] Standard vendor contract-term framework specified, applicable to all deferred selections
- [x] Ch.18's silo-tenant SLA Open Question resolved as a contract-term distinction

## Confidence Level
**High** for the framework structure (well-established enterprise vendor-management practice) — actual vendor selections remain unresolved by design, consistent with this being a procurement, not architecture, activity.

## 5. Chapter Review

**Red Team:** This chapter, like Chapters 44 and 45, defers all concrete decisions
(specific vendors) — a pattern of three consecutive chapters producing frameworks rather
than decisions could reasonably be read as this AKB running out of genuinely architectural
content to decide by Part IX, with the interesting work having concluded in Parts II-VIII.

**Blue Team:** This is a fair pattern observation, but the underlying reason differs
per chapter: Chapter 44 defers because empirical data doesn't exist yet (a genuine
prerequisite gap), Chapter 45 defers specific dollar figures for the same reason (already
partly conceded and corrected via the CTO's follow-up condition), and this chapter defers
specific *vendor names* because vendor selection is legitimately a procurement/commercial
activity, not an architecture one — consistent with Chapter 1 §7's explicit scope boundary
excluding "specific content licensing negotiations with marketplace vendors" from this
AKB's scope, extended here consistently to all vendor categories. The pattern is not
running out of content; it's correctly recognizing the boundary between architecture
(what this AKB owns) and procurement (what it doesn't), a distinction Chapter 1 drew
deliberately at the very start.

**CTO:** ADR-079/080 **Approved**. The Red Team's pattern observation is noted as valid
meta-commentary but doesn't change this chapter's verdict — the scope boundary it reflects
was intentional from Chapter 1, not a late-stage improvisation.

---
*End of Chapter 46. Proceed to Chapter 47 — Governance.*
