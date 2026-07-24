package com.elemes.tenancy.infrastructure

import com.elemes.tenancy.IsolationTier
import com.elemes.tenancy.Tenant
import com.elemes.tenancy.TenantStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Repository
class TenantRepository(private val jdbcTemplate: JdbcTemplate) {

    @Transactional
    fun create(tenantId: String, name: String, isolationTier: IsolationTier, region: String): Tenant {
        val now = Instant.now()
        jdbcTemplate.update(
            """
            insert into tenants (tenant_id, name, isolation_tier, region, status, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            tenantId, name, isolationTier.name, region, TenantStatus.PROVISIONING.name, Timestamp.from(now), Timestamp.from(now),
        )
        return Tenant(tenantId, name, isolationTier, region, TenantStatus.PROVISIONING, now, now)
    }

    fun findById(tenantId: String): Tenant? =
        jdbcTemplate.query(
            "select tenant_id, name, isolation_tier, region, status, created_at, updated_at, silo_database from tenants where tenant_id = ?",
            { rs, _ -> mapTenant(rs) },
            tenantId,
        ).firstOrNull()

    fun findAll(): List<Tenant> =
        jdbcTemplate.query(
            "select tenant_id, name, isolation_tier, region, status, created_at, updated_at, silo_database from tenants order by created_at",
            { rs, _ -> mapTenant(rs) },
        )

    fun updateStatus(tenantId: String, status: TenantStatus): Tenant? {
        val now = Instant.now()
        jdbcTemplate.update("update tenants set status = ?, updated_at = ? where tenant_id = ?", status.name, Timestamp.from(now), tenantId)
        return findById(tenantId)
    }

    fun updateSiloDatabase(tenantId: String, siloDatabase: String): Tenant? {
        val now = Instant.now()
        jdbcTemplate.update("update tenants set silo_database = ?, updated_at = ? where tenant_id = ?", siloDatabase, Timestamp.from(now), tenantId)
        return findById(tenantId)
    }

    /** Ch.12 §2 pool-to-silo migration's successful-completion step — flips tier, records the new database, and un-freezes traffic (status back to ACTIVE) in one update. */
    fun completeSiloMigration(tenantId: String, siloDatabase: String): Tenant? {
        val now = Instant.now()
        jdbcTemplate.update(
            "update tenants set isolation_tier = ?, silo_database = ?, status = ?, updated_at = ? where tenant_id = ?",
            IsolationTier.SILO.name, siloDatabase, TenantStatus.ACTIVE.name, Timestamp.from(now), tenantId,
        )
        return findById(tenantId)
    }

    private fun mapTenant(rs: ResultSet) = Tenant(
        tenantId = rs.getString("tenant_id"),
        name = rs.getString("name"),
        isolationTier = IsolationTier.valueOf(rs.getString("isolation_tier")),
        region = rs.getString("region"),
        status = TenantStatus.valueOf(rs.getString("status")),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
        siloDatabase = rs.getString("silo_database"),
    )
}
