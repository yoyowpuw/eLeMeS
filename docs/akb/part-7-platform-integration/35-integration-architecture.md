# Chapter 35 — Integration Architecture

> Part VII — Platform & Integration · [Index](../00-index.md) · Previous: [Ch. 34 — Notification System](../part-6-insight/34-notification-system.md) · Next: Ch. 36 — Mobile Strategy

## 1. Purpose

The Integration Gateway context (Ch.11 #17) has been referenced by nearly every prior
chapter as the Anti-Corruption Layer boundary for HRIS, IdP, authoring tools, video,
marketplaces, and now foundation models. This chapter gives it a concrete architecture,
discharges the NFR-051 dependency-fallback obligation accumulated across five chapters, and
delivers NFR-043/044/045 (conformance, sync latency, API docs).

## 2. Guarding Against the "Dumping Ground" Risk (Chapter 11's Open Question)

Chapter 11 flagged Integration Gateway as a risk of becoming an unbounded dumping ground
(echoing Ch.8's Moodle/Cornerstone anti-patterns). Resolution: the Gateway is not one
service but a **pattern applied consistently across per-integration sub-modules**, each
independently versioned and owned by the bounded context it serves (e.g., the HRIS
connector is owned by Org Hierarchy's team, Ch.11 #4; the authoring-tool import connector
is owned by Course Management, Ch.11 #7) — "Integration Gateway" names an architectural
pattern (ACL discipline, standard connector interface) enforced platform-wide, not a single
monolithic service. This closes Chapter 11's Open Question with a governance answer, not
just a diagram.

## 3. Standard Connector Interface

Every integration (inbound: HRIS, IdP/SCIM; outbound: data warehouse export, webhook
notifications to partner systems) implements a common contract: authentication config,
field-mapping/transformation, sync-scheduling (real-time webhook vs. polling), error/retry
handling, and a standardized health/status endpoint feeding [Ch. 38 — Observability](../part-8-operations/38-observability.md)
— this consistency is what NFR-045 (public API documentation completeness) extends to
integration-facing documentation for Integrator Ivan.

## 4. Dependency Fallback Registry (Discharges Accumulated NFR-051 Obligations)

Per Chapter 7's NFR-051 and the explicit carry-forward obligations from Chapters 16
(CIAM), 22 (conformance engine), 23 (proctoring), and 27 (video), this chapter maintains
the canonical registry:

| Dependency | Owning Chapter | Fallback Behavior |
|---|---|---|
| CIAM/Identity | Ch.16 | Cached session validation continues briefly; new logins degrade to a status-page-communicated outage |
| Content conformance engine | Ch.22 | Already-downloaded/cached content (Ch.37) remains playable; new content publishing queues |
| Proctoring vendor | Ch.23 | High-stakes exams requiring proctoring are deferred/rescheduled; non-proctored assessments unaffected |
| Video platform | Ch.27 | Cached/offline video remains playable; new ingest queues |
| Foundation model API | Ch.31 | Core compliance flows entirely unaffected (Ch.1 Principle 4); AI features show a clear degraded-state indicator |
| HRIS | This chapter | Org Hierarchy operates on cached data (Ch.19 §3's 5-minute TTL pattern, extended here to the HRIS sync boundary itself) |
| Cloud KMS (certificate-signing key) | Ch.40 §3 | Certificate issuance queues (does not fail) during a brief KMS outage, reusing Ch.23's async submission-processing pattern; a sustained outage escalates per NFR-012's compliance-tier incident process — *added retroactively per Ch.40's Blue Team review, confirming this registry's "living document" status (ADR-058)* |

This table is the single canonical answer to NFR-051 across the whole AKB — future
chapters introducing new external dependencies must append to it here rather than defining
fallback behavior in isolation.

## Summary
Integration Gateway is confirmed as an architectural pattern (standard connector interface,
ACL discipline) applied per-bounded-context, not a monolithic dumping-ground service,
resolving Chapter 11's Open Question. This chapter also consolidates the NFR-051
dependency-fallback obligations accumulated across five prior chapters into a single
canonical registry, extending the same cached/degraded-mode principle to the HRIS sync
boundary.

## Open Questions
Real-time webhook vs. polling default for new integrations — implementation-phase, per-vendor-capability decision.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Per-context connector ownership (§2) could still drift into inconsistent implementations without a shared library | Medium | Medium | Standard connector interface (§3) should be a shared internal library/SDK, not just a documented convention, enforced via [Ch. 39 — DevOps](../part-8-operations/39-devops.md) code review tooling |
| Dependency fallback registry (§4) becomes stale as new integrations are added without updating it | Medium | Medium | Same governance discipline as Ch.8's Consolidated Risk Register (ADR-011) — must be actively maintained |

## Architecture Decisions
**ADR-057: Integration Gateway is an architectural pattern (standard connector interface + ACL), applied per-bounded-context, not a monolithic service** — §2, resolves Ch.11 Open Question. **ADR-058: Canonical dependency-fallback registry consolidating all NFR-051 obligations** — §4.

## Future Research
Shared connector SDK design (implementation phase).

## Cross References
[Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-043–045, 051) · [Ch. 8](../part-1-foundations/08-benchmark-analysis.md) (anti-patterns) · [Ch. 10](../part-2-system-domain-architecture/10-domain-driven-design.md) §4 (ACL) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #17 · [Ch. 16](../part-3-identity-organization/16-authentication.md), [Ch. 22](../part-4-learning-domain/22-course-management.md), [Ch. 23](../part-4-learning-domain/23-assessment-engine.md), [Ch. 27](../part-5-media-discovery/27-video-streaming.md), [Ch. 31](../part-5-media-discovery/31-ai-integration.md) (registry entries) · [Ch. 42](../part-8-operations/42-disaster-recovery.md)

## Definition of Done
- [x] Integration Gateway confirmed as pattern-not-service, resolving Ch.11 Open Question
- [x] Standard connector interface specified against NFR-043–045
- [x] Canonical NFR-051 dependency-fallback registry consolidated across 6 chapters

## Confidence Level
**High** — this chapter's primary value is consolidation and governance of already-established decisions, which is inherently lower-risk than introducing new architecture.

## 5. Chapter Review

**Red Team:** The dependency-fallback registry (§4) is presented as complete, but
[Ch. 42 — Disaster Recovery](../part-8-operations/42-disaster-recovery.md) and [Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md)
(video vendor, proctoring vendor, conformance engine, CIAM) haven't been written yet at
this point in the AKB — this registry may need updates once those chapters finalize actual
vendor selections and their specific failure modes.

**Blue Team:** Accepted — correctly caveated as provisional. Addendum: this registry (§4)
is explicitly marked a living artifact, to be revisited and finalized once
[Ch. 42](../part-8-operations/42-disaster-recovery.md) and [Ch. 46](../part-9-governance-future/46-licensing.md) complete vendor-specific
analysis — consistent with how Chapter 8's Consolidated Risk Register (ADR-011) is
maintained.

**CTO:** ADR-057 **Approved**. ADR-058 **Approved with Conditions** — condition is the
registry is explicitly living/provisional pending Ch.42/46, not presented as final.

---
*End of Chapter 35. Proceed to Chapter 36 — Mobile Strategy.*
