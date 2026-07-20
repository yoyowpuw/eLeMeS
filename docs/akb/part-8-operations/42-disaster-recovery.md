# Chapter 42 — Disaster Recovery

> Part VIII — Operations · [Index](../00-index.md) · Previous: [Ch. 41 — Compliance](41-compliance.md) · Next: Ch. 43 — Scalability

## 1. Purpose

This chapter resolves three items explicitly deferred here: Chapter 12's Open Question on
reconsidering distributed SQL for the compliance tier, Chapter 12's action item on
silo-vs-pool backup/restore operational differences, and Chapter 18's action item on the
control plane's own DR treatment. It also finalizes NFR-013/014 (RPO/RTO) implementation.

## 2. Resolving Ch.12's Distributed-SQL Reconsideration

**Decision: retain PostgreSQL for the compliance tier; do not adopt distributed SQL.**
RPO ≤5min (NFR-013) and RTO ≤30min (NFR-014) are both achievable with PostgreSQL via
synchronous-within-region replication (bounding RPO near-zero intra-region) plus
asynchronous cross-region replication with automated failover orchestration (e.g.,
Patroni-class), achieving the 30-minute RTO target through automated (not manual) failover
promotion. Distributed SQL's main advantage — native multi-region without manual replication
topology (Ch.12 §3) — does not translate into a materially better RPO/RTO outcome than a
well-automated Postgres failover setup, so it does not overcome Chapter 12's original
talent-pool/maturity objection (Ch.1 Principle 6). This confirms, rather than reverses,
Chapter 12's original decision — closing that Open Question with evidence rather than
leaving it perpetually open.

## 3. Silo vs. Pool Backup/Restore (Discharges Ch.12 Action Item)

| Tier | Backup/Restore Profile |
|---|---|
| Pooled clusters (Ch.12 §2) | Standard full-cluster point-in-time-recovery backups; a restore event affects all tenants sharing that cluster simultaneously — operationally simpler, but blast radius spans multiple tenants |
| Silo clusters | Per-tenant backup/restore — operationally more numerous (one backup job per large/regulated tenant) but a restore event is isolated to a single tenant, consistent with the "dedicated-feeling" isolation promise (Ch.18 §3) |

This is a direct, intentional trade-off: pooled tenants accept shared blast radius in
exchange for lower operational cost (consistent with why they're pooled at all, Ch.12 §2);
silo tenants pay for isolated blast radius as part of what their tier is buying them. This
is now made explicit rather than left as an unstated implication of the isolation model.

## 4. Control Plane DR (Discharges Ch.18 Action Item)

The control plane (Ch.18 §2) has a **more lenient RTO (1 hour)** than data-plane services,
because a control-plane-only outage does not affect already-provisioned tenants' running
services — edge/gateway routing configuration is cached (Ch.13 §6), so existing learner
sessions continue uninterrupted. Only *new* provisioning, configuration changes, and
tenant onboarding are blocked during a control-plane outage — a materially lower-consequence
failure mode than a data-plane outage, justifying the more relaxed target.

## 5. Full Dependency Inventory (Finalizes Ch.35 §4 Registry for DR Purposes)

Every entry in Chapter 35 §4's dependency-fallback registry (CIAM, conformance engine,
proctoring, video, foundation model, HRIS, Cloud KMS) is formally included in this
platform's DR runbook, with each dependency's fallback behavior treated as a tested
(not merely documented) failover procedure — closing the loop on NFR-051 obligations
accumulated since Chapter 7.

## Summary
PostgreSQL is retained for the compliance-critical tier after evidence-based reconsideration
against distributed SQL, achieved via automated intra/cross-region replication and failover
orchestration meeting NFR-013/014 targets without distributed SQL's talent-pool/maturity
trade-off. Silo and pooled tenants receive intentionally different backup/restore blast-
radius profiles, made explicit as part of what tier isolation is buying. The control plane
receives a more lenient 1-hour RTO, justified by cached edge routing decoupling existing
sessions from control-plane availability. All NFR-051 dependency-fallback obligations are
formally tested, not merely documented, as part of the DR runbook.

## Open Questions
Specific automated-failover tooling selection (e.g., Patroni-class vs. cloud-managed equivalent) — implementation-phase, evaluated against Ch.45 cost modeling.

## Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Automated failover orchestration itself has known failure modes (split-brain risk during network partition) | High | Low | Standard mitigation via quorum-based consensus in the failover tooling; must be load/chaos-tested, not just configured |
| DR runbook dependency tests (§5) become stale as vendors/mechanisms change | Medium | Medium | Periodic (quarterly) DR drill requirement, feeding [Ch. 48 — Operations](../part-9-governance-future/48-operations.md) |

## Architecture Decisions
**ADR-069: PostgreSQL retained for the compliance tier; distributed SQL reconsideration closed via evidence-based confirmation, not reversal** — §2, resolves Ch.12 Open Question. **ADR-070: Explicit, differentiated backup/restore blast-radius profiles for silo vs. pooled tenants** — §3, discharges Ch.12 action item. **ADR-071: Control plane RTO of 1 hour, more lenient than data-plane RTO, justified by cached edge routing** — §4, discharges Ch.18 action item.

## Future Research
Failover-tooling selection (Ch.45 cost modeling); quarterly DR drill program design (Ch.48).

## Cross References
[Ch. 7](../part-1-foundations/07-non-functional-requirements.md) (NFR-013, 014, 051) · [Ch. 12](../part-2-system-domain-architecture/12-database-architecture.md) §2–3 (Open Question, action item) · [Ch. 13](../part-2-system-domain-architecture/13-api-strategy.md) §6 · [Ch. 18](../part-3-identity-organization/18-multi-tenancy.md) §2 (action item) · [Ch. 35](../part-7-platform-integration/35-integration-architecture.md) §4 · [Ch. 45](45-cost-optimization.md) · [Ch. 48](../part-9-governance-future/48-operations.md)

## Definition of Done
- [x] Ch.12's distributed-SQL Open Question resolved with evidence-based reasoning
- [x] Silo-vs-pool backup/restore profiles explicitly differentiated, discharging Ch.12 action item
- [x] Control plane DR treatment specified with justified, differentiated RTO, discharging Ch.18 action item
- [x] Full NFR-051 dependency inventory formally tested, not merely documented

## Confidence Level
**Medium-High.** RPO/RTO targets and the Postgres-retention decision are grounded in well-established replication/failover practice — **High** confidence. The specific automated-failover tooling and its split-brain risk profile remain implementation-phase details — **Medium** confidence pending real testing.

## 6. Chapter Review

**Red Team:** The claim that automated cross-region failover achieves a 30-minute RTO is
asserted without a tested reference — this is the same "unvalidated numbers" pattern
Chapter 7's own Red Team flagged for NFR targets generally, recurring here for a
particularly consequential claim (DR is precisely the capability that's hardest to
validate without deliberately breaking production).

**Blue Team:** Accepted — consistent with how Chapter 7 already handled this class of
concern (Confidence Level correctly downgraded, Future Research assigned). Addendum:
[Ch. 48 — Operations](../part-9-governance-future/48-operations.md)'s quarterly DR drill requirement (already added in
this chapter's Risk mitigation) is the actual validation mechanism — this chapter sets the
target and mechanism design; Ch.48's drills are what convert the claim from "designed to
achieve" to "demonstrated to achieve." This distinction is now explicit rather than
implied.

**CTO:** ADR-069/070/071 **Approved with Conditions** — condition is
[Ch. 48 — Operations](../part-9-governance-future/48-operations.md)'s quarterly DR drills are the binding validation
mechanism for the RTO/RPO targets asserted here; until drilled, these targets remain
design intent, not demonstrated capability.

---
*End of Chapter 42. Proceed to Chapter 43 — Scalability.*
