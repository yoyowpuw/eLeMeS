package com.elemes.orghierarchy

import java.time.Instant
import java.util.UUID

data class OrgUnit(
    val orgUnitId: UUID,
    val tenantId: String,
    val name: String,
    val unitType: String,
    /** Optional — the user managing this unit, e.g. Keycloak username. Not a full membership model. */
    val managerUserId: String?,
    val createdAt: Instant,
)

/**
 * One row per (hierarchyType, ancestor, descendant) pair, including a
 * depth-0 self row for every unit. Ch.19 ADR-031: supporting several
 * independent hierarchyType values over the same OrgUnit rows is what makes
 * matrixed/dual-manager reporting (FR-009) possible without a second
 * database technology — a unit can have a different parent under
 * "reporting-line" than under "cost-center".
 */
data class OrgClosureEntry(
    val hierarchyType: String,
    val ancestorId: UUID,
    val descendantId: UUID,
    val depth: Int,
)
