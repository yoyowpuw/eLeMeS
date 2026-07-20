# Chapter 2 — Business Requirements

> Part I — Foundations · [Index](../00-index.md) · Previous: [Ch. 1 — Overview](01-enterprise-lms-overview.md) · Next: Ch. 3 — Stakeholders

## 1. Purpose of This Chapter

Chapter 1 defined *what* an enterprise LMS is and the frame we operate inside. This chapter
converts that frame into **traceable business requirements** — the concrete, numbered
statements that every later architecture decision must be justifiable against. It also
resolves three Open Questions left unresolved in Chapter 1: the meaning of "millions of
users," the regulated-vertical reference profile, and whether AI demand is real or
architecture-team enthusiasm.

Every requirement below gets a stable ID (`BR-###`) so later chapters (Functional
Requirements, Non-Functional Requirements, ADRs) can cite it instead of re-deriving intent.

---

## 2. Business Drivers — Why Build (or Buy) an Enterprise LMS At All

Before any feature discussion, the Business Analyst and CTO roles jointly interrogate the
first-principles question: **why does this need to exist as a platform investment, rather
than being bought as SaaS off the shelf?**

### 2.1 Build vs. Buy vs. Integrate — At the Platform Level

This is the highest-level Build/Buy/Integrate decision in the whole AKB; every later
chapter's per-capability Build/Buy/Integrate analysis is subordinate to it.

| Option | Description | Why Considered | Why Rejected / Accepted |
|---|---|---|---|
| **Buy commercial suite** (Docebo, Cornerstone, SAP SuccessFactors Learning, Saba/Cornerstone) | License an existing enterprise LMS outright | Fastest time-to-value; vendor carries compliance certifications already | **Rejected as the AKB's premise** — the mandate for this AKB is to *build*, driven by: (a) need for deep product-embedded differentiation (competency data feeding proprietary talent systems), (b) data-residency/control requirements that multi-region SaaS vendors may not meet per-tenant, (c) long-term TCO crossover favoring owned platform at the assumed scale (Ch. 1 §3). This is a business decision this AKB treats as *given*, not re-litigated per chapter — see [Ch. 8](08-benchmark-analysis.md) for why each incumbent falls short of at least one driver. |
| **Buy + heavily customize** | License a suite with an extensibility layer (e.g., Docebo APIs) | Lower build risk | Rejected — enterprise LMS suites' extensibility ceilings historically fail exactly at the deep-customization asks (custom competency models, bespoke compliance workflows) that justify this program; documented per-incumbent in [Ch. 8](08-benchmark-analysis.md) |
| **Build on open-source core** (Moodle, open-edX) | Fork/extend an OSS LMS | Avoids reinventing basics | Rejected as the default — architecture in [Ch. 8](08-benchmark-analysis.md) shows OSS cores optimize for academic, not enterprise-compliance, data models; adopting one would import years of schema debt inconsistent with Ch. 1 §3's 7–10 year horizon. Individual OSS *components* (not the whole core) remain viable per-capability (e.g., a search engine, a video pipeline) — evaluated per chapter. |
| **Build enterprise-native, integrate everything non-core** | Build the compliance/competency/assessment core; integrate identity, video, content-authoring, live-classroom, data warehouse | — | **Accepted.** Matches Ch. 1 §2.2 scope boundary exactly. |

**BR-001:** The platform SHALL be built as an owned, enterprise-native system of record for
learning, integrating (not rebuilding) identity, video transport, authoring tools, and live
classroom, per Ch. 1 §2.2.

### 2.2 Strategic Business Goals

| ID | Goal | Business Rationale |
|---|---|---|
| BR-002 | Reduce compliance risk exposure | Auditable, defensible completion records reduce regulatory fine and litigation exposure — the single largest quantifiable ROI driver for enterprise L&D buyers |
| BR-003 | Reduce time-to-competency for revenue-driving roles | Faster ramp for sales/support roles is a direct revenue lever, not just a cost center — differentiates this program from "LMS as cost center" framing |
| BR-004 | Enable workforce mobility / internal talent marketplace | Competency data ([Ch. 20](../part-4-learning-domain/20-competency-management.md)) feeds internal mobility, a top-3 stated driver in enterprise L&D benchmarks industry-wide |
| BR-005 | Consolidate fragmented point-solution spend | Enterprises often run 3–5 overlapping learning tools per business unit; consolidation is a direct OPEX reduction case |
| BR-006 | Support M&A and org-restructuring agility | Multi-tenant, hierarchy-aware design ([Ch. 18](../part-3-identity-organization/18-multi-tenancy.md), [Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md)) must allow tenants/org units to be re-parented without data migration projects |

---

## 3. Quantified Scale Requirements (Resolving Ch. 1 Open Question)

Chapter 1 flagged that "millions of users" was not precise enough to drive sharding
decisions. This section commits to concrete, sourced-from-assumption numbers, explicitly
labeled as **assumed contractual envelope** since no live customer contract exists yet —
later real deals may change these, but downstream architecture needs a number to design
against.

| Parameter | Assumed Value | Basis |
|---|---|---|
| BR-007: Total platform learners (aggregate, all tenants) | Up to 50 million | Aggregate across a portfolio of Fortune 500 tenants over a 5-year horizon |
| BR-008: Largest single tenant | Up to 3 million learners | Reference: largest known enterprise workforces (retail/logistics multinational scale) |
| BR-009: Typical enterprise tenant | 10,000–250,000 learners | Median Fortune 500 employee count range |
| BR-010: Peak concurrent active sessions (single large tenant) | 150,000 | Assumed ~5% concurrent-of-total during a mandatory compliance deadline spike |
| BR-011: Peak concurrent active sessions (platform-wide) | 1,000,000 | Multiple large tenants' deadlines can coincide (e.g., calendar-year-end compliance windows) |
| BR-012: Assessment submissions per peak minute | 50,000 | Derived from BR-011 assuming a synchronized cohort assessment event |
| BR-013: Content catalog size (single large tenant) | 100,000+ items (courses, assessments, paths, videos) | Enterprise catalogs blending internal + marketplace-federated content |
| BR-014: Video minutes stored (platform-wide, steady state) | 10+ million minutes, growing | Reference-scale assumption for [Ch. 27](../part-5-media-discovery/27-video-streaming.md), [Ch. 28](../part-5-media-discovery/28-file-storage.md) |

**Both single-tenant ceiling AND aggregate matter** (resolving Ch. 1's ambiguity directly):
sharding/partitioning strategy ([Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md)) must handle both a
very large single tenant (BR-008) and a very large *number* of small-to-medium tenants
(implied by BR-007/BR-009), which are different scaling problems — tenant-count scaling
(control-plane, provisioning, noisy-neighbor isolation) vs. single-tenant data-volume
scaling (partitioning within a tenant). Both are now binding inputs to Chapter 12.

---

## 4. Regulated Vertical Requirements (Resolving Ch. 1 Open Question)

**BR-015:** The platform SHALL support at minimum the following reference regulatory
profiles as first-class, not bolt-on, requirements:

| Vertical | Regulatory Driver | Architectural Implication | Chapter |
|---|---|---|---|
| Financial services | FINRA/SEC continuing-education tracking, immutable audit trail of completion + assessment content version at time of completion | Certification records must be versioned and immutable, not just timestamped | [Ch. 26](../part-4-learning-domain/26-certification.md), [Ch. 41](../part-8-operations/41-compliance.md) |
| Healthcare-adjacent (med-device, pharma, payers) | HIPAA-adjacent PII handling for learner health-related job functions; validated-system requirements (21 CFR Part 11-style e-signature/audit for regulated training) | E-signature on assessment completion; validated deployment/change-control process | [Ch. 41](../part-8-operations/41-compliance.md), [Ch. 39](../part-8-operations/39-devops.md) |
| Government / public sector contractors | FedRAMP-capable hosting posture, Section 508/WCAG 2.1 AA accessibility as a contractual gate | Accessibility elevated to explicit NFR (Red Team finding from Ch. 1, now formally resolved here); hosting must support govcloud-capable regions | [Ch. 7](07-non-functional-requirements.md), [Ch. 41](../part-8-operations/41-compliance.md) |
| EU-headquartered or EU-workforce tenants | GDPR — data residency, right to erasure, data minimization | Multi-region data residency is architectural (Ch. 1 Principle 2), erasure must cascade across all bounded contexts including analytics/BI exports | [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md), [Ch. 41](../part-8-operations/41-compliance.md) |

This directly resolves the Chapter 1 Red Team finding: **accessibility (WCAG 2.1 AA /
Section 508) is now a formally numbered requirement (BR-015, government row) and MUST
appear in [Ch. 7 — Non-Functional Requirements](07-non-functional-requirements.md) as a
first-class NFR**, not a Compliance sub-bullet.

---

## 5. AI Demand Validation (Resolving Ch. 1 Open Question)

Chapter 1's Red Team challenged whether "AI-native" was real customer demand or
architecture-team enthusiasm. Resolution:

**BR-016:** AI capabilities are prioritized in this order, reflecting actual enterprise
L&D buying-committee behavior observed industry-wide (procurement/security asks "what does
AI touch and can we turn it off," not "how much AI do you have"):

1. **High confidence demand:** AI-assisted authoring/content-tagging for L&D admins
   (reduces content-ops labor cost — a CFO-legible ROI case).
2. **Medium confidence demand:** Personalized recommendations/learning-path suggestions
   ([Ch. 30](../part-5-media-discovery/30-recommendation-engine.md)) — desired but rarely a deal-breaker on its own.
3. **Lower initial confidence, monitor:** Conversational AI tutor/assistant
   ([Ch. 31](../part-5-media-discovery/31-ai-integration.md)) — high interest but immature enterprise trust; must be
   optional and auditable (no ungoverned LLM output counted toward compliance completion).

This confirms Chapter 1 Principle 4 (AI additive, not load-bearing) was the correct hedge:
demand is real but *ordered and conditional*, not a blanket mandate — architecture should
not front-load investment in the lowest-confidence tier (conversational tutor) at the
expense of the highest-confidence one (authoring assistance).

---

## 6. Business Success Metrics (KPIs)

These are the metrics the business will use to evaluate the platform post-launch; they
constrain [Ch. 32](../part-6-insight/32-reporting.md) and [Ch. 33](../part-6-insight/33-analytics.md) to ensure the data model
can actually produce them.

| KPI | Definition | Feeds |
|---|---|---|
| Compliance completion rate | % of mandatory-training-assigned learners completing by deadline, per tenant/org unit | [Ch. 32](../part-6-insight/32-reporting.md) |
| Time-to-competency | Days from role assignment to competency-path completion | [Ch. 20](../part-4-learning-domain/20-competency-management.md), [Ch. 33](../part-6-insight/33-analytics.md) |
| Content reuse rate | % of content consumed across >1 tenant (marketplace/federation value) | [Ch. 29](../part-5-media-discovery/29-search.md) |
| Platform gross margin at scale | Infra + ops cost per learner per year vs. license/subscription revenue per learner | [Ch. 45](../part-8-operations/45-cost-optimization.md) |
| Audit defensibility rate | % of compliance audits passed without data-gap findings | [Ch. 41](../part-8-operations/41-compliance.md) |
| System availability | Actual vs. contractual SLA | [Ch. 42](../part-8-operations/42-disaster-recovery.md), [Ch. 38](../part-8-operations/38-observability.md) |

---

## 7. Budget Framing — CAPEX vs OPEX

**BR-017:** The platform is funded and evaluated as a multi-year OPEX-dominant investment
(cloud infrastructure, SaaS integrations, ongoing engineering) with an initial CAPEX-like
build phase (core platform engineering, initial migration tooling). This has two binding
implications carried into every later Technology Evaluation:

- Infrastructure choices are judged primarily on **steady-state OPEX at BR-007/BR-008
  scale**, not build-phase convenience — a technology that is cheap to prototype but
  expensive to run at 3M-learner tenant scale fails the evaluation even if faster to build
  initially.
- Vendor/managed-service costs must be modeled at BR-011 peak concurrency, not average
  load, since enterprise L&D spend is compliance-deadline-driven and bursty by nature
  (Ch. 1 §3) — flat-rate vendor pricing models that don't accommodate bursts are a hidden
  cost risk flagged for [Ch. 45](../part-8-operations/45-cost-optimization.md).

---

## 8. Requirements Traceability Table (Summary)

| ID | Statement | Downstream Owner |
|---|---|---|
| BR-001 | Build enterprise-native core; integrate non-core | [Ch. 9](../part-2-system-domain-architecture/09-product-architecture.md), [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) |
| BR-002–BR-006 | Strategic goals (risk, revenue, mobility, consolidation, agility) | [Ch. 6](06-functional-requirements.md) |
| BR-007–BR-014 | Quantified scale envelope | [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md), [Ch. 43](../part-8-operations/43-scalability.md) |
| BR-015 | Regulated-vertical support incl. accessibility | [Ch. 7](07-non-functional-requirements.md), [Ch. 41](../part-8-operations/41-compliance.md) |
| BR-016 | AI priority ordering | [Ch. 31](../part-5-media-discovery/31-ai-integration.md) |
| BR-017 | OPEX-dominant budget framing | [Ch. 45](../part-8-operations/45-cost-optimization.md) |

---

## Summary

This chapter converted Chapter 1's frame into 17 numbered, traceable business requirements.
It resolved all three of Chapter 1's Open Questions: scale is now dual-dimensioned
(single-tenant ceiling of 3M plus platform-wide aggregate of 50M — BR-007/BR-008), regulated
verticals are named with concrete architectural implications (BR-015, elevating
accessibility to a formal NFR), and AI demand is confirmed real but explicitly
prioritized/ordered rather than a blanket mandate (BR-016). A platform-level Build vs. Buy
decision (BR-001) was made and will not be re-litigated per capability except where a
specific chapter's Technology Evaluation identifies a reason to integrate rather than build.

## Open Questions

- BR-007/BR-008 figures are assumption-based, not contract-sourced. Must be validated
  against real pipeline/deal data once available; flag for revision before
  [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) is treated as final rather than provisional.
- BR-016's AI prioritization assumes enterprise buying-committee behavior "observed
  industry-wide" — this is directional judgment, not a customer survey. [Ch. 3](03-stakeholders.md)
  should attempt to ground this further if any stakeholder interviews are available.
- Consolidation driver (BR-005) implies a migration-from-legacy-LMS capability; the scope
  and tooling for that migration is not yet defined — candidate for its own subsection in
  [Ch. 6](06-functional-requirements.md) or a dedicated future chapter.

## Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| BR-001 (build-not-buy) treated as unchallengeable dogma in later chapters | High — could justify building things that should be bought (Ch. 1 §2.2 boundary erosion) | Medium | Every chapter still required to run its own Build/Buy/Integrate per Technology Evaluation Template; BR-001 sets platform-level intent, not a blanket license to build everything |
| Scale numbers (§3) become anchoring bias — treated as precise even though labeled assumed | High for Ch. 12 sharding cost | Medium | Explicit "assumed contractual envelope" labeling; Ch. 12 must design for headroom beyond BR-008, not exactly to it |
| Regulated-vertical scope (§4) expands ad hoc per deal without formal architecture review | Medium — compliance debt accrues silently | Medium | [Ch. 41](../part-8-operations/41-compliance.md) must define a formal process for adding a new regulatory profile, not just this chapter's static table |

## Architecture Decisions

**ADR-001: Platform is built enterprise-native with integration, not a commercial-suite
customization or OSS fork**
- *Context:* §2.1 Build/Buy/Integrate analysis.
- *Candidate Options:* Buy suite; buy+customize; fork OSS core; build native + integrate.
- *Selected:* Build native + integrate.
- *Rejected:* Buy suite (extensibility ceiling); buy+customize (same ceiling, worse
  contract terms); fork OSS (imports academic-oriented schema debt inconsistent with 7–10
  year horizon).
- *Migration Strategy:* Non-core capabilities (video, identity, authoring, live classroom)
  remain swappable integrations by design (Ch. 1 §2.2), limiting lock-in even though the
  core is custom-built.
- *Review Trigger:* Revisit if a specific customer's TCO analysis in [Ch. 45](../part-8-operations/45-cost-optimization.md)
  shows buy-side crossover is more favorable than modeled here.

**ADR-002: Dual-dimension scale target — 3M single-tenant ceiling, 50M platform aggregate**
- *Context:* §3.
- *Selected:* Design for both dimensions independently in [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md)
  and [Ch. 43](../part-8-operations/43-scalability.md).
- *Rejected:* Designing only for aggregate scale via generic horizontal scaling — rejected
  because it under-designs for single-tenant noisy-neighbor and data-volume concerns.
- *Review Trigger:* Real deal data superseding assumed figures.

## Future Research

- Formal migration-from-legacy-LMS capability scoping (raised in Open Questions).
- Deeper validation of AI prioritization (BR-016) against actual stakeholder interviews once
  available ([Ch. 3](03-stakeholders.md)).

## Cross References
- [Ch. 1 — Enterprise LMS Overview](01-enterprise-lms-overview.md)
- [Ch. 3 — Stakeholders](03-stakeholders.md)
- [Ch. 6 — Functional Requirements](06-functional-requirements.md)
- [Ch. 7 — Non-Functional Requirements](07-non-functional-requirements.md)
- [Ch. 8 — Benchmark Analysis](08-benchmark-analysis.md)
- [Ch. 12 — Database Architecture](../part-2-system-domain-architecture/12-database-architecture.md)
- [Ch. 41 — Compliance](../part-8-operations/41-compliance.md)
- [Ch. 45 — Cost Optimization](../part-8-operations/45-cost-optimization.md)

## Definition of Done
- [x] Platform-level Build/Buy/Integrate decision made and recorded as ADR
- [x] Strategic business goals enumerated with IDs
- [x] Scale envelope quantified (single-tenant and aggregate)
- [x] Regulated-vertical profiles named with architectural implications
- [x] AI demand prioritized, resolving Ch. 1's open question
- [x] Business KPIs enumerated and linked to downstream reporting/analytics chapters
- [x] Budget framing (CAPEX/OPEX) stated as a binding evaluation lens
- [x] Full traceability table produced (BR-001–BR-017)
- [x] Red Team / Blue Team / CTO review completed

## Confidence Level
**Medium.** The qualitative drivers (§2, §6) reflect well-established enterprise L&D
industry patterns — **High** confidence. The quantified scale figures (§3) and regulated-
vertical list (§4) are reasonable, clearly-labeled assumptions standing in for absent
contract data — **Medium** confidence, explicitly provisional pending real pipeline data.

---

## 9. Chapter Review

### 9.1 Red Team Review

- **Hidden assumption:** BR-007/BR-008 numbers, while labeled "assumed," are precise enough
  (e.g., "50 million," "3 million") that later chapters may anchor on them as if
  contract-sourced. The labeling mitigates but does not eliminate this risk.
- **Circular reasoning risk:** ADR-001 (build native) is justified partly by BR-001, which
  was itself stated as "given" in §2.1 rather than independently derived — this chapter
  performs the Build/Buy analysis *after* declaring the conclusion in Ch. 1's premise. This
  is acceptable only because the AKB's mandate is explicitly to build (stated by the
  requester), but it should be named as a constraint-driven decision, not a
  purely-evaluated one, to avoid the appearance of a rigged analysis.
- **Missing requirement:** No mention yet of data portability / export requirements for the
  *customer's* exit strategy (if a tenant leaves the platform, can they extract their
  learning records?). This is different from this AKB's own vendor exit strategy (Ch. 1
  Principle 5) — it's the *tenant's* exit strategy from this platform, and it's currently
  unaddressed.
- **Unvalidated claim:** §5's "observed industry-wide" buying-committee behavior for AI is
  asserted without citation; flagged correctly in Open Questions but worth stating plainly
  here that it is analyst judgment, not empirical data.

### 9.2 Blue Team Review

- The BR-001 circularity is acknowledged and accepted as reasonable: this AKB was
  explicitly commissioned to architect a *build*, so §2.1 functions as documentation of
  *why* that premise is sound (risk/differentiation/TCO reasoning), not a neutral
  make-or-break evaluation — this is now stated explicitly in the Red Team finding response
  rather than hidden.
- Tenant data-portability/exit-strategy gap is a valid, fair miss — accepted and added as a
  new item below rather than argued away.
- Scale-figure anchoring risk is already mitigated as much as is reasonable at this stage
  via explicit "assumed contractual envelope" labeling (§3) and a dedicated Open Question;
  full resolution requires real data this chapter cannot manufacture.

### 9.3 CTO Review

| Item | Verdict | Reasoning |
|---|---|---|
| ADR-001 (build native + integrate) | **Approved with Conditions** | Sound given the AKB's build mandate; condition is that the circularity with Ch. 1's premise is documented (now done in §9.1/9.2) so future readers don't mistake it for a from-scratch neutral evaluation |
| Quantified scale envelope (BR-007–BR-014) | **Approved with Conditions** | Usable as a design target; condition is mandatory revalidation against real deal data before Ch. 12 is treated as final |
| Regulated-vertical requirements (BR-015) | **Approved** | Correctly elevates accessibility to formal NFR status, closing Ch. 1's Red Team gap |
| AI prioritization (BR-016) | **Approved with Conditions** | Directionally sound and appropriately hedged; condition is to seek real stakeholder validation in Ch. 3 rather than resting on analyst judgment alone |
| Tenant data-portability / customer exit strategy | **Requires More Research** | New gap identified by Red Team; must be added to [Ch. 6 — Functional Requirements](06-functional-requirements.md) as an explicit requirement (customer-facing export/API for their own learning records) |

**Action item carried forward:** Chapter 6 (Functional Requirements) must include an
explicit tenant data-export/portability requirement, distinct from this AKB's own
vendor-lock-in exit strategy discipline (Ch. 1 Principle 5).

---

*End of Chapter 2. Proceed to Chapter 3 — Stakeholders.*
