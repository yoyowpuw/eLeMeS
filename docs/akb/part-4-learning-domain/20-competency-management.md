# Chapter 20 — Competency Management

> Part IV — Learning Domain · [Index](../00-index.md) · Previous: [Ch. 19 — Organization Hierarchy](../part-3-identity-organization/19-organization-hierarchy.md) · Next: Ch. 21 — Learning Paths

## 1. Purpose

Competency Management is a Core subdomain (Ch.10 §3) realizing BR-004 (internal mobility)
and FR-010/011 (competency models, gap computation). This chapter also carries a binding
constraint from Chapter 8 (Docebo's extensibility-ceiling anti-pattern, item 10): the
competency data model must not repeat incumbents' rigidity.

## 2. Competency Taxonomy Approach — Technology Evaluation

| Dimension | Fully custom proprietary taxonomy | Adopt external standard (O*NET/SFIA) wholesale | **Hybrid: tenant-defined models + optional standard-framework import/mapping, Selected** |
|---|---|---|---|
| Fit to enterprise reality (each tenant has its own competency language) | Poor — forces every tenant into one vendor taxonomy, the exact Docebo-class rigidity flagged in Ch.8 | Poor — same problem, different source | **Good — tenants define their own competencies natively; standard frameworks are an optional import/starter-content source, not a constraint** |
| Time-to-value for new tenants (NFR-010 provisioning) | N/A | Fast (pre-built) | Fast for tenants who import a standard framework as a starting point; flexible for tenants who don't |
| Cross-tenant benchmarking potential (a possible future analytics feature) | Impossible | Possible | Possible, opt-in, only where tenants adopted the same standard mapping |
| Extensibility ceiling risk (Ch.8 §5 item 10) | High | High | **Low — directly designed to avoid this named anti-pattern** |
| Complexity (1-10) | 3 | 3 | 5 |
| Final Recommendation | Rejected | Rejected | **Selected** |

**Decision:** Competencies are tenant-owned entities (proficiency-leveled, role-mappable)
with optional import/crosswalk mapping to external standard frameworks (O*NET, SFIA) as
accelerator content, not a structural constraint — directly discharging the Chapter 8 Blue
Team's instruction to avoid Docebo/SAP SuccessFactors-class rigidity (Ch.8 §5 items 10 and
"Extensibility ceiling").

## 3. Data Model

| Aggregate (Ch.11 #5) | Key Attributes | Notes |
|---|---|---|
| `Competency` | name, description, proficiency scale (tenant-definable, e.g., 1-5 or novice/expert) | Tenant-scoped |
| `RoleProfile` | target role, required competencies + minimum proficiency | Sourced from HRIS role data where available (Ch.35), manually curated otherwise |
| `LearnerCompetencyProfile` | current assessed/inferred proficiency per competency | Updated by Assessment (Ch.23) results and manager attestation |
| `CompetencyGap` | computed delta between `LearnerCompetencyProfile` and target `RoleProfile` | Read-model, recomputed on relevant events (`AssessmentGraded`, `RoleChanged`) |

## 4. Gap Computation (Satisfies FR-011)

Event-driven recomputation (not on-demand batch): a `CompetencyGap` read model updates
reactively when `AssessmentGraded` or an HRIS-sourced role-change event fires, keeping gap
data fresh for Manager Maya's dashboards (Ch.32) without expensive nightly batch jobs —
consistent with Chapter 12 §5's CDC/event-driven standard-tier pattern.

## Summary
Competency Management adopts a hybrid taxonomy approach — tenant-owned competency models
with optional standard-framework (O*NET/SFIA) import as accelerator content — specifically
designed to avoid the extensibility-ceiling anti-pattern Chapter 8 identified in incumbent
enterprise talent suites. Gap computation is event-driven, consistent with the platform's
standard-tier CDC pattern from Chapter 12.

## Open Questions
Whether cross-tenant, opt-in benchmarking (anonymized competency-gap comparison across tenants using the same standard framework) is a future product opportunity — flagged for [Ch. 50 — Future Roadmap](../part-9-governance-future/50-future-roadmap.md), not decided here.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Tenant-defined taxonomies fragment so badly that even basic cross-tenant reporting benchmarks (Ch.2 §6 KPIs) become incomparable | Medium | Medium | Standard-framework mapping (§2) is opt-in but should be actively encouraged during onboarding (Ch.18 §4) as a default suggestion, not just a hidden option |

## Architecture Decisions
**ADR-033: Hybrid tenant-owned competency taxonomy with optional standard-framework import, not a fixed proprietary or wholesale-external taxonomy** — §2, directly discharges Ch.8 Blue Team guidance.

## Future Research
Cross-tenant benchmarking feature feasibility (Ch.50).

## Cross References
[Ch. 5](../part-1-foundations/05-learning-lifecycle.md) (Phase 1) · [Ch. 8](../part-1-foundations/08-benchmark-analysis.md) §5 · [Ch. 10](../part-2-system-domain-architecture/10-domain-driven-design.md) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #5 · [Ch. 21](21-learning-paths.md) · [Ch. 23](23-assessment-engine.md) · [Ch. 32](../part-6-insight/32-reporting.md)

## Definition of Done
- [x] Taxonomy approach selected via Technology Evaluation Template, explicitly countering a named Ch.8 anti-pattern
- [x] Data model specified with aggregate ownership
- [x] Gap computation mechanism specified as event-driven

## Confidence Level
**High** — directly and traceably responds to prior chapters' explicit guidance (Ch.8 Blue Team) rather than introducing independent new judgment calls.

## 5. Chapter Review

**Red Team:** Onboarding "actively encouraging" standard-framework adoption (Risk
mitigation) is vague — no mechanism is specified for *how* this is surfaced to Admin Aisha
during setup, risking it being dropped as a UX afterthought.

**Blue Team:** Accepted — addendum: framework-import is specified as a mandatory step
(skippable, but not hidden) in the tenant onboarding wizard defined in
[Ch. 18](../part-3-identity-organization/18-multi-tenancy.md) §4's provisioning sequence, not an optional settings-page
feature discovered later.

**CTO:** ADR-033 **Approved with Conditions** — condition is [Ch. 18](../part-3-identity-organization/18-multi-tenancy.md)'s
onboarding flow explicitly includes the framework-import step per the Blue Team addendum.

---
*End of Chapter 20. Proceed to Chapter 21 — Learning Paths.*
