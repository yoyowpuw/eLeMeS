# Chapter 27 — Video Streaming

> Part V — Media & Discovery · [Index](../00-index.md) · Previous: [Ch. 26 — Certification](../part-4-learning-domain/26-certification.md) · Next: Ch. 28 — File Storage

## 1. Purpose

Video Streaming is a Generic subdomain (Ch.10 §3) with a Conformist relationship to Course
Management (Ch.11 §4). This chapter selects the managed video vendor approach satisfying
NFR-003 (time-to-first-frame), BR-014 (10M+ minutes stored/growing), NFR-031 (captioning),
and NFR-051 (dependency degradation).

## 2. Build vs. Buy — Technology Evaluation

| Dimension | Build custom transcoding/streaming pipeline | **Managed video platform (Mux/Cloudflare Stream-class), Selected** |
|---|---|---|
| Fit to NFR-003 (adaptive bitrate, P95<2s broadband/<5s degraded) | Achievable but requires deep encoding/CDN expertise this AKB's team doesn't need to own | **Core competency of the vendor category; adaptive bitrate is table stakes** |
| Fit to BR-014 (10M+ minutes, growing) | High storage/transcoding infra cost to build and operate | Usage-based, vendor operates at internet scale already |
| Fit to Ch.1 §2.2 (integrate, don't rebuild adjacent categories) | Directly contradicts the boundary — video platforms are explicitly named as an integration point | **Consistent** |
| NFR-031 captioning | Would require building/integrating a separate captioning pipeline anyway | Most managed platforms include auto-captioning; human-review layer added on top for compliance-critical content |
| Cost (Ch.1 Principle 6 TCO) | High engineering + infra cost | Predictable usage-based cost |
| Final Recommendation | Rejected | **Selected** |

**Decision:** A managed video platform handles ingest, transcoding, adaptive-bitrate
streaming, and baseline auto-captioning; the platform's own Video Streaming context (Ch.11
#13) is a thin orchestration/metadata layer (`MediaAsset`, `StreamSession`) over the vendor
API — the Conformist relationship (Ch.11 §4) means this platform deliberately does not
abstract away the vendor's model.

## 3. Captioning Compliance Layer (Satisfies NFR-031)

Auto-generated captions from the vendor are treated as a first draft; compliance-critical
content (BR-015-tagged) requires human review/correction before publish, tracked as part of
the `ContentVersion` publish workflow (Ch.12 §7) — captions are versioned alongside the
video itself, not a detachable afterthought.

## 4. Dependency Degradation (Satisfies NFR-051)

Per Chapter 7's NFR-051 and Chapter 22's dependency-inventory obligation, Video Streaming is
formally registered as a critical external dependency: on vendor outage, previously-cached/
downloaded video (Ch.37 offline) remains playable; live ingest/transcoding of new content
gracefully queues rather than failing hard, and the platform surfaces a clear degraded-state
indicator rather than a generic error — detailed failover behavior owned by
[Ch. 42 — Disaster Recovery](../part-8-operations/42-disaster-recovery.md)'s dependency inventory.

## Summary
A managed video platform (not a custom-built pipeline) is selected for ingest, transcoding,
and adaptive-bitrate streaming, consistent with Chapter 1's integration boundary and
directly serving Frontline Fiona's low-bandwidth performance needs (NFR-003). Captioning is
vendor-generated but human-reviewed for compliance-critical content, versioned alongside
video. This context is formally registered as an NFR-051 critical dependency with defined
graceful-degradation behavior.

## Open Questions
Specific vendor selection deferred to Ch.46 Licensing cost/contract evaluation.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Vendor pricing model doesn't accommodate BR-011 burst patterns (Ch.2 §7 cost-framing risk) | Medium | Medium | [Ch. 45 — Cost Optimization](../part-8-operations/45-cost-optimization.md) must model burst-pricing exposure specifically for this vendor category |

## Architecture Decisions
**ADR-044: Managed video platform for transcoding/streaming; thin Conformist orchestration layer, no custom pipeline** — §2.

## Future Research
Vendor selection (Ch.46); burst-pricing modeling (Ch.45).

## Cross References
[Ch. 1](../part-1-foundations/01-enterprise-lms-overview.md) §2.2 · [Ch. 4](../part-1-foundations/04-user-personas.md) (Fiona) · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-003, 031, 051) · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #13 · [Ch. 37](../part-7-platform-integration/37-offline-learning.md) · [Ch. 42](../part-8-operations/42-disaster-recovery.md) · [Ch. 45](../part-8-operations/45-cost-optimization.md)

## Definition of Done
- [x] Build-vs-buy decided via Technology Evaluation Template
- [x] Captioning compliance workflow specified against NFR-031
- [x] Dependency degradation behavior specified against NFR-051

## Confidence Level
**High** — video-platform buy decision is extremely well-precedented across the industry and directly consistent with Chapter 1's stated boundary.

## 5. Chapter Review

**Red Team:** No consideration of data residency (NFR-023) for video content specifically —
does the chosen managed platform support region-pinned storage/processing consistent with
Chapter 12's residency model, or could video content inadvertently cross regions the
tenant's contract prohibits?

**Blue Team:** Accepted as a real gap. Addendum: vendor selection (Ch.46) must explicitly
evaluate regional processing/storage capability as a hard requirement, not an optional
nice-to-have, for any tenant under NFR-023 residency obligations.

**CTO:** ADR-044 **Approved with Conditions** — condition is [Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md)'s
vendor evaluation must include NFR-023 regional-residency capability as a gating criterion.

---
*End of Chapter 27. Proceed to Chapter 28 — File Storage.*
