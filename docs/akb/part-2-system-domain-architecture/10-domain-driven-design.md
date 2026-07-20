# Chapter 10 — Domain-Driven Design

> Part II — System & Domain Architecture · [Index](../00-index.md) · Previous: [Ch. 9 — Product Architecture](09-product-architecture.md) · Next: Ch. 11 — Bounded Contexts

## 1. Purpose

Formalize the Ubiquitous Language seeded in Chapter 1 and classify subdomains (core/
supporting/generic) so Chapter 11 can draw bounded-context boundaries against a stable
vocabulary and a clear value classification, rather than ad hoc service boundaries.

## 2. Ubiquitous Language (Glossary) — v1

| Term | Definition | First Appeared |
|---|---|---|
| Tenant | An enterprise customer organization; the top-level data-isolation boundary | Ch.1 |
| Learner | A person consuming learning content; may be employee, partner, or customer (Ch.4 personas) | Ch.1 |
| Org Unit | A node in a tenant's organizational hierarchy | Ch.1 |
| Competency | A defined skill/knowledge area with proficiency levels, mapped to roles | Ch.1, Ch.20 |
| Learning Path | An ordered/unordered collection of learning items toward a competency or role goal | Ch.1, Ch.21 |
| Enrollment | The binding of a learner to a learning item, with lifecycle state (Ch.5 §4) | Ch.5 |
| Assignment | A rule- or manually-driven instruction that a learner/cohort must complete a learning item | Ch.5 §3.2 |
| Content Version | An immutable, specific revision of a learning item, referenced by certificates (Ch.5 ADR-005) | Ch.5 |
| Certificate | An immutable, evidentiary record of completion pinning learner, content version, score, org context, timestamp | Ch.5 §3.6 |
| Cohort | A group of learners sharing enrollment/completion state for a learning item (Ch.5 §6, FR-017) | Ch.5, Ch.6 |
| Compliance Rule | A policy defining who must complete what, by when, recurring or one-time | Ch.5 §3.1/3.8 |
| Recertification | The cyclical re-triggering of an assignment upon certificate expiration | Ch.5 §3.8 |

This glossary is versioned; every later chapter that introduces a new domain term must add
it here rather than defining local synonyms.

## 3. Subdomain Classification (Strategic DDD)

| Subdomain | Type | Rationale | Design Implication |
|---|---|---|---|
| Compliance & Certification | **Core** | Direct realization of BR-002, the platform's primary differentiator and non-negotiable invariant (Ch.1 Principle 1) | Highest engineering investment; never outsourced/genericized |
| Assignment & Enrollment | **Core** | Dynamic, org-aware assignment (Ch.5 §3.2) is a key differentiator vs. incumbents (Ch.8 §5 item 10) | High investment |
| Competency Management | **Core** | Direct realization of BR-004 (internal mobility) | High investment |
| Assessment & Question Bank | **Core** | Regulatory-defensibility requirements (BR-015) exceed generic quiz-tool needs | High investment |
| Course/Content Management | **Supporting** | Necessary but largely a well-understood problem (import/organize/version content) | Moderate investment; benefits from patterns borrowed in Ch.8 §5 |
| Search & Recommendation | **Supporting** | Important for UX (FR-028/029) but not the platform's compliance-defensibility differentiator | Moderate investment; open to using well-proven off-the-shelf components (evaluated in Ch.29/30) |
| Reporting & Analytics | **Supporting** | Critical for BR-002/BR-003 KPIs but built on patterns (BI/warehousing) that are well-understood, not novel | Moderate investment |
| Notification | **Generic** | Solved problem industry-wide | Low investment; strong candidate for buy/integrate in [Ch. 34](../part-6-insight/34-notification-system.md) |
| Identity & Access | **Generic** (from this platform's perspective) | Enterprise IAM is a solved, standards-based problem (SAML/OIDC/SCIM) | Integrate against buyer's IdP (Ch.1 §2.2); do not build a competing IAM product |
| Video Streaming | **Generic** | Solved problem with mature managed providers | Integrate, per Ch.1 §2.2 |
| File Storage | **Generic** | Commodity cloud capability | Integrate/buy |
| Org Hierarchy Sync | **Supporting** | Necessary translation layer between generic HRIS and core Assignment/Competency logic | Moderate investment |

**Design implication carried into Chapter 11:** bounded-context granularity should be
finest (most decomposed, most engineering scrutiny) for Core subdomains, and coarsest
(potentially collapsed into a single "Platform Services" context, or replaced by
integration) for Generic subdomains — directly informing which of Ch.9's ~15-20 services
deserve the deepest investment.

## 4. Tactical DDD Patterns Adopted

| Pattern | Applied To | Notes |
|---|---|---|
| Aggregate | `Enrollment`, `Certificate`, `Assignment`, `CompetencyProfile` | Aggregate roots enforce Ch.5's state-machine invariants (e.g., a Certificate cannot be issued without a passing Assessment result) |
| Value Object | `ContentVersionReference`, `ComplianceRuleExpression`, `Score` | Immutable by construction — directly supports Ch.5 ADR-005/006 |
| Domain Event | The ~26 events inventoried in [Ch. 5 §5](../part-1-foundations/05-learning-lifecycle.md#5-consolidated-domain-event-inventory-event-storming-output) | Published on the event bus (Ch.9 §3) for cross-context consumption |
| Domain Service | `AssignmentRuleEvaluator`, `RecertificationScheduler` | Stateless logic spanning multiple aggregates |
| Anti-Corruption Layer (ACL) | At every integration boundary named in Ch.1 §2.2 (HRIS, IdP, authoring tools, video) | Prevents external systems' models (e.g., HRIS org data shape) from leaking into core domain model |

## Summary
This chapter established a versioned Ubiquitous Language (12 core terms), classified 12
subdomains as Core/Supporting/Generic (4/4/4 split — compliance, assignment, competency,
and assessment are Core), and adopted a standard tactical DDD pattern set including
mandatory anti-corruption layers at every external integration point from Ch.1 §2.2. The
Core/Supporting/Generic classification directly determines investment level and bounded-
context granularity in Chapter 11.

## Open Questions
- Is Search/Recommendation correctly classified Supporting rather than Core, given BR-016's
  emphasis on AI-driven personalization? Tentatively Supporting because it doesn't carry the
  compliance-defensibility invariant; revisit if [Ch. 30](../part-5-media-discovery/30-recommendation-engine.md) finds
  otherwise.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Glossary drifts as later chapters coin new terms without updating this chapter | Medium | Medium | Explicit instruction (§2) that new terms must be added here, not locally redefined |
| Generic subdomains (Identity, Video, Storage, Notification) under-scrutinized because "generic," missing tenant-specific compliance needs (e.g., data residency in storage) | Medium | Low-Medium | Cross-reference: generic classification affects *build* investment, not NFR compliance — Ch.28/16/27/34 must still satisfy all applicable NFRs from Ch.7 |

## Architecture Decisions
**ADR-013: Subdomain classification (Core/Supporting/Generic) directly determines both build-vs-integrate posture and bounded-context granularity** — Context: §3. Rejected alternative: uniform investment across all subdomains — rejected as inconsistent with Ch.1 Principle 6 (TCO discipline) and Ch.2 ADR-001 (integrate non-core). Review trigger: re-classify if a chapter's detailed analysis contradicts the provisional classification here (as flagged in Open Questions for Search/Recommendation).

## Future Research
Re-evaluate Search/Recommendation classification after Ch.29/30.

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) · [Ch. 5](../part-1-foundations/05-learning-lifecycle.md) · [Ch. 11](11-bounded-contexts.md) · [Ch. 20](../part-4-learning-domain/20-competency-management.md)

## Definition of Done
- [x] Ubiquitous Language glossary established (versioned, extensible)
- [x] All subdomains classified Core/Supporting/Generic with rationale
- [x] Tactical DDD pattern set adopted including mandatory ACLs

## Confidence Level
**High** — DDD strategic classification directly follows from already-established BR/FR priorities (Ch.2, Ch.6); low new-assumption surface area.

## 5. Chapter Review

**Red Team:** Identity & Access classified "Generic from this platform's perspective" is a
subtle sleight — the platform still builds substantial Authorization logic (org-hierarchy-
scoped RBAC/ABAC, FR-004) that is arguably Core, even if Authentication (SSO/SCIM) is
Generic/integrated. Conflating the two under one Generic label risks under-investing in
Authorization.

**Blue Team:** Accepted — valid distinction. §3's Identity & Access row is corrected: split
into "Authentication" (Generic, integrate per FR-001/002) and "Authorization" (**Supporting**,
build — org-hierarchy-scoped access control is bespoke enough to warrant real investment,
though not Core since it isn't the compliance-differentiating invariant itself).

**CTO:** Approved with the Red Team's Authorization reclassification applied. Action item:
[Ch. 17 — Authorization](../part-3-identity-organization/17-authorization.md) to be scoped with Supporting-tier investment,
not treated as a thin wrapper around Ch.16's Authentication integration.

---
*End of Chapter 10. Proceed to Chapter 11 — Bounded Contexts.*
