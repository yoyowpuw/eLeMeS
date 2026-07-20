# Chapter 3 — Stakeholders

> Part I — Foundations · [Index](../00-index.md) · Previous: [Ch. 2 — Business Requirements](02-business-requirements.md) · Next: Ch. 4 — User Personas

## 1. Purpose of This Chapter

Chapter 2 produced numbered business requirements but treated "the business" as a single
voice. Enterprise LMS procurement and adoption is never a single voice — it is a committee
with conflicting incentives, and architecture decisions that ignore one seat on that
committee routinely fail at contract signature or at security review, long after the
engineering is done. This chapter enumerates every stakeholder group (buyer-side and
build-side), their goals, what they can veto, and how their concerns map onto specific
later chapters — so no chapter downstream can claim a stakeholder concern was
"undocumented."

This chapter is also where Chapter 2's Open Question about AI demand validation is
addressed as far as an AKB (without live customer interviews) responsibly can.

---

## 2. Stakeholder Taxonomy

Two populations, kept explicitly separate because they are routinely conflated in LMS
literature and that conflation causes real design mistakes:

- **Buyer/Tenant-side stakeholders** — people inside the *customer* organization who
  select, configure, administer, and use the platform.
- **Build/Vendor-side stakeholders** — people inside the organization *building and
  operating* the platform (engineering, product, GTM, support).

A recurring failure mode named by the Red Team in Chapter 1 (rebuilding adjacent
categories) traces directly back to over-weighting one buyer-side stakeholder (e.g.,
Content Author wanting a full authoring suite) without checking it against the Security
Architect or Procurement stakeholder who will reject that scope. This chapter exists to
prevent that.

---

## 3. Buyer/Tenant-Side Stakeholders

| Stakeholder | Primary Goal | Can Veto / Block | Key Concerns Mapped to Chapters |
|---|---|---|---|
| **Chief Learning Officer / VP L&D** (Economic Buyer) | Demonstrable ROI: compliance risk reduction (BR-002), time-to-competency (BR-003) | Budget approval, renewal | [Ch. 32](../part-6-insight/32-reporting.md), [Ch. 33](../part-6-insight/33-analytics.md), [Ch. 45](../part-8-operations/45-cost-optimization.md) |
| **L&D Administrator** | Day-to-day configurability: org structure, content assignment rules, compliance workflows | Operational adoption — if unusable, shadow-IT workarounds emerge | [Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md), [Ch. 22](../part-4-learning-domain/22-course-management.md), [Ch. 34](../part-6-insight/34-notification-system.md) |
| **CISO / Security Architect (buyer-side)** | Attestable security posture: SSO/SCIM, data residency, pen-test results, SOC 2/ISO 27001 | Hard veto — security review failure blocks procurement outright, independent of feature quality | [Ch. 16](../part-3-identity-organization/16-authentication.md), [Ch. 17](../part-3-identity-organization/17-authorization.md), [Ch. 40](../part-8-operations/40-security.md) |
| **Legal / Compliance Officer (buyer-side)** | Defensible audit trail, data processing agreements, regulatory-specific certification (BR-015) | Hard veto in regulated verticals | [Ch. 41](../part-8-operations/41-compliance.md), [Ch. 26](../part-4-learning-domain/26-certification.md) |
| **IT / Enterprise Architecture (buyer-side)** | Integration fit with existing HRIS/IdP/data warehouse stack; won't accept a platform that can't integrate | Technical veto pre-contract | [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) |
| **Procurement** | Total contract cost, vendor viability, exit-clause terms | Commercial veto | [Ch. 46](../part-9-governance-future/46-licensing.md), tenant data-portability item flagged in [Ch. 2](02-business-requirements.md) §9 |
| **Data Privacy Officer / DPO (EU tenants)** | GDPR compliance: residency, erasure, minimization | Hard veto for EU-headquartered tenants | [Ch. 41](../part-8-operations/41-compliance.md) |
| **People Manager** | Visibility into direct reports' compliance/competency status without administrative overhead | Adoption driver, not a hard veto | [Ch. 32](../part-6-insight/32-reporting.md), [Ch. 6](06-functional-requirements.md) |
| **Learner** (end user) | Low-friction access, mobile/offline capability, relevant content | Adoption/engagement — poor UX drives non-completion, which undermines BR-002 | [Ch. 4](04-user-personas.md), [Ch. 36](../part-7-platform-integration/36-mobile-strategy.md), [Ch. 37](../part-7-platform-integration/37-offline-learning.md) |
| **Content Author / Instructional Designer** | Efficient authoring/import workflow, analytics on content effectiveness | Content-supply risk if tooling is too weak — but bounded by Ch. 1 §2.2 (authoring tools are integrated, not rebuilt) | [Ch. 22](../part-4-learning-domain/22-course-management.md), [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) |
| **Instructor / Facilitator (ILT)** | Scheduling, roster, attendance, grading tools for live sessions | Adoption driver | [Ch. 22](../part-4-learning-domain/22-course-management.md), [Ch. 25](../part-4-learning-domain/25-assignment-engine.md) |
| **Accessibility Office (public-sector/regulated tenants)** | WCAG 2.1 AA / Section 508 conformance | Hard veto for government contracts (per BR-015) | [Ch. 7](07-non-functional-requirements.md) |

**Key tension flagged for later chapters:** the CLO wants engagement/discovery features
(LXP-style, Ch. 1 §2.2), while the CISO and DPO want the *minimum necessary data
collection* consistent with privacy-by-design (Ch. 1 Principle 7). This tension is not
resolved here — it is explicitly assigned to [Ch. 30](../part-5-media-discovery/30-recommendation-engine.md) and
[Ch. 41](../part-8-operations/41-compliance.md) to resolve via data-minimization-compatible recommendation
design (e.g., avoid unnecessary behavioral tracking beyond what completion/compliance and
opted-in personalization require).

---

## 4. Build/Vendor-Side Stakeholders

| Stakeholder | Primary Goal | Can Veto / Block | Key Concerns Mapped to Chapters |
|---|---|---|---|
| **CTO** | Long-term technical sustainability, TCO discipline | Final architecture sign-off (per this AKB's review protocol) | All chapters — final review authority |
| **Product Leadership** | Market differentiation, roadmap sequencing | Feature prioritization | [Ch. 50](../part-9-governance-future/50-future-roadmap.md) |
| **Principal/Enterprise Architects** | Cross-domain consistency, avoiding architectural drift across 50 domains | Architecture review gate | [Ch. 9](../part-2-system-domain-architecture/09-product-architecture.md)–[Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) |
| **Security/DevSecOps Leadership (vendor-side)** | Platform-wide security posture defensible across *all* tenants simultaneously (not just one buyer's review) | Release gate | [Ch. 39](../part-8-operations/39-devops.md), [Ch. 40](../part-8-operations/40-security.md) |
| **SRE / Platform Ops** | Operability at BR-011 peak concurrency without heroics | Operational readiness gate | [Ch. 38](../part-8-operations/38-observability.md), [Ch. 43](../part-8-operations/43-scalability.md) |
| **FinOps** | Cloud spend predictability at BR-007/BR-008 scale | Budget gate | [Ch. 45](../part-8-operations/45-cost-optimization.md) |
| **Sales Engineering / GTM** | Ability to credibly answer RFP security/compliance questionnaires | Deal-closing input, not architecture veto, but a strong signal source | [Ch. 8](08-benchmark-analysis.md), [Ch. 41](../part-8-operations/41-compliance.md) |
| **Customer Success / Support** | Debuggability, tenant-level observability, safe self-service admin tools | Post-sale retention risk if ignored | [Ch. 38](../part-8-operations/38-observability.md), [Ch. 48](../part-9-governance-future/48-operations.md) |
| **Legal (vendor-side)** | Vendor's own liability exposure, DPA templates, sub-processor management | Contract-term veto | [Ch. 46](../part-9-governance-future/46-licensing.md), [Ch. 41](../part-8-operations/41-compliance.md) |

---

## 5. Stakeholder Influence/Interest Matrix

```mermaid
quadrantChart
    title Stakeholder Influence vs Interest
    x-axis Low Interest --> High Interest
    y-axis Low Influence --> High Influence
    quadrant-1 Manage Closely
    quadrant-2 Keep Satisfied
    quadrant-3 Monitor
    quadrant-4 Keep Informed
    CISO (buyer): [0.7, 0.95]
    DPO (buyer): [0.6, 0.9]
    CLO / Economic Buyer: [0.85, 0.85]
    Procurement: [0.5, 0.8]
    L&D Administrator: [0.9, 0.55]
    Learner: [0.5, 0.2]
    People Manager: [0.5, 0.3]
    Content Author: [0.6, 0.3]
    CTO (vendor): [0.9, 0.9]
    SRE / Platform Ops: [0.6, 0.6]
    FinOps: [0.4, 0.6]
    Sales Engineering: [0.55, 0.4]
```

The two highest influence+interest stakeholders — **CISO (buyer)** and **CLO (buyer)** —
frequently want opposite things at the margin (security minimalism vs. feature richness).
This chapter formally names that as the central tension the architecture must hold, echoed
throughout [Ch. 16](../part-3-identity-organization/16-authentication.md)–[Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md) and
[Ch. 40](../part-8-operations/40-security.md)–[Ch. 41](../part-8-operations/41-compliance.md).

---

## 6. RACI for Key Architecture Decision Categories

| Decision Category | Responsible | Accountable | Consulted | Informed |
|---|---|---|---|---|
| Data residency / multi-region topology | Enterprise Architect, Cloud Architect | CTO | DPO (buyer), CISO (buyer), Legal (vendor) | Procurement, Sales Eng |
| Authentication/SSO model | IAM Architect | CTO | CISO (buyer), IT (buyer) | L&D Admin |
| Compliance/audit data model | Compliance Consultant, Database Architect | CTO | Legal (buyer), Legal (vendor) | CLO |
| Video streaming vendor selection | Video Streaming Architect | Enterprise Architect | FinOps, SRE | Content Author |
| AI feature scope | AI Architect | Product Leadership | CISO (buyer), DPO (buyer) — per BR-016 governance | CLO, Learner |
| Pricing/licensing model | Product Leadership | CTO/CFO (outside this AKB's authority) | Procurement (buyer), FinOps | Sales Eng |

---

## 7. AI Demand Validation — Follow-up from Chapter 2

Chapter 2 flagged that BR-016's AI prioritization rested on analyst judgment, not
stakeholder interviews. This chapter cannot fabricate interviews that were not conducted,
but it can responsibly triangulate using the stakeholder map above:

- The **CISO and DPO** (both high-influence, high-interest per §5) will scrutinize any AI
  feature that processes learner behavioral data — this independently corroborates BR-016's
  ordering (authoring-assistance, which touches admin-authored content, carries materially
  lower privacy scrutiny than a conversational tutor processing learner interaction data).
- The **CLO**, as economic buyer, evaluates AI features against BR-002/BR-003 ROI framing —
  AI-assisted authoring has a direct, CFO-legible labor-cost story; conversational tutoring
  does not yet, reinforcing BR-016's tier ordering.
- **Formal resolution:** BR-016's ordering is retained as-is, now corroborated by
  stakeholder-incentive analysis rather than resting solely on unstated industry
  observation. The Open Question from Ch. 2 is downgraded from "unresolved" to "resolved
  by stakeholder-incentive triangulation, pending real interview validation" — full closure
  still requires actual customer discovery, which is out of this AKB's authority to perform
  (see Definition of Done).

---

## Summary

This chapter enumerated 12 buyer-side and 9 vendor-side stakeholder roles, mapped each to
the chapters that must satisfy their concerns, and identified the CISO-vs-CLO
security-minimalism-vs-feature-richness tension as the architecture's central recurring
conflict — to be actively managed in the identity, compliance, and AI chapters rather than
resolved once. It also triangulated (without fabricating) additional support for Chapter
2's AI demand-prioritization decision using stakeholder incentive analysis.

## Open Questions

- No real stakeholder interviews exist behind this chapter — all goals/concerns are
  informed-analyst synthesis of well-documented enterprise-buying-committee patterns. Full
  validation requires actual discovery calls with a real (or design-partner) customer,
  outside this AKB's authority.
- RACI in §6 assumes a vendor org structure (Enterprise Architect, IAM Architect, etc.)
  matching this AKB's own expert roster (see operating mandate) — a real organization may
  not have all these roles as distinct people. Should be treated as decision *accountability
  types* to assign to real staff, not a literal headcount requirement.
- The CISO-vs-CLO tension (§5, §7) is named but not resolved here by design — track whether
  later chapters ([Ch. 16](../part-3-identity-organization/16-authentication.md), [Ch. 30](../part-5-media-discovery/30-recommendation-engine.md),
  [Ch. 41](../part-8-operations/41-compliance.md)) actually revisit and resolve it, or silently drop it.

## Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Stakeholder map treated as complete/final when it is analyst-synthesized | Medium — real stakeholder may surface unlisted concerns late (e.g., during actual procurement) | Medium | Re-validate this chapter against real design-partner feedback as soon as available; treat as a living document |
| CISO-vs-CLO tension named but not actively tracked through later chapters | High — the whole point of naming it is lost if later chapters don't reference it | Medium | Cross-reference this chapter explicitly in Ch. 16, Ch. 30, Ch. 41 Open Questions/Risks sections when written |
| RACI treated as literal org chart requirement by an implementing team | Low-Medium — could cause unnecessary hiring pressure | Low | Explicit note in Open Questions clarifying RACI = accountability types, not headcount |

## Architecture Decisions

No technology ADRs in this chapter (stakeholder analysis, not implementation). One process
decision recorded:

**ADR-003: CISO/DPO privacy concerns and CLO feature-richness goals are held as a permanent
design tension, not resolved once**
- *Context:* §5, §7 — the platform's two highest-influence buyer stakeholders want
  structurally opposing things at the margin.
- *Selected:* Every chapter touching learner behavioral data, identity, or AI must
  explicitly address both stakeholders' concerns in its own Risks/Open Questions, rather
  than this chapter attempting a one-time resolution.
- *Rejected:* Picking a single default posture now (e.g., "always privacy-maximalist") —
  rejected because it would bias every later chapter without domain-specific context (e.g.,
  a recommendation engine's data needs differ from an authentication system's).
- *Review Trigger:* If [Ch. 16](../part-3-identity-organization/16-authentication.md), [Ch. 30](../part-5-media-discovery/30-recommendation-engine.md),
  or [Ch. 41](../part-8-operations/41-compliance.md) fail to reference this tension, it should be treated as a
  process gap and flagged in that chapter's Red Team review.

## Future Research

- Real stakeholder/design-partner interviews to validate or correct §3–§4.
- Whether a formal stakeholder-sign-off gate (e.g., CISO sign-off required before GA) should
  be codified as part of [Ch. 39 — DevOps](../part-8-operations/39-devops.md) release process.

## Cross References
- [Ch. 2 — Business Requirements](02-business-requirements.md)
- [Ch. 4 — User Personas](04-user-personas.md)
- [Ch. 16 — Authentication](../part-3-identity-organization/16-authentication.md)
- [Ch. 30 — Recommendation Engine](../part-5-media-discovery/30-recommendation-engine.md)
- [Ch. 40 — Security](../part-8-operations/40-security.md)
- [Ch. 41 — Compliance](../part-8-operations/41-compliance.md)
- [Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md)

## Definition of Done
- [x] Buyer-side stakeholders enumerated with goals, veto power, chapter mapping
- [x] Vendor-side stakeholders enumerated with goals, veto power, chapter mapping
- [x] Influence/interest matrix produced
- [x] RACI for key decision categories produced
- [x] Central recurring stakeholder tension (CISO vs CLO) explicitly named and assigned an ADR
- [x] Chapter 2's AI-demand Open Question triangulated as far as responsibly possible without fabricated data
- [x] Red Team / Blue Team / CTO review completed
- [ ] Real stakeholder interview validation — explicitly out of scope for this AKB; flagged, not faked

## Confidence Level
**Medium.** Stakeholder *categories* and their structural incentives (§3–§4) reflect
well-established enterprise software procurement patterns — **High** confidence. Specific
weighting (influence/interest scores in §5, AI-triangulation in §7) is reasoned synthesis,
not measured data — **Medium** confidence, explicitly flagged as requiring real validation.

---

## 8. Chapter Review

### 8.1 Red Team Review

- **Fabrication risk:** §7's "AI demand validation" could be read as if real evidence
  supports BR-016, when it is actually incentive-based reasoning about *likely* stakeholder
  behavior, not observed behavior. The chapter must not overstate this as closure.
- **Missing stakeholder:** No **Union / Works Council representative** is listed, which is
  a hard blocker in EU-headquartered tenants (Germany, France) for any system that
  tracks individual employee performance/behavioral data — works councils can legally block
  deployment of monitoring-adjacent systems. This is a real gap.
- **Missing stakeholder:** No **Procurement-adjacent Vendor Risk Management (VRM) /
  Third-Party Risk function** distinct from generic "Procurement" — in large enterprises
  this is often a separate, slow-moving gate (SIG questionnaires, vendor security
  assessments) that materially affects sales cycle and therefore architecture priorities
  (e.g., need for a public trust/security documentation page).

### 8.2 Blue Team Review

- The fabrication-risk concern is valid; §7's own text already hedges this ("cannot
  fabricate," "corroborated," "pending real interview validation," DoD unchecked box) — the
  chapter is self-aware about this limitation rather than overclaiming. No change needed
  beyond what's already written, but the Red Team's phrasing is adopted verbatim into Open
  Questions for emphasis.
- Works Council gap is accepted as valid and material — added below as a new stakeholder
  row rather than argued away, since GDPR-adjacent EU employment law makes this a real veto
  point, not a theoretical one.
- VRM/Third-Party Risk gap is accepted; noted for [Ch. 41](../part-8-operations/41-compliance.md) to ensure a
  public-facing trust/security documentation deliverable is scoped there.

**Corrective addendum (accepted from Red Team):**

| Stakeholder | Primary Goal | Can Veto / Block | Key Concerns Mapped to Chapters |
|---|---|---|---|
| **Works Council / Employee Representative (EU tenants)** | Ensure system does not enable unlawful individual performance surveillance | Legal veto on deployment in Germany/France/Netherlands etc. under co-determination law | [Ch. 33](../part-6-insight/33-analytics.md) (must distinguish aggregate/cohort reporting from individual surveillance), [Ch. 41](../part-8-operations/41-compliance.md) |
| **Vendor Risk Management / Third-Party Risk (buyer-side)** | Complete, standardized security questionnaire responses (SIG, CAIQ) without lengthy custom review cycles | Deal-velocity gate, escalates to CISO veto if unsatisfied | [Ch. 40](../part-8-operations/40-security.md), [Ch. 41](../part-8-operations/41-compliance.md) — should scope a public trust-center deliverable |

### 8.3 CTO Review

| Item | Verdict | Reasoning |
|---|---|---|
| Buyer/vendor stakeholder taxonomy (§2–§4) | **Approved with Conditions** | Sound structure; condition is incorporating the Works Council and VRM rows identified in Red Team review (now added in §8.2) |
| Influence/interest matrix (§5) | **Approved** | Correctly surfaces CISO-vs-CLO as the central tension |
| RACI (§6) | **Approved** | Useful as accountability-type framework; Open Questions correctly caveat it's not a literal org chart |
| CISO-vs-CLO tension as standing ADR rather than one-time resolution (ADR-003) | **Approved** | Correct call — a premature single resolution would bias later, more-informed chapters |
| §7 AI demand triangulation | **Approved with Conditions** | Acceptable as directional reasoning; condition is it must never be cited later as "stakeholder-validated" without the caveats intact |
| Works Council gap | **Requires More Research** | Must be carried into [Ch. 33](../part-6-insight/33-analytics.md) and [Ch. 41](../part-8-operations/41-compliance.md) as a binding requirement (aggregate vs. individual analytics distinction), not just noted here |

**Action items carried forward:**
1. [Ch. 33 — Analytics](../part-6-insight/33-analytics.md) must explicitly design for aggregate/cohort
   reporting as separable from individual-level surveillance, to satisfy Works Council
   concerns in EU deployments.
2. [Ch. 41 — Compliance](../part-8-operations/41-compliance.md) must scope a public-facing trust/security
   documentation deliverable to accelerate VRM/Third-Party Risk review cycles.

---

*End of Chapter 3. Proceed to Chapter 4 — User Personas.*
