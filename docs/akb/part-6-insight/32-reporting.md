# Chapter 32 — Reporting

> Part VI — Insight · [Index](../00-index.md) · Previous: [Ch. 31 — AI Integration](../part-5-media-discovery/31-ai-integration.md) · Next: Ch. 33 — Analytics

## 1. Purpose

Deliver FR-033 (manager-scoped dashboards, structurally separate from admin UI per Ch.4
ADR-004), FR-027/NFR-034 (auditor export, confirming Ch.26's implementation), and Chapter
2 §6's business KPIs — while resolving Chapter 4's Open Question on whether an Executive
Sponsor persona needs separation from Manager Maya.

## 2. Reporting Architecture

Reporting is a read-model consumer, never a system of record (Ch.10 §3 Supporting
classification) — it projects from the event-sourced compliance tier (Ch.12 §5) and the
CDC-fed standard tier, materializing tenant/org/individual-scoped views without owning any
source-of-truth data itself. This keeps Reporting swappable and horizontally scalable
independent of the compliance-critical tier's stricter availability requirements (Ch.15
§4) — a reporting outage never risks a compliance-tier SLA breach, and vice versa.

## 3. Persona-Scoped Views

| View | Persona | Scope | Data Source |
|---|---|---|---|
| Team compliance dashboard | Manager Maya | Own reporting line only (Ch.19 org-scope via Ch.17 ABAC) | Event-sourced compliance tier read model |
| Compliance/configuration reports | Admin Aisha | Full tenant (or delegated org scope) | Same, broader scope |
| Auditor lookup/export | Auditor Alex | Individual/cohort record lookup, signed export | Ch.26's `Certificate` event log directly |
| Executive summary | **New — see §4** | Cross-org aggregate KPIs only, no individual drill-down | Aggregated read model |

## 4. Resolving the Executive Sponsor Persona Question (Ch.4 Open Question)

**Decision: yes, a distinct Executive Summary view is warranted**, but not a full distinct
*persona* requiring its own chapter-level treatment — it is a **restricted view mode** of
the Manager/Admin reporting surface: aggregate-only (no individual learner drill-down),
matching Chapter 2 §6's KPIs (compliance completion rate, time-to-competency, etc.)
directly, and satisfying Chapter 3's Works Council aggregate-vs-individual separation
(FR-034) by construction — an executive literally cannot drill into individual data through
this view, which is both a UX simplification and a privacy safeguard. This resolves
Chapter 4's Open Question: Executive Sponsor doesn't need new architecture, only a new
view-scope on already-planned reporting infrastructure.

## Summary
Reporting is architected as a pure read-model layer over the event-sourced compliance tier
and CDC-fed standard tier, never a source of truth, keeping it independently scalable and
failure-isolated from the compliance-critical tier. Chapter 4's Executive Sponsor Open
Question is resolved: an aggregate-only Executive Summary view (not a new persona/context)
satisfies both the KPI-reporting need (Ch.2 §6) and the Works Council aggregate-vs-
individual constraint (FR-034) by construction.

## Open Questions
Whether report-definition customization (tenant-specific custom reports) should be self-service (Admin Aisha configurable) or require engineering support — implementation-phase product decision.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Read-model projection lag creates a reporting/reality gap during high-volume periods (BR-011 peaks) | Medium | Medium | Explicit staleness indicator in the UI ("as of" timestamp), consistent with the same honesty principle applied to Search's CDC lag (Ch.29) |

## Architecture Decisions
**ADR-052: Reporting is a pure read-model layer, never a system of record, independently scalable from the compliance-critical tier** — §2. **ADR-053: Executive Summary is an aggregate-only view mode, not a separate persona/context** — §4, resolves Ch.4 Open Question.

## Future Research
Self-service custom report builder feasibility (implementation phase).

## Cross References
[Ch. 2](../part-1-foundations/02-business-requirements.md) §6 · [Ch. 3](../part-1-foundations/03-stakeholders.md) (FR-034) · [Ch. 4](../part-1-foundations/04-user-personas.md) (Open Question, ADR-004) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §5 · [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) §4 · [Ch. 26](../part-4-learning-domain/26-certification.md) §4

## Definition of Done
- [x] Reporting architecture specified as a read-model layer, isolated from compliance-tier SLA
- [x] Persona-scoped views specified including confirmed auditor export
- [x] Ch.4's Executive Sponsor Open Question explicitly resolved

## Confidence Level
**High** — this chapter composes already-approved patterns (event sourcing, CDC, ABAC scoping) rather than introducing new architectural risk.

## 5. Chapter Review

**Red Team:** The Executive Summary view's "cannot drill into individual data" guarantee
(§4) needs to be enforced at the authorization-policy layer (Ch.17), not just by omitting
a UI button — otherwise it's a UX convention, not a real privacy safeguard, and a
sufficiently technical executive could still query the underlying API.

**Blue Team:** Accepted — critical distinction. Addendum: the Executive Summary role must
be a genuinely distinct OPA policy role (Ch.17 §3) with no grant path to individual-level
endpoints, not merely a frontend view restriction — this is now binding, closing the gap
between "UX convention" and "real safeguard."

**CTO:** ADR-053 **Approved with Conditions** — condition is [Ch. 17 — Authorization](../part-3-identity-organization/17-authorization.md)'s
policy set must enforce the Executive Summary restriction structurally, per the Blue Team
addendum.

---
*End of Chapter 32. Proceed to Chapter 33 — Analytics.*
