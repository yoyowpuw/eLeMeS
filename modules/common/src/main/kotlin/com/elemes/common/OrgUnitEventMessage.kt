package com.elemes.common

import java.time.Instant
import java.util.UUID

/**
 * Ch.19 §3/§4 Published Language: org-hierarchy's wire contract for
 * "something about this org unit changed". "OrgUnitChanged" covers creation
 * (and would cover any future edit); "OrgUnitReparented" covers the
 * closure-table-rewriting move specifically — Ch.19 §4 ties it to
 * `AssignmentReassigned`-triggering recomputation, which doesn't exist as a
 * concept in this codebase yet, so today it's consumed only for cache
 * invalidation (§3's ADR-032 pattern).
 */
data class OrgUnitEventMessage(
    val eventType: String,
    val orgUnitId: UUID,
    val tenantId: String,
    /** Set only for OrgUnitReparented — which hierarchy type moved. */
    val hierarchyType: String?,
    val occurredAt: Instant,
)

object OrgUnitEventTopics {
    const val ORG_UNIT_EVENTS = "org-unit-events"
}
