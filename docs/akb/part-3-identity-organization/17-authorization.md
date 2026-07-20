# Chapter 17 — Authorization

> Part III — Identity & Organization · [Index](../00-index.md) · Previous: [Ch. 16 — Authentication](16-authentication.md) · Next: Ch. 18 — Multi-tenancy

## 1. Purpose

Per Chapter 10's Red-Team-corrected classification, Authorization is a **Supporting**
subdomain (build the policy *model*, don't necessarily build the policy *engine* from
scratch) — distinct from Authentication's Generic/buy treatment in Chapter 16. This chapter
delivers FR-004 (org-hierarchy-scoped RBAC/ABAC) and the concrete separation underlying
Chapter 4's ADR-004 (Manager vs. Admin UI).

## 2. Policy Evaluation Architecture — Technology Evaluation

| Dimension | Custom in-house RBAC tables/code | **Open Policy Agent (OPA)-class policy-as-code engine, Selected** | Commercial authorization-as-a-service (e.g., Permit.io-class) |
|---|---|---|---|
| Fit to org-hierarchy-scoped ABAC (FR-004) | Achievable but each of the 17 services reimplements evaluation logic independently, risking drift | Centralized policy language (Rego-class), consistently evaluated across all 17 services via sidecar/library | Also centralized, but adds a runtime external dependency for every authorization check |
| Consistency across bounded contexts (Ch.11) | Poor — high drift risk, echoing Ch.8 §5 item 9 (Cornerstone-style inconsistency) | Excellent — one policy source of truth | Excellent |
| Latency (NFR-001, in the hot path of every request) | Fast (in-process) | Fast if deployed as a local sidecar (no network hop) | Adds network latency risk unless heavily cached, at odds with NFR-001 |
| Vendor lock-in / exit strategy (Ch.1 Principle 5) | None (owned) | Low — open source, policy logic portable | Higher — proprietary policy language/runtime |
| Auditability (Auditor Alex, BR-002) | Manual, ad hoc | Policies are version-controlled, testable artifacts — strong audit story | Also strong, but external to this platform's own audit trail |
| Complexity (1-10) | 4 | 6 | 5 |
| Final Recommendation | Rejected — drift risk directly contradicts Ch.9's macroservice-discipline rationale | **Selected** | Rejected — external runtime dependency on the authorization hot path is an unacceptable NFR-001/availability risk given ADR-025's compliance-tier dependency rules (Ch.15) |

**Decision:** An open-source, policy-as-code engine (OPA-class), deployed as a **local
sidecar** to every service (not a remote call) — satisfying both centralized-policy
consistency and NFR-001 latency, and keeping the compliance-critical tier's dependency
rule (Ch.15 ADR-025) intact since the sidecar is local, not a new external service.

## 3. Authorization Model

| Layer | Mechanism |
|---|---|
| Coarse-grained (role) | RBAC: Learner, Manager, Admin, Auditor, Content Author, Integrator — mapped to Ch.4 personas |
| Fine-grained (scope) | ABAC: policy evaluates `(subject, action, resource, org-scope)` — e.g., Manager Maya's role grants read access to compliance dashboards, but the *org-scope* attribute (her position in Ch.11 #4's Org Hierarchy) restricts it to her reporting line only |
| Manager/Admin separation (Ch.4 ADR-004) | Enforced at the policy layer, not just the frontend (Ch.14) — a Manager role's policy set structurally excludes admin-configuration actions, so the separation is a genuine security boundary, not a UI convenience |
| Tenant isolation reinforcement | Every policy evaluation is implicitly tenant-scoped, layering on top of Ch.12's RLS as a second, independent enforcement point (defense-in-depth for NFR-021) |

## 4. Policy Change Governance

Policies are version-controlled artifacts (per §2's auditability row) with mandatory review
before deployment, feeding [Ch. 41 — Compliance](../part-8-operations/41-compliance.md)'s audit-evidence needs
directly — a policy change history is itself a compliance artifact.

## Summary
Authorization is built (per Chapter 10's Supporting-tier reclassification) using an
open-source policy-as-code engine deployed as a local sidecar, avoiding both the drift
risk of fully custom in-house logic and the latency/dependency risk of a commercial
authorization-as-a-service. RBAC (coarse role) plus ABAC (org-scope-aware) enforces Chapter
4's Manager/Admin separation as a genuine policy-layer security boundary, and doubles as
defense-in-depth alongside Chapter 12's row-level security.

## Open Questions
Whether policy evaluation results should be cached per-request-session to further reduce sidecar evaluation overhead at BR-011 peak concurrency — deferred to [Ch. 44](../part-8-operations/44-performance-optimization.md).

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Policy-as-code language (Rego-class) has a real learning curve, risking policy bugs | High (authorization bugs are security-critical) | Medium | Mandatory policy unit-testing gate in [Ch. 39 — DevOps](../part-8-operations/39-devops.md) CI |
| Sidecar deployment adds operational surface area to all 17 services | Medium | Medium | Standardized as part of the Ch.15 service template, not per-service bespoke configuration |

## Architecture Decisions
**ADR-028: OPA-class policy-as-code engine deployed as a local sidecar, not a remote authorization service or fully custom in-house logic** — §2.

## Future Research
Policy-evaluation caching strategy (Ch.44).

## Cross References
[Ch. 4](../part-1-foundations/04-user-personas.md) (ADR-004) · [Ch. 10](../part-2-system-domain-architecture/10-domain-driven-design.md) (Authorization reclassification) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) (RLS) · [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) · [Ch. 39](../part-8-operations/39-devops.md) · [Ch. 41](../part-8-operations/41-compliance.md)

## Definition of Done
- [x] Policy evaluation architecture selected via Technology Evaluation Template
- [x] RBAC+ABAC model specified with explicit Manager/Admin policy-layer enforcement
- [x] Defense-in-depth relationship to Ch.12 RLS made explicit
- [x] Policy-change governance tied to Ch.41 compliance evidence

## Confidence Level
**High** — policy-as-code-as-sidecar is a well-proven enterprise pattern; the specific engine choice is low-regret given open-source portability (Ch.1 Principle 5 exit strategy).

## 7. Chapter Review

**Red Team:** No discussion of policy-sidecar version skew — if 17 services deploy at
different times, they may briefly run different policy-engine or policy-bundle versions,
risking inconsistent authorization decisions across services during a rolling deploy.

**Blue Team:** Accepted as a real, if narrow, operational risk. Addendum: policy bundles
must be deployed via a centralized, atomically-versioned distribution mechanism (policy
bundle server) with services polling for updates on a short interval, and
[Ch. 39 — DevOps](../part-8-operations/39-devops.md) must treat policy-bundle rollout with the same rigor as a
service deployment (canary, rollback capability) — this is now a binding requirement, not
left as an unmanaged edge case.

**CTO:** ADR-028 **Approved with Conditions** — condition is the policy-bundle
atomic-distribution/rollout mechanism specified above must be implemented in
[Ch. 39](../part-8-operations/39-devops.md), not assumed.

---
*End of Chapter 17. Proceed to Chapter 18 — Multi-tenancy.*
