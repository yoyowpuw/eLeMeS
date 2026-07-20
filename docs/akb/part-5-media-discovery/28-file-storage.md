# Chapter 28 — File Storage

> Part V — Media & Discovery · [Index](../00-index.md) · Previous: [Ch. 27 — Video Streaming](27-video-streaming.md) · Next: Ch. 29 — Search

## 1. Purpose

File Storage (Ch.11 #14, Generic subdomain) provides the object storage layer underlying
Chapter 12 §7's content-hash versioning and Chapter 26's certificate export bundles. This
chapter selects the storage technology and defines residency, access-control, and
lifecycle policy.

## 2. Technology Evaluation

| Dimension | Build custom storage (self-hosted) | **Cloud object storage (S3-class, multi-cloud-portable API), Selected** |
|---|---|---|
| Fit to NFR-015 (11-nines durability) | Very hard to achieve independently | **Native to the service category** |
| Fit to Ch.12 §4 (multi-region residency) | Requires building regional replication from scratch | Native regional bucket/object placement |
| Cost (Ch.1 Principle 6) | High operational burden | Usage-based, extremely well-proven pricing model |
| Exit strategy (Ch.1 Principle 5) | N/A | S3-compatible API is a de facto portable standard across providers, keeping switching cost moderate rather than high |
| Final Recommendation | Rejected | **Selected** |

## 3. Content Addressing & Access Control

Objects are stored content-hash-addressed, directly implementing Chapter 12 §7's
version-pinning mechanism at the storage layer (immutable objects, never overwritten in
place — a new version is always a new object key). Access is mediated exclusively through
short-lived, signed URLs issued after an Authorization (Ch.17) policy check — no object is
ever made directly publicly accessible, including for "public" marketing-style content,
keeping one consistent access-control model platform-wide.

## 4. Residency & Lifecycle

Buckets are provisioned per-region, matching Chapter 12 §4's tenant-region pinning — a
tenant's uploaded/generated files never leave their assigned region, satisfying NFR-023.
Lifecycle policies apply cost-tiering (hot→cold storage transition) for aged,
infrequently-accessed content, feeding [Ch. 45 — Cost Optimization](../part-8-operations/45-cost-optimization.md).

## Summary
Cloud object storage (S3-compatible) is selected over self-hosted storage, chosen
specifically for its de facto API portability (bounding exit-strategy risk) and native fit
to durability and multi-region residency requirements. Objects are content-hash-addressed
and immutable, directly implementing Chapter 12's version-pinning mechanism at the storage
layer, with access exclusively via short-lived signed URLs gated by Chapter 17's
authorization policy.

## Open Questions
Specific cloud provider(s) — single-cloud vs. multi-cloud storage strategy — deferred to [Ch. 45 — Cost Optimization](../part-8-operations/45-cost-optimization.md) and [Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md).

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Signed-URL expiry misconfiguration could either leak long-lived access or break legitimate playback/download UX | Medium | Low-Medium | Standardized signed-URL TTL policy defined once in the shared service template (Ch.15), not per-service bespoke |

## Architecture Decisions
**ADR-045: Cloud object storage (S3-compatible API), content-hash-addressed, signed-URL-only access — no self-hosted storage, no directly public objects** — §2–3.

## Future Research
Single-cloud vs. multi-cloud storage strategy (Ch.45/46).

## Cross References
[Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §4, §7 · [Ch. 17](../part-3-identity-organization/17-authorization.md) · [Ch. 26](../part-4-learning-domain/26-certification.md) · [Ch. 45](../part-8-operations/45-cost-optimization.md)

## Definition of Done
- [x] Storage technology selected via Technology Evaluation Template
- [x] Content-hash addressing tied explicitly to Ch.12 §7 mechanism
- [x] Residency and signed-URL access control specified against NFR-023/authorization

## Confidence Level
**High** — cloud object storage is an industry-default, extremely low-risk choice; the interesting design decisions (hash-addressing, signed-URL-only) are direct extensions of already-approved patterns.

## 5. Chapter Review

**Red Team:** No consideration of large-file upload UX/reliability (e.g., a multi-GB video
source file from Author Amir, Ch.4) — signed-URL single-PUT uploads can fail on large files
over unreliable connections without resumable-upload support.

**Blue Team:** Accepted — addendum: uploads must use a resumable/multipart upload protocol
(standard cloud-storage capability), not naive single-request PUT, specified here as a
binding requirement rather than left to implementation-phase discovery.

**CTO:** ADR-045 **Approved with Conditions** — condition is resumable multipart upload is
explicitly required, per the Blue Team addendum.

---
*End of Chapter 28. Proceed to Chapter 29 — Search.*
