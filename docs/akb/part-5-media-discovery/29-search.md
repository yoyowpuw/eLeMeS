# Chapter 29 — Search

> Part V — Media & Discovery · [Index](../00-index.md) · Previous: [Ch. 28 — File Storage](28-file-storage.md) · Next: Ch. 30 — Recommendation Engine

## 1. Purpose

Deliver FR-028 (sub-second P95 full-text/faceted search across 100k+ item catalogs) and
resolve Chapter 11's Open Question on whether Search and Recommendation should remain
separate bounded contexts.

## 2. Search Technology — Technology Evaluation

| Dimension | PostgreSQL full-text search (reuse Ch.12 platform) | **Managed search engine (OpenSearch/Elasticsearch-class), Selected** | SaaS search (Algolia-class) |
|---|---|---|---|
| Fit to NFR-002 (P95<1s, faceted, 100k+ items) | Adequate for simple text match; weak for rich faceting/relevance tuning at this catalog size | **Purpose-built for exactly this** | Purpose-built, excellent DX |
| Fit to NFR-023 (region-pinned residency, per Ch.12/28) | Inherits Postgres's already-solved regional topology | Deployable region-pinned via managed cluster placement | Weaker direct control over exact regional processing, vendor-dependent |
| Cost at BR-007 aggregate scale (many tenants' catalogs) | Lowest — no new infra | Moderate — cluster-based, scales with data volume, not per-query | Can balloon — often priced per-record/per-query, risky at this aggregate scale (Ch.2 §7 burst-cost framing) |
| Vendor lock-in / exit strategy (Ch.1 Principle 5) | None | Low — open-source core, portable index format broadly | Higher — proprietary index/ranking, harder to replicate elsewhere |
| Complexity (1-10) | 3 | 6 | 4 (as a consumer), but less control |
| Final Recommendation | Rejected — insufficient for FR-028's faceting/scale bar | **Selected** | Rejected — cost/residency-control risk at this AKB's scale outweighs DX convenience |

**Decision:** A managed OpenSearch/Elasticsearch-class engine, fed via CDC from the Course
Management and Competency contexts (Ch.11 #5/#7) through the event bus (Ch.9 §3, Ch.12
§5's standard-tier CDC pattern) — Search holds only a read-optimized index, never the
system of record.

## 3. Resolving Chapter 11's Search/Recommendation Separation Question

**Confirmed: remain separate contexts.** Search (Ch.11 #11) is synchronous-latency-critical
(NFR-002) and privacy-light (querying catalog metadata, not learner behavior).
Recommendation (Ch.11 #12) is privacy-heavy (Ch.3 ADR-003's CISO/DPO-vs-CLO tension over
behavioral data) and tolerant of asynchronous/batch computation. These are genuinely
different NFR and data-sensitivity profiles, not an artificial split — confirming Chapter
11's provisional boundary rather than merging, which would force Search's tight latency
requirement and Recommendation's privacy-governance requirement into one component's
design constraints unnecessarily.

## Summary
A managed search engine, not Postgres full-text search or SaaS search, is selected for
FR-028's faceted, sub-second, 100k+-item catalog search — chosen specifically for its
balance of purpose-built capability, residency control, and bounded vendor lock-in at
BR-007's aggregate scale. Chapter 11's Search/Recommendation context separation is
confirmed as genuine (differing latency and privacy-sensitivity profiles), not artificial.

## Open Questions
Specific cluster sizing/sharding-per-tenant-vs-shared-index strategy — deferred to [Ch. 43 — Scalability](../part-8-operations/43-scalability.md).

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Shared search index across pooled tenants (Ch.12 §2) risks cross-tenant query leakage if not carefully filtered | High | Low-Medium | Mandatory tenant-scoped query filtering at the index-query layer, tested with the same rigor as NFR-021's RLS isolation suite |
| CDC lag between system-of-record updates and search-index freshness creates a stale-search-result window | Low-Medium | Medium | Acceptable for catalog search (not compliance-critical); bound and monitor via [Ch. 38 — Observability](../part-8-operations/38-observability.md) |

## Architecture Decisions
**ADR-046: Managed search engine (OpenSearch/Elasticsearch-class) as a CDC-fed, read-optimized index — never the system of record** — §2. **ADR-047: Search and Recommendation remain separate bounded contexts** — §3, confirms Ch.11.

## Future Research
Cluster sizing/sharding strategy (Ch.43).

## Cross References
[Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-028) · [Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-002, 023) · [Ch. 9](../part-2-system-domain-architecture/09-product-architecture.md) §3 · [Ch. 11](../part-2-system-domain-architecture/11-bounded-contexts.md) #11/#12 · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §5 · [Ch. 30](30-recommendation-engine.md) · [Ch. 43](../part-8-operations/43-scalability.md)

## Definition of Done
- [x] Search technology selected via Technology Evaluation Template
- [x] Ch.11's context-separation Open Question explicitly resolved with rationale
- [x] Cross-tenant query-isolation risk identified with test-parity mitigation to NFR-021

## Confidence Level
**High** — search-engine selection and CDC-fed read-model pattern are both well-established, low-risk industry patterns already consistent with Chapter 12's approach.

## 6. Chapter Review

**Red Team:** No mention of search relevance-tuning governance — who decides ranking
rules, and could a tenant's own biased/gamed content metadata skew results in a way that
disadvantages certain content unfairly (e.g., compliance-critical content buried below
popular but non-mandatory content)?

**Blue Team:** Accepted as a legitimate product-governance question, but correctly scoped
outside architecture: relevance-tuning *policy* (e.g., boosting mandatory/compliance
content in ranking) is a [Ch. 30 — Recommendation Engine](30-recommendation-engine.md)-
adjacent product-configuration concern, not a Search *architecture* decision — this
chapter's job is to make ranking configurable, not to set the ranking policy itself.
Cross-reference added rather than solved here.

**CTO:** ADR-046/047 **Approved**. Note: ranking-policy governance explicitly assigned to
product/[Ch. 30](30-recommendation-engine.md), not silently missing from this chapter.

---
*End of Chapter 29. Proceed to Chapter 30 — Recommendation Engine.*
