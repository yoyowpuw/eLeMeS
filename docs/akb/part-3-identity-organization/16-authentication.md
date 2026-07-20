# Chapter 16 — Authentication

> Part III — Identity & Organization · [Index](../00-index.md) · Previous: [Ch. 15 — Backend Architecture](../part-2-system-domain-architecture/15-backend-architecture.md) · Next: Ch. 17 — Authorization

## 1. Purpose

Deliver the authentication architecture for FR-001 (enterprise SSO), FR-002 (SCIM), and
FR-003 (external identity federation), and resolve Chapter 4's Open Question on
reconciling External Ellie's B2B2C identity model with standard enterprise SSO.

## 2. Build vs. Buy — Identity Platform

Per Ch.10 ADR-013, Authentication is a Generic subdomain — this is the sharpest Build/
Buy/Integrate test case in the AKB so far.

| Dimension | Build custom SAML/OIDC broker | **Buy: CIAM platform (Auth0/Okta CIC/Cognito-class), Selected** |
|---|---|---|
| Fit to FR-001 (multi-IdP per tenant) | Achievable but reinvents a solved, security-critical problem | Native multi-tenant SSO orchestration is the product's core purpose |
| Security risk (NFR-017-020) | High — auth bugs are catastrophic and this AKB's team is not an identity-security specialist team | Low — vendor's core competency, independently audited |
| Fit to FR-003 (external/B2B2C identity) | Requires building consumer-identity flows from scratch | Native support (this is exactly what CIAM platforms are built for, distinct from workforce IdP) |
| Cost (Ch.1 Principle 6 TCO) | High engineering cost, ongoing security-maintenance burden | Predictable per-MAU licensing cost |
| Exit strategy | N/A (owned) | Standards-based (OIDC/SAML) — the platform's *own* identity layer speaks standard protocols to the CIAM vendor, so switching vendors means reconfiguration, not a rebuild |
| Final Recommendation | Rejected — directly contradicts Ch.1 §2.2's "integrate, don't rebuild" boundary applied to a security-critical Generic subdomain | **Selected** |

**Decision:** Buy a CIAM platform as the identity broker sitting between (a) each tenant's
enterprise IdP (their own Okta/Azure AD, consumed via standard SAML2/OIDC) and (b) the
platform's own Identity & Auth bounded context (Ch.11 #1), which issues the platform's own
session tokens after the CIAM layer confirms authentication.

## 3. Resolving External Ellie's Identity Model (Ch.4 Open Question)

The CIAM platform selection directly resolves this: workforce learners authenticate via
**tenant-configured enterprise SSO** (SAML2/OIDC to their own IdP), while external
learners (partners, franchisees, customers — Ellie persona) authenticate via the **same
CIAM platform's B2B2C identity flows** (email/password, social login, or partner-organization-
scoped SSO), both terminating in the same platform-internal session model. This means Ch.11
#1's `Session` aggregate is identity-source-agnostic — it doesn't need two different session
models, only two different upstream authentication flows feeding one downstream contract.
This closes Chapter 4's Open Question with a concrete mechanism.

## 4. SCIM Provisioning (FR-002)

Standard SCIM 2.0 endpoint consumed by tenant IdPs for automated user lifecycle
(create/update/deactivate), feeding the Org Hierarchy context (Ch.11 #4) for role/org data
and satisfying NFR-044's 15-minute propagation target in combination with [Ch. 35](../part-7-platform-integration/35-integration-architecture.md)'s
HRIS sync.

## 5. Session & MFA

| Aspect | Decision |
|---|---|
| Session token | Short-lived JWT (platform-issued, per §2) + refresh token, validated by every service via the mesh-issued identity per Ch.15 §6 |
| MFA | Delegated to the tenant's own IdP where SSO is used (workforce learners); CIAM-native MFA (TOTP/WebAuthn) for external learners (Ellie) lacking an enterprise IdP |
| Break-glass/support access (FR-008) | Separate, more tightly scoped token type, mandatory MFA, full audit log per NFR-022 — never issued via the standard learner session flow |

## Summary
A bought CIAM platform (not a custom-built broker) mediates between tenant enterprise IdPs
and the platform's own session model, directly satisfying FR-001–003 while respecting
Chapter 1's integrate-don't-rebuild boundary for a security-critical Generic subdomain.
This concretely resolves Chapter 4's External Ellie B2B2C identity Open Question: two
upstream authentication paths (enterprise SSO, CIAM-native B2B2C) converge on one
downstream, identity-source-agnostic session contract.

## Open Questions
Specific CIAM vendor selection (Auth0 vs. Okta CIC vs. Cognito) deferred to implementation-phase procurement, evaluated against Ch.46 Licensing cost modeling.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| CIAM vendor outage blocks all platform login | Very High | Low | Directly addressed by NFR-051 (Ch.7); [Ch. 42](../part-8-operations/42-disaster-recovery.md) must define a degraded-mode/cached-session fallback |
| Break-glass token type reused informally for non-support purposes | High | Low-Medium | NFR-022 audit logging plus quarterly access review, specified in [Ch. 48](../part-9-governance-future/48-operations.md) |

## Architecture Decisions
**ADR-026: Buy a CIAM platform as the identity broker; do not build a custom SAML/OIDC broker** — §2. **ADR-027: Single identity-source-agnostic session contract serving both enterprise-SSO and B2B2C authentication paths** — §3, resolves Ch.4 Open Question.

## Future Research
CIAM vendor selection (Ch.46).

## Cross References
[Ch. 4](../part-1-foundations/04-user-personas.md) · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-001–003) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) · [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) · [Ch. 42](../part-8-operations/42-disaster-recovery.md) · [Ch. 46](../part-9-governance-future/46-licensing.md)

## Definition of Done
- [x] Build-vs-buy decided via Technology Evaluation Template
- [x] Ch.4's External Ellie Open Question resolved with a concrete mechanism
- [x] SCIM provisioning specified against FR-002/NFR-044
- [x] Session and MFA model specified, including break-glass token type (FR-008)

## Confidence Level
**High** — buying a CIAM platform for a Generic subdomain is a well-precedented, low-risk enterprise pattern.

## 6. Chapter Review

**Red Team:** No mention of what happens when a tenant's own enterprise IdP (not the CIAM
vendor) is unavailable — a different, more likely failure mode than CIAM-platform-wide
outage, since it's tenant-specific and entirely outside this platform's control.

**Blue Team:** Accepted — valid, distinct failure mode from the one already captured.
Addendum: tenant-IdP-specific outages are a tenant-communication and status-page concern
([Ch. 48 — Operations](../part-9-governance-future/48-operations.md)) rather than an architectural mitigation this
platform can solve (this platform cannot make a customer's own Okta tenant more available)
— but the platform must clearly surface IdP-specific auth failures in its status/observability
tooling so support isn't blamed for a tenant-side outage.

**CTO:** ADR-026/027 **Approved**. Action item: [Ch. 48](../part-9-governance-future/48-operations.md) to define
tenant-IdP-outage communication process, distinct from CIAM-platform-outage handling.

---
*End of Chapter 16. Proceed to Chapter 17 — Authorization.*
