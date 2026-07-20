package com.elemes.orghierarchy.infrastructure

import com.elemes.orghierarchy.OrgUnit
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class OrgUnitRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val eventPublisher: OrgUnitEventPublisher,
) {

    @Transactional
    fun create(orgUnitId: UUID, tenantId: String, name: String, unitType: String, managerUserId: String?): OrgUnit {
        val now = Instant.now()
        jdbcTemplate.update(
            "insert into org_units (org_unit_id, tenant_id, name, unit_type, manager_user_id, created_at) values (?, ?, ?, ?, ?, ?)",
            orgUnitId, tenantId, name, unitType, managerUserId, Timestamp.from(now),
        )
        // Transactional outbox (Ch.19 §4): commits or rolls back atomically with the insert above.
        eventPublisher.enqueue("OrgUnitChanged", orgUnitId, tenantId, hierarchyType = null)
        return OrgUnit(orgUnitId, tenantId, name, unitType, managerUserId, now)
    }

    fun findById(orgUnitId: UUID): OrgUnit? =
        jdbcTemplate.query(
            "select org_unit_id, tenant_id, name, unit_type, manager_user_id, created_at from org_units where org_unit_id = ?",
            { rs, _ ->
                OrgUnit(
                    orgUnitId = UUID.fromString(rs.getString("org_unit_id")),
                    tenantId = rs.getString("tenant_id"),
                    name = rs.getString("name"),
                    unitType = rs.getString("unit_type"),
                    managerUserId = rs.getString("manager_user_id"),
                    createdAt = rs.getTimestamp("created_at").toInstant(),
                )
            },
            orgUnitId,
        ).firstOrNull()

    /**
     * Ch.19 ADR-031/FR-006: re-parents `orgUnitId`'s whole subtree, under a
     * single `hierarchyType`, within one transaction — no data-migration
     * project. `newParentId == null` detaches the subtree into its own root.
     *
     * Standard closure-table re-parent algorithm:
     *   1. Ensure self rows exist (a unit may never have been assigned a
     *      parent in this hierarchyType before).
     *   2. Delete every row that links an ancestor OUTSIDE the subtree to a
     *      descendant INSIDE it — these are exactly the links that become
     *      stale once the subtree moves.
     *   3. If there's a new parent, insert the cross product of (ancestors of
     *      the new parent, including itself) x (descendants of the moved
     *      unit, including itself), depth-summed — reattaching the whole
     *      subtree under its new ancestors in one INSERT.
     */
    @Transactional
    fun reparent(orgUnitId: UUID, newParentId: UUID?, hierarchyType: String, tenantId: String) {
        ensureSelfRow(orgUnitId, hierarchyType)
        if (newParentId != null) ensureSelfRow(newParentId, hierarchyType)

        jdbcTemplate.update(
            """
            delete from org_closure
            where hierarchy_type = ?
              and descendant_id in (
                  select descendant_id from org_closure where hierarchy_type = ? and ancestor_id = ?
              )
              and ancestor_id not in (
                  select descendant_id from org_closure where hierarchy_type = ? and ancestor_id = ?
              )
            """.trimIndent(),
            hierarchyType, hierarchyType, orgUnitId, hierarchyType, orgUnitId,
        )

        if (newParentId != null) {
            jdbcTemplate.update(
                """
                insert into org_closure (hierarchy_type, ancestor_id, descendant_id, depth)
                select ?, anc.ancestor_id, desc_.descendant_id, anc.depth + desc_.depth + 1
                from org_closure anc, org_closure desc_
                where anc.hierarchy_type = ? and anc.descendant_id = ?
                  and desc_.hierarchy_type = ? and desc_.ancestor_id = ?
                """.trimIndent(),
                hierarchyType, hierarchyType, newParentId, hierarchyType, orgUnitId,
            )
        }

        // Transactional outbox (Ch.19 §4): commits or rolls back atomically with the closure-table rewrite above.
        eventPublisher.enqueue("OrgUnitReparented", orgUnitId, tenantId, hierarchyType)
    }

    private fun ensureSelfRow(orgUnitId: UUID, hierarchyType: String) {
        jdbcTemplate.update(
            "insert into org_closure (hierarchy_type, ancestor_id, descendant_id, depth) values (?, ?, ?, 0) on conflict do nothing",
            hierarchyType, orgUnitId, orgUnitId,
        )
    }

    /** Units this user is the direct manager of — the starting point for resolving their full authority scope (see descendants()). */
    fun findManagedBy(managerUserId: String, tenantId: String): List<OrgUnit> =
        jdbcTemplate.query(
            "select org_unit_id, tenant_id, name, unit_type, manager_user_id, created_at from org_units where manager_user_id = ? and tenant_id = ?",
            { rs, _ -> mapUnit(rs) },
            managerUserId, tenantId,
        )

    /** Fast indexed join, not a recursive query — Ch.19 §2's stated reason for choosing closure table. */
    fun descendants(orgUnitId: UUID, hierarchyType: String): List<OrgUnit> =
        jdbcTemplate.query(
            """
            select ou.org_unit_id, ou.tenant_id, ou.name, ou.unit_type, ou.manager_user_id, ou.created_at
            from org_closure c join org_units ou on ou.org_unit_id = c.descendant_id
            where c.hierarchy_type = ? and c.ancestor_id = ?
            order by c.depth
            """.trimIndent(),
            { rs, _ -> mapUnit(rs) },
            hierarchyType, orgUnitId,
        )

    fun ancestors(orgUnitId: UUID, hierarchyType: String): List<OrgUnit> =
        jdbcTemplate.query(
            """
            select ou.org_unit_id, ou.tenant_id, ou.name, ou.unit_type, ou.manager_user_id, ou.created_at
            from org_closure c join org_units ou on ou.org_unit_id = c.ancestor_id
            where c.hierarchy_type = ? and c.descendant_id = ?
            order by c.depth
            """.trimIndent(),
            { rs, _ -> mapUnit(rs) },
            hierarchyType, orgUnitId,
        )

    private fun mapUnit(rs: java.sql.ResultSet) = OrgUnit(
        orgUnitId = UUID.fromString(rs.getString("org_unit_id")),
        tenantId = rs.getString("tenant_id"),
        name = rs.getString("name"),
        unitType = rs.getString("unit_type"),
        managerUserId = rs.getString("manager_user_id"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
    )
}
