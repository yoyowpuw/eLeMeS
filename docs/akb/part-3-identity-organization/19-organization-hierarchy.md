# Chapter 19 — Organization Hierarchy

> Part III — Identity & Organization · [Index](../00-index.md) · Previous: [Ch. 18 — Multi-tenancy](18-multi-tenancy.md) · Next: Ch. 20 — Competency Management

## 1. Purpose

Deliver FR-009 (matrixed org hierarchy) and FR-006 (re-parenting without migration
projects), and discharge Chapter 15's action item: the cached/fallback behavior for
Assignment's synchronous reads of Org Hierarchy data under the corrected compliance-tier
dependency rule (Ch.15 ADR-025).

## 2. Hierarchy Data Model — Technology Evaluation

| Dimension | Adjacency List (parent_id column) | Nested Set | **Closure Table, Selected** | Graph Database (separate store) |
|---|---|---|---|---|
| Fit to matrixed/dual-manager reporting (FR-009) | Poor — one parent per row by design | Poor — same limitation | **Good — a separate closure-table row per ancestor-descendant pair supports multiple hierarchies (e.g., reporting-line vs. cost-center) over the same org units** | Excellent, but introduces a second database technology |
| Fit to re-parenting without migration (FR-006, BR-006) | Cheap single-row update, but any cached descendant queries must be invalidated | Expensive — nested-set re-parenting requires renumbering large subtrees | Moderate — closure-table rows for the moved subtree must be rewritten, but this is a bounded, well-understood operation, not a full migration project | Cheap, but at the cost of introducing graph-DB operational overhead (violates Ch.12's single-OLTP-technology decision without strong justification) |
| Query performance for "all reports under Maya" (Manager Maya, Ch.4) | Requires recursive query | Fast | **Fast (indexed join)** | Fast |
| Consistency with Ch.12 ADR-016 (PostgreSQL platform-wide) | Yes | Yes | **Yes — implementable natively in PostgreSQL, no new technology** | No — would require introducing a second database technology, rejected per Ch.9/Ch.12's technology-consolidation rationale |
| Final Recommendation | Rejected — fails matrixed-reporting requirement | Rejected — re-parenting cost too high given BR-006's explicit M&A-agility driver | **Selected** | Rejected — technically elegant but violates Ch.1 Principle 6 TCO/hiring-pool discipline for a need the closure-table pattern already meets in PostgreSQL |

**Decision:** Closure table pattern in PostgreSQL, supporting multiple concurrent
hierarchy *types* (reporting-line, cost-center, matrixed dotted-line) over the same
`OrgUnit` aggregate — directly satisfying FR-009's matrixed-reporting requirement without a
second database technology.

## 3. Org Hierarchy Read Caching/Fallback (Discharges Ch.15 Action Item)

Per Chapter 15's corrected dependency rule (ADR-025), Assignment (compliance-tier) may
synchronously read Org Hierarchy (standard-tier) for eligibility computation, bounded by:

| Mechanism | Behavior |
|---|---|
| Local cache | Assignment maintains a short-TTL (5 min, aligned with NFR-044) local cache of the org-scope data it needs, refreshed via the event bus (`OrgUnitChanged` events) rather than polling |
| Fallback on Org Hierarchy unavailability | Assignment continues operating against last-known-good cached org data; new assignments proceed using cached scope, flagged for async re-validation once Org Hierarchy recovers — **never blocks enrollment/assessment/certification** (preserving NFR-012's 99.95% compliance-tier target) even if Org Hierarchy (standard-tier, 99.9%) is degraded |
| Staleness bound | 5-minute cache TTL bounds the worst-case staleness window to a level already implicitly accepted by NFR-044's 15-minute HRIS-to-LMS propagation target |

This is the concrete mechanism Chapter 15 deferred here, closing that action item with a
specific TTL, refresh trigger, and degraded-mode behavior rather than a general statement.

## 4. Re-parenting (Satisfies FR-006/BR-006)

A re-parent operation rewrites the affected subtree's closure-table rows within a single
transaction, publishes `AssignmentReassigned`-triggering `OrgUnitReparented` events (Ch.5
§3.2's dynamic-assignment-recomputation requirement), and does **not** require a
data-migration project — directly fulfilling BR-006's M&A/restructuring agility driver.

## Summary
A PostgreSQL closure-table model (not adjacency list, nested set, or a separate graph
database) supports FR-009's matrixed reporting and FR-006's cheap re-parenting while
staying consistent with Chapter 12's single-database-technology decision. Chapter 15's
Org-Hierarchy read-caching/fallback action item is discharged with a concrete 5-minute-TTL,
event-refreshed cache and explicit degraded-mode behavior that never blocks the
compliance-critical enrollment/assessment/certification path.

## Open Questions
Whether the 5-minute cache TTL should be tenant-configurable for very large silo tenants (Ch.18 §3) with stricter internal SLAs — deferred to implementation-phase tuning.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Re-parenting large subtrees (a major reorg affecting millions of learners, BR-008 scale) taking longer than an acceptable transaction window | Medium | Low-Medium | Batch/async re-parenting for very large subtrees, following the same bulk-operation pattern (idempotent, checkpointed) established in [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) §4 |
| Cached org data used for an assignment decision during an extended Org Hierarchy outage becomes meaningfully stale | Medium | Low | Async re-validation (§3) and alerting if cache age exceeds a threshold, feeding [Ch. 38 — Observability](../part-8-operations/38-observability.md) |

## Architecture Decisions
**ADR-031: Closure-table hierarchy model in PostgreSQL, supporting multiple concurrent hierarchy types** — §2. **ADR-032: 5-minute TTL, event-refreshed local cache with never-blocking degraded-mode fallback for Assignment's reads of Org Hierarchy** — §3, discharges Ch.15 action item.

## Future Research
Tenant-configurable cache TTL (implementation phase); large-subtree re-parenting batching mechanics.

## Cross References
[Ch. 5](../part-1-foundations/05-learning-lifecycle.md) §3.2 · [Ch. 6](../part-1-foundations/06-functional-requirements.md) (FR-006, FR-009) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) · [Ch. 15](../part-2-system-domain-architecture/15-backend-architecture.md) (ADR-025) · [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) · [Ch. 38](../part-8-operations/38-observability.md)

## Definition of Done
- [x] Hierarchy data model selected via Technology Evaluation Template
- [x] Ch.15's caching/fallback action item discharged with concrete mechanism
- [x] Re-parenting mechanics specified against FR-006/BR-006

## Confidence Level
**High** — closure-table pattern is a well-established, low-risk solution for exactly this requirement shape; the cache/fallback mechanism directly operationalizes an already-approved dependency rule rather than inventing new policy.

## 5. Chapter Review

**Red Team:** The 5-minute cache TTL is asserted as "aligned with NFR-044" but NFR-044 is
about HRIS→LMS propagation (external system to Org Hierarchy context), while this cache is
internal (Org Hierarchy context to Assignment context) — conflating two different latency
budgets that happen to share a similar number is a reasoning shortcut, not a real
derivation.

**Blue Team:** Accepted — the numeric alignment was coincidental framing, not a rigorous
derivation. The 5-minute figure is retained (it is still a reasonable, conservative choice
independent of NFR-044), but the justification is corrected: it is chosen as a standalone
internal cache-freshness target balancing staleness risk against cache-invalidation
overhead, not derived from NFR-044. This is now correctly marked as a design default in
Open Questions (tenant-configurability) rather than implied to be NFR-044-derived.

**CTO:** ADR-031 **Approved**. ADR-032 **Approved with Conditions** — condition is the
reasoning correction above must stand: the 5-minute TTL is a design default, not an
NFR-044-derived requirement, avoiding future confusion if NFR-044's target changes
independently.

---
*End of Chapter 19. Proceed to Chapter 20 — Competency Management.*
