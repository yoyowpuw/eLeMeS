# Chapter 33 — Analytics

> Part VI — Insight · [Index](../00-index.md) · Previous: [Ch. 32 — Reporting](32-reporting.md) · Next: Ch. 34 — Notification System

## 1. Purpose

Deliver FR-034 (aggregate-vs-individual analytics separation for Works Council compliance)
with a concrete architectural mechanism, and select the analytics warehouse technology
Chapter 12 §4 deferred here (the CDC destination).

## 2. Analytics Warehouse — Technology Evaluation

| Dimension | Extend PostgreSQL (Ch.12) for analytics workloads | **Dedicated columnar OLAP warehouse (Snowflake/BigQuery-class), Selected** |
|---|---|---|
| Fit to cross-tenant aggregate reporting at BR-007 scale | Poor — row-store OLTP engines degrade badly on large aggregate scans | **Purpose-built for exactly this query pattern** |
| Isolation from OLTP workload (protects NFR-001) | Risky — analytical queries competing with transactional load on the same engine | **Fully isolated — separate compute/storage entirely** |
| Cost model | Avoids new technology cost | Usage/compute-based, well-understood at this data volume |
| Multi-region/residency (NFR-023) | Inherits Postgres's regional topology | Requires explicit regional warehouse instances, mirroring Ch.12 §4's per-region model |
| Final Recommendation | Rejected — would risk NFR-001 by co-locating analytical and transactional load | **Selected** |

**Decision:** A dedicated, region-pinned columnar OLAP warehouse, fed via the CDC pipelines
Chapter 12 §5 already established for the 14 standard-tier contexts (and periodic
snapshot export for the 3 event-sourced compliance-tier contexts) — this is the concrete
technology behind Chapter 12 §4's "Analytics Warehouse" box.

## 3. Aggregate-vs-Individual Separation — Concrete Mechanism (Discharges FR-034)

| Layer | Mechanism |
|---|---|
| Ingestion | Individual-level (learner-attributable) events land in a restricted-access warehouse schema | 
| Aggregation | A separate, broadly-accessible schema contains only pre-aggregated, cohort/org-level materialized views (never queryable down to an individual) |
| Access control | Manager/Executive views (Ch.32) query *only* the aggregate schema by construction — the restricted individual-level schema is accessible solely to the specific compliance-evidence lookups already governed by Ch.17/Ch.26 (e.g., Auditor Alex's certificate lookup), not general analytics users |
| Tenant configuration | Tenants with Works Council/EU co-determination constraints (Ch.3 §8.2) can disable individual-level schema access entirely for that tenant, leaving only aggregate analytics available | 

This is the concrete architectural mechanism Chapter 3's CTO action item and Chapter 6's
FR-034 required — not a query-time filter (which could be bypassed or misconfigured) but a
**schema-level separation with independently governed access**, giving the aggregate/
individual boundary the same structural rigor Chapter 32's Red Team demanded for the
Executive Summary view.

## 4. Content-Effectiveness Analytics (Serves Author Amir, Ch.4 §4.3)

Content-level aggregate metrics (completion rate, average time-to-complete, assessment
pass-rate per `ContentVersion`) are computed in the aggregate schema, giving Author Amir
data-driven content-effectiveness insight without exposing individual learner performance —
naturally satisfying his persona need within the same schema-separation model.

## Summary
A dedicated, region-pinned columnar OLAP warehouse (not an extended Postgres analytics
workload) is selected, resolving Chapter 12 §4's deferred technology choice while
protecting NFR-001 from analytical-query interference. FR-034's aggregate-vs-individual
separation is implemented as genuine schema-level isolation with independently governed
access — not a query filter — including a tenant-level kill switch for individual-level
schema access to satisfy Works Council requirements, and content-effectiveness analytics
for Author Amir fall naturally out of the aggregate schema.

## Open Questions
Specific OLAP vendor selection deferred to [Ch. 45 — Cost Optimization](../part-8-operations/45-cost-optimization.md)/[Ch. 46 — Licensing](../part-9-governance-future/46-licensing.md).

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| CDC-to-warehouse latency creates a reporting-freshness gap larger than Reporting's own read-model lag (Ch.32) | Low-Medium | Medium | Explicit, separate staleness SLA for warehouse-derived analytics vs. near-real-time operational reporting (Ch.32) — should not be conflated |
| Individual-level schema access, even when governed, remains a re-identification risk if aggregate views are too granular (small-cohort aggregation could de-anonymize) | Medium | Low-Medium | Minimum-cohort-size thresholds on aggregate views (e.g., suppress results for cohorts below N learners) — new requirement, flagged for [Ch. 41 — Compliance](../part-8-operations/41-compliance.md) |

## Architecture Decisions
**ADR-054: Dedicated, region-pinned columnar OLAP warehouse for analytics, isolated from OLTP** — §2, resolves Ch.12 §4's deferred choice. **ADR-055: Schema-level (not query-filter-level) separation of individual and aggregate analytics data, with tenant-level kill switch** — §3, discharges FR-034.

## Future Research
OLAP vendor selection (Ch.45/46); minimum-cohort-size threshold policy (Ch.41).

## Cross References
[Ch. 3](../part-1-foundations/03-stakeholders.md) §8.2 · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-034) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §4–5 · [Ch. 32](32-reporting.md) · [Ch. 41](../part-8-operations/41-compliance.md) · [Ch. 45](../part-8-operations/45-cost-optimization.md)

## Definition of Done
- [x] OLAP warehouse technology selected via Technology Evaluation Template, resolving Ch.12's deferred choice
- [x] FR-034 discharged with genuine schema-level (not query-filter) separation mechanism
- [x] Tenant-level Works Council kill switch specified
- [x] Content-effectiveness analytics shown to fall out of the aggregate schema naturally

## Confidence Level
**High** — warehouse technology choice is industry-standard for this scale; the schema-separation mechanism directly and rigorously answers a requirement (FR-034) that has been carried and re-emphasized across four prior chapters.

## 6. Chapter Review

**Red Team:** The new small-cohort re-identification risk (Risks table) is significant
enough that it probably shouldn't be a "Future Research" hand-off to Chapter 41 — it's a
gap in *this* chapter's own aggregate-schema design (§3) that a determined analyst could
exploit today, not a distant future concern.

**Blue Team:** Accepted — elevated from Future Research to a binding requirement.
Addendum: aggregate materialized views (§3) must enforce a **minimum-cohort-size
suppression threshold (default: 5 learners) at view-generation time**, not left as an
unenforced policy recommendation for a later chapter to someday define.

**CTO:** ADR-055 **Approved with Conditions** — condition is the minimum-cohort-size
suppression threshold from the Blue Team addendum is binding and implemented at
view-generation time, not deferred.

---
*End of Chapter 33. Proceed to Chapter 34 — Notification System.*
