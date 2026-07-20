# Chapter 4 — User Personas

> Part I — Foundations · [Index](../00-index.md) · Previous: [Ch. 3 — Stakeholders](03-stakeholders.md) · Next: Ch. 5 — Learning Lifecycle

## 1. Purpose of This Chapter

Chapter 3 identified *stakeholder roles* — who has influence and veto power over the
program. This chapter narrows to the subset who actually **operate the product day-to-day**
and converts them into personas concrete enough to drive UX, API, offline, and mobile
architecture decisions in Part II onward. A stakeholder role (e.g., "CISO") is a
procurement-and-governance concern; a persona (e.g., "Frontline Learner") is a
product-usage concern with a device profile, frequency, proficiency level, and job-to-be-
done. Several stakeholders from Chapter 3 collapse into or spawn multiple personas here
(e.g., "Learner" stakeholder → three distinct learner personas below, because a
frontline retail worker and a remote knowledge worker have materially different
architecture-relevant needs).

---

## 2. Persona Framework

Each persona is defined against the same dimensions, chosen because they are the
dimensions that actually change architecture decisions (not demographic flavor text):

- **Job-to-be-done** — the core task, stated as an outcome.
- **Frequency & context of use** — drives caching, offline, and performance priorities.
- **Device profile** — drives [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md), [Ch. 36](../part-7-platform-integration/36-mobile-strategy.md), [Ch. 37](../part-7-platform-integration/37-offline-learning.md).
- **Connectivity assumption** — drives offline-first vs. online-only design decisions.
- **Technical proficiency** — drives UX complexity budget, not engineering complexity.
- **Volume** (how many exist, per BR-007–BR-009 scale) — drives whether a workflow needs to
  be highly optimized (high-volume persona) or can tolerate a heavier admin UI (low-volume
  persona).
- **Primary pain point today** (with legacy/incumbent tools, per [Ch. 8](08-benchmark-analysis.md) to be detailed) — drives differentiation priorities.
- **Chapters this persona most constrains.**

---

## 3. Learner Personas

Stakeholder Chapter 3's single "Learner" row is split into three personas because their
architecture-relevant needs genuinely diverge — collapsing them would bias
[Ch. 36](../part-7-platform-integration/36-mobile-strategy.md)/[Ch. 37](../part-7-platform-integration/37-offline-learning.md) toward only one profile.

### 3.1 Persona — "Frontline Fiona" (Deskless/Frontline Worker)

| Dimension | Detail |
|---|---|
| Job-to-be-done | Complete short, mandatory compliance/safety training between shifts, on a shared or personal mobile device |
| Frequency & context | Bursty — spikes around shift start/end, new-hire onboarding, compliance deadlines |
| Device profile | Personal smartphone (BYOD) or shared kiosk/tablet; low-to-mid-tier Android common in some sectors (retail, logistics, manufacturing) |
| Connectivity assumption | **Unreliable** — warehouse floors, retail backrooms, moving vehicles; offline-capable is not a nice-to-have for this persona |
| Technical proficiency | Variable, often low tolerance for friction; app must work in under 3 taps to resume a course |
| Volume | Largest single population by headcount at BR-008 scale (e.g., retail/logistics tenants are majority-frontline) |
| Primary pain point today | Legacy LMS mobile experiences are frequently a "shrunk desktop site," not mobile-native; offline mode is often absent entirely |
| Constrains | [Ch. 36](../part-7-platform-integration/36-mobile-strategy.md), [Ch. 37](../part-7-platform-integration/37-offline-learning.md), [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md), [Ch. 27](../part-5-media-discovery/27-video-streaming.md) (adaptive bitrate for poor connectivity) |

### 3.2 Persona — "Knowledge-Worker Ken" (Desk-based Office Worker)

| Dimension | Detail |
|---|---|
| Job-to-be-done | Complete role-based learning paths, professional development, and compliance training during work hours at a desk |
| Frequency & context | Scheduled/planned — often blocks calendar time; sometimes squeezed between meetings |
| Device profile | Laptop/desktop primary, mobile secondary for notifications and short content |
| Connectivity assumption | Reliable broadband/corporate network |
| Technical proficiency | Moderate-high; comfortable with SaaS UX patterns |
| Volume | Second-largest population; dominant in tech, finance, professional services tenants |
| Primary pain point today | Content discovery friction — catalogs of 100k+ items (BR-013) with poor search/recommendation |
| Constrains | [Ch. 29](../part-5-media-discovery/29-search.md), [Ch. 30](../part-5-media-discovery/30-recommendation-engine.md), [Ch. 21](../part-4-learning-domain/21-learning-paths.md) |

### 3.3 Persona — "External Extension Ellie" (Partner/Franchisee/Customer Learner)

| Dimension | Detail |
|---|---|
| Job-to-be-done | Complete certification/product training as a non-employee (partner, franchisee, reseller, or paying customer) |
| Frequency & context | Infrequent, often self-paced, sometimes paywalled/gated by contract terms |
| Device profile | Mixed — often outside corporate device management entirely (no MDM, no corporate SSO by default) |
| Connectivity assumption | Unmanaged/unknown — cannot assume corporate network reliability or security posture |
| Technical proficiency | Variable; lowest tolerance for requiring IT support to log in |
| Volume | Smaller per-tenant but can be large in aggregate for franchise/reseller-heavy tenants |
| Primary pain point today | Identity friction — external users are often forced through awkward guest-account flows; this is a direct driver for [Ch. 16](../part-3-identity-organization/16-authentication.md)'s external-identity design (B2B2C-style federation, not just enterprise SSO) |
| Constrains | [Ch. 16](../part-3-identity-organization/16-authentication.md), [Ch. 18](../part-3-identity-organization/18-multi-tenancy.md) (external-user tenancy model), [Ch. 26](../part-4-learning-domain/26-certification.md) (external certification issuance) |

---

## 4. Non-Learner Operational Personas

### 4.1 Persona — "Manager Maya" (People Manager)

| Dimension | Detail |
|---|---|
| Job-to-be-done | See team compliance/competency status at a glance; approve learning requests; avoid becoming an L&D admin herself |
| Frequency & context | Periodic check-ins, spikes before compliance deadlines and performance review cycles |
| Device profile | Desktop primary, mobile for quick approvals |
| Connectivity assumption | Reliable |
| Technical proficiency | Moderate; low patience for admin-grade complexity — this persona must NOT be handed the L&D Administrator's UI |
| Volume | Roughly 1 per 6–8 learners at typical enterprise span-of-control — a high-volume persona at BR-008 scale |
| Primary pain point today | Legacy tools conflate manager and admin views, overwhelming managers with configuration they don't need |
| Constrains | [Ch. 32](../part-6-insight/32-reporting.md) (manager-scoped views, distinct from admin views), [Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md) (manager-of-record must be sourced from HRIS, not hand-maintained) |

### 4.2 Persona — "Admin Aisha" (L&D Administrator / Tenant Admin)

| Dimension | Detail |
|---|---|
| Job-to-be-done | Configure org structure, assign mandatory training, manage content catalog, run compliance reports, respond to audits |
| Frequency & context | Daily, power-user |
| Device profile | Desktop, multi-monitor common |
| Connectivity assumption | Reliable |
| Technical proficiency | High within domain (L&D operations), not necessarily technical/IT-literate — admin UI must not assume API/technical fluency |
| Volume | Small in absolute count (dozens per large tenant) but extremely high-leverage — errors here affect thousands of learners |
| Primary pain point today | Bulk operations (mass-enroll, mass-reassign on reorg) are often slow, unsafe, or unavailable in legacy tools, forcing risky manual work at scale |
| Constrains | [Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md), [Ch. 22](../part-4-learning-domain/22-course-management.md), [Ch. 41](../part-8-operations/41-compliance.md) (audit report generation), bulk-operation safety/idempotency requirements for [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) |

### 4.3 Persona — "Author Amir" (Content Author / Instructional Designer)

| Dimension | Detail |
|---|---|
| Job-to-be-done | Import/publish content (often authored externally per Ch. 1 §2.2), organize into paths, view content-level engagement/effectiveness data |
| Frequency & context | Project-based, bursty around content releases |
| Device profile | Desktop |
| Connectivity assumption | Reliable |
| Technical proficiency | Moderate-high in content tooling, not necessarily technical |
| Volume | Small population, high content-supply leverage |
| Primary pain point today | SCORM/xAPI import failures are frequently silent or cryptically reported in legacy tools; content-effectiveness analytics are often absent |
| Constrains | [Ch. 22](../part-4-learning-domain/22-course-management.md), [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) (import fidelity/validation UX), [Ch. 33](../part-6-insight/33-analytics.md) (content-level analytics) |

### 4.4 Persona — "Auditor Alex" (Internal/External Compliance Auditor)

| Dimension | Detail |
|---|---|
| Job-to-be-done | Retrieve immutable, defensible evidence of specific individuals' or cohorts' training completion, often for a specific point-in-time content version |
| Frequency & context | Infrequent but high-stakes; often time-boxed (audit response deadlines) |
| Device profile | Desktop, may require exportable formats (PDF/CSV) rather than in-app viewing only |
| Connectivity assumption | Reliable, sometimes read-only/limited-access accounts |
| Technical proficiency | Low product familiarity — this persona may use the system once a year; UX must not assume learned muscle memory |
| Volume | Very low count, extremely high consequence of failure (a bad audit response has legal/regulatory impact) |
| Primary pain point today | Named directly in [Ch. 2](02-business-requirements.md) BR-002/regulated-vertical requirements — this persona is the human face of "audit defensibility rate" (Ch. 2 §6 KPI) |
| Constrains | [Ch. 26](../part-4-learning-domain/26-certification.md) (immutable, versioned records), [Ch. 41](../part-8-operations/41-compliance.md), [Ch. 32](../part-6-insight/32-reporting.md) (export formats) |

### 4.5 Persona — "Integrator Ivan" (Buyer-side IT/Integration Engineer)

| Dimension | Detail |
|---|---|
| Job-to-be-done | Wire up SSO/SCIM, HRIS sync, data warehouse export, without ongoing vendor support tickets |
| Frequency & context | Heavy during onboarding/implementation, light thereafter (monitoring/maintenance) |
| Device profile | Desktop, API/CLI-comfortable |
| Connectivity assumption | Reliable, corporate network, may require IP-allowlisting/VPN support |
| Technical proficiency | High — this is the one persona architecture can assume real technical fluency for |
| Volume | Very low count per tenant, but a blocked Ivan blocks the entire tenant's go-live |
| Primary pain point today | Poor/incomplete API documentation and lack of sandbox environments are the most commonly cited integration friction in enterprise SaaS generally |
| Constrains | [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) (API design and documentation quality as a first-class deliverable), [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) |

---

## 5. Persona Volume & Priority Matrix

Cross-referencing persona volume (§3–4) against Chapter 3's influence/interest matrix to
set UX investment priority — high volume + currently high pain point = highest ROI for
early design investment:

| Persona | Relative Volume (per BR-008 tenant) | Current Pain Severity | Design Priority |
|---|---|---|---|
| Frontline Fiona | Very High | High (offline/mobile gap) | **P0** |
| Knowledge-Worker Ken | High | Medium (discovery friction) | P1 |
| Manager Maya | High | Medium (UI conflation with admin) | P1 |
| Admin Aisha | Low | High (bulk-op safety) | **P0** (leverage, not volume) |
| Author Amir | Low | Medium | P2 |
| External Extension Ellie | Variable, tenant-dependent | High (identity friction) | P1 |
| Auditor Alex | Very Low | High (consequence, not frequency) | **P0** (consequence-weighted) |
| Integrator Ivan | Very Low | High (blocks go-live) | **P0** (blocking dependency) |

**Design implication carried forward:** four personas (Fiona, Aisha, Alex, Ivan) are P0 for
reasons other than raw volume — Fiona by sheer scale, and Aisha/Alex/Ivan by leverage,
consequence, or blocking-dependency respectively. This directly informs which chapters get
scrutinized hardest in later Red Team reviews: [Ch. 37](../part-7-platform-integration/37-offline-learning.md) (Fiona),
bulk-operation design in [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md)/[Ch. 19](../part-3-identity-organization/19-organization-hierarchy.md)
(Aisha), immutable records in [Ch. 26](../part-4-learning-domain/26-certification.md) (Alex), and API/docs quality in
[Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) (Ivan).

---

## Summary

Chapter 3's stakeholder roles are refined here into 8 concrete personas — 3 learner
variants (Frontline Fiona, Knowledge-Worker Ken, External Extension Ellie) and 5 operational
personas (Manager Maya, Admin Aisha, Author Amir, Auditor Alex, Integrator Ivan) — each
defined by job-to-be-done, device/connectivity profile, technical proficiency, volume, and
current pain point. A volume/priority matrix identifies four P0 personas (Fiona by scale;
Aisha, Alex, and Ivan by leverage/consequence/blocking-dependency) that should receive the
highest design scrutiny in later chapters, particularly offline/mobile, bulk operations,
immutable audit records, and API/documentation quality.

## Open Questions

- Persona volume figures (§5) are relative/qualitative, not derived from real usage
  telemetry (none exists pre-launch). Should be revisited with real analytics once
  [Ch. 33 — Analytics](../part-6-insight/33-analytics.md) is operational post-launch.
- Is there a distinct "Executive Sponsor" persona (e.g., a business-unit VP who consumes
  only high-level compliance dashboards and never touches operational features) that
  deserves separation from Manager Maya? Tentatively folded into Manager Maya's reporting
  needs for now; flag for [Ch. 32 — Reporting](../part-6-insight/32-reporting.md) to confirm whether a
  separate executive-dashboard persona is warranted.
- External Extension Ellie's identity model (B2B2C-style) has significant overlap with, but
  is not identical to, standard enterprise SSO federation — needs explicit reconciliation in
  [Ch. 16 — Authentication](../part-3-identity-organization/16-authentication.md), not just a mention here.

## Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Admin Aisha and Manager Maya personas get UI-conflated during actual design (common LMS anti-pattern per §4.1) | High — overwhelms managers, undermines adoption (BR-003) | Medium | Explicit persona separation recorded here as a reusable citation for [Ch. 32](../part-6-insight/32-reporting.md) and future UX work |
| Frontline Fiona's offline/connectivity needs treated as a "mobile chapter" afterthought rather than a cross-cutting constraint | High — P0 persona by volume; failure here undermines BR-002 compliance completion for the largest population | Medium | Cross-referenced explicitly into [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md), [Ch. 27](../part-5-media-discovery/27-video-streaming.md), not just Ch. 36/37 |
| Auditor Alex's low frequency of use leads engineering to under-invest in this UX, despite P0 consequence rating | High if an actual audit occurs | Low-Medium | Explicit consequence-weighted P0 designation here, to be cited against any future deprioritization attempt |

## Architecture Decisions

**ADR-004: Manager and Admin personas receive structurally separate UI surfaces, not a
permissions-gated single UI**
- *Context:* §4.1, §4.2 — recurring legacy anti-pattern of conflating these roles.
- *Selected:* Manager-facing views (team compliance/competency) are architecturally distinct
  from Admin-facing configuration surfaces, even where they share underlying data — to be
  detailed as separate frontend surfaces/routes in [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md).
- *Rejected:* Single admin UI with role-based feature-flagging down to a "manager mode" —
  rejected because this is precisely the pattern identified as causing manager overwhelm in
  incumbent tools (§4.1 pain point).
- *Review Trigger:* Revisit if [Ch. 14](../part-2-system-domain-architecture/14-frontend-architecture.md) finds the separation
  creates unacceptable duplication cost; must be weighed against the adoption-risk evidence
  here before reversing.

## Future Research

- Real usage telemetry to validate persona volume assumptions post-launch.
- Executive Sponsor persona separation question (Open Questions).
- External learner (Ellie) identity model reconciliation with enterprise SSO.

## Cross References
- [Ch. 3 — Stakeholders](03-stakeholders.md)
- [Ch. 5 — Learning Lifecycle](05-learning-lifecycle.md)
- [Ch. 13 — API Strategy](../part-2-system-domain-architecture/13-api-strategy.md)
- [Ch. 16 — Authentication](../part-3-identity-organization/16-authentication.md)
- [Ch. 32 — Reporting](../part-6-insight/32-reporting.md)
- [Ch. 36 — Mobile Strategy](../part-7-platform-integration/36-mobile-strategy.md)
- [Ch. 37 — Offline Learning](../part-7-platform-integration/37-offline-learning.md)

## Definition of Done
- [x] Learner stakeholder split into 3 architecture-relevant personas with justification
- [x] 5 operational personas defined with job-to-be-done, device/connectivity, proficiency, volume, pain point
- [x] Volume/priority matrix produced, identifying P0 personas with explicit rationale
- [x] Each persona explicitly linked to the chapters it constrains
- [x] Red Team / Blue Team / CTO review completed

## Confidence Level
**Medium-High.** Persona archetypes and their device/connectivity/proficiency profiles
(§3–4) reflect well-documented enterprise/frontline-workforce UX research patterns —
**High** confidence. Relative volume and priority ranking (§5) is directionally sound but
qualitative, pending real telemetry — **Medium** confidence.

---

## 6. Chapter Review

### 6.1 Red Team Review

- **Missing persona:** No **Security/Compliance-reviewer-as-a-user persona for the vendor
  side** (e.g., a vendor Customer Success Engineer troubleshooting a specific tenant's
  issue) — this "impersonation/support access" use case has major security implications
  (must be audited, time-boxed, tenant-consented) and is currently only implied, not
  defined. Should be added or explicitly deferred to [Ch. 48 — Operations](../part-9-governance-future/48-operations.md).
- **Unresolved tension carried without action:** §4.1/§4.2 note Manager vs Admin conflation
  as a risk and produce ADR-004, but Auditor Alex's "low frequency, high consequence" UX
  problem (people who use the system once a year) doesn't yet have a corresponding design
  principle (e.g., "no muscle-memory-dependent workflows for P0-consequence personas") the
  way Manager/Admin got ADR-004.
- **Ellie's identity model reconciliation** is flagged as an Open Question but risks being
  forgotten by the time Ch. 16 is reached, given how many Open Questions accumulate across
  chapters — no tracking mechanism beyond individual chapter cross-references exists yet.

### 6.2 Blue Team Review

- Support-impersonation persona gap is accepted as valid — correctly belongs in
  [Ch. 48 — Operations](../part-9-governance-future/48-operations.md) (support tooling) with a security cross-reference
  to [Ch. 40](../part-8-operations/40-security.md), not fabricated here without operational context this chapter
  doesn't have.
- Auditor Alex design-principle gap is accepted; a lightweight principle is added below
  rather than deferred, since it is directly derivable from the persona already defined
  here.
- The cross-chapter Open-Question-tracking concern is valid at the AKB-process level (not
  specific to this chapter) — noted, but resolving it is a [Ch. 00-index](../00-index.md)
  process concern (e.g., a consolidated Open Questions register), not something Chapter 4
  itself can fix. Recommend the index adopt a running register once Part I closes.

**Corrective addendum (accepted from Red Team):**

**New design principle (extends ADR-004's spirit, informal — not a full ADR):** Any
workflow used by a P0-consequence, low-frequency persona (Auditor Alex today; potentially
others later) must be self-explanatory without prior training — clear in-context labeling,
no multi-step hidden gestures, and exportable evidence formats (PDF/CSV) as a first-class
output, not an edge case. Carried forward as a binding constraint on
[Ch. 26](../part-4-learning-domain/26-certification.md) and [Ch. 32](../part-6-insight/32-reporting.md) design.

### 6.3 CTO Review

| Item | Verdict | Reasoning |
|---|---|---|
| Persona set (§3–4) | **Approved** | Correctly differentiates architecture-relevant needs, not demographic flavor |
| Volume/priority matrix (§5) | **Approved with Conditions** | Directionally useful; condition is revalidation against real telemetry post-launch per Open Questions |
| ADR-004 (Manager/Admin UI separation) | **Approved** | Directly addresses a named, well-evidenced legacy anti-pattern |
| Auditor Alex design principle (Red Team addendum) | **Approved** | Low-cost, high-value constraint; binding on Ch. 26/32 |
| Support-impersonation persona gap | **Requires More Research** | Must be scoped in [Ch. 48 — Operations](../part-9-governance-future/48-operations.md) with security review from [Ch. 40](../part-8-operations/40-security.md) |
| Cross-chapter Open Question tracking | **Requires More Research** | Process gap at the AKB level; recommend a consolidated register be introduced when Part I closes (after Ch. 8) |

**Action items carried forward:**
1. [Ch. 48 — Operations](../part-9-governance-future/48-operations.md) must define a vendor support-impersonation
   persona and access model, reviewed by [Ch. 40 — Security](../part-8-operations/40-security.md).
2. [Ch. 26](../part-4-learning-domain/26-certification.md) and [Ch. 32](../part-6-insight/32-reporting.md) must honor the
   low-frequency/high-consequence design principle established in §6.2.
3. Consider introducing a consolidated Open Questions/Risk register at the end of Part I
   (after Ch. 8), per Blue Team recommendation.

---

*End of Chapter 4. Proceed to Chapter 5 — Learning Lifecycle.*
