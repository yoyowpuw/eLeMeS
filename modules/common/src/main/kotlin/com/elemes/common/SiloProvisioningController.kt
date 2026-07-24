package com.elemes.common

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.sql.DriverManager
import javax.sql.DataSource

data class ProvisionSiloRequest(val tenantId: String, val siloDatabaseUrl: String)

/**
 * Ch.12 §2 silo tier / Ch.18 §4 "Provision cluster" step: tenant-provisioning
 * calls this on every data-plane service, relaying the platform-admin's own
 * token (token-relay, same pattern as every other cross-service call in this
 * codebase), right after creating a SILO tenant's dedicated database. Each
 * service owns and applies its own schema in response — this never runs
 * migrations on another service's behalf. Deliberately re-checks
 * authorization independently (`provision_tenant_silo`, platform-admin
 * only) rather than trusting that tenant-provisioning already checked it —
 * every service in this codebase authorizes its own mutations, never just
 * the caller's.
 */
@RestController
@RequestMapping("/internal/silo")
class SiloProvisioningController(
    private val migrator: TenantSiloMigrator,
    private val dataMigrator: TenantDataMigrator,
    private val authorizer: OpaAuthorizer,
    /** The service's own tenant-aware `DataSource` bean — reading through it (not a raw pooled-only one) is what makes [migrateData] correctly source the migrating tenant's rows from the pooled cluster, exactly like every other query in this service. */
    private val dataSource: DataSource,
    @Value("\${spring.datasource.url}") private val poolJdbcUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/provision")
    fun provision(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: ProvisionSiloRequest): ResponseEntity<Void> {
        authorizer.check(AuthzInput("provision_tenant_silo", jwt.tenantId().value, jwt.roles()))
        val schema = ownSchema()
        val url = "${request.siloDatabaseUrl}?currentSchema=$schema"
        migrator.migrate(url, "elemes_app", "elemes_app_local_dev", schema)
        log.info("Provisioned silo schema '{}' for tenant {} at {}", schema, request.tenantId, request.siloDatabaseUrl)
        return ResponseEntity.ok().build()
    }

    /**
     * Ch.12 §2 pool-to-silo migration's data-copy step — called by
     * `TenantController.migrateToSilo()` after [provision] has already
     * created the target schema. The tenant is already `MIGRATING`
     * (write-frozen) by the time this runs, so [TenantContext] is safe to
     * set for the duration: no concurrent request from this tenant can be
     * touching [dataSource] at the same time.
     */
    @PostMapping("/migrate-data")
    fun migrateData(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: ProvisionSiloRequest): ResponseEntity<Void> {
        authorizer.check(AuthzInput("tenant_migrate", jwt.tenantId().value, jwt.roles()))
        val schema = ownSchema()
        val targetUrl = "${request.siloDatabaseUrl}?currentSchema=$schema"
        TenantContext.set(request.tenantId)
        try {
            DriverManager.getConnection(targetUrl, "elemes_app", "elemes_app_local_dev").use { targetConnection ->
                targetConnection.autoCommit = false
                // Unlike the SOURCE side (routed through the tenant-aware
                // [dataSource], which sets this on every checkout), this
                // connection is a plain one-shot JDBC connection with no RLS
                // context of its own — every target table's RLS policy
                // requires `app.tenant_id` to match the row being inserted,
                // so without this every single insert below is rejected.
                targetConnection.prepareStatement("select set_config('app.tenant_id', ?, false)").use { statement ->
                    statement.setString(1, request.tenantId)
                    statement.execute()
                }
                try {
                    dataMigrator.migrate(dataSource, targetConnection, request.tenantId)
                    targetConnection.commit()
                } catch (ex: Exception) {
                    targetConnection.rollback()
                    throw ex
                }
            }
            // Only reached once the silo-side copy is durably committed —
            // safe to remove the pooled-side originals now.
            dataMigrator.purgeSource(dataSource, request.tenantId)
        } finally {
            TenantContext.clear()
        }
        log.info("Copied tenant {} data into silo database at {} and purged the pooled originals", request.tenantId, request.siloDatabaseUrl)
        return ResponseEntity.ok().build()
    }

    /** Same schema this service already uses on the pooled cluster (`?currentSchema=X` in `spring.datasource.url`) — every silo tenant's database gets an identically-named schema, just in a different physical database. */
    private fun ownSchema(): String =
        Regex("currentSchema=([^&]+)").find(poolJdbcUrl)?.groupValues?.get(1)
            ?: error("spring.datasource.url is missing ?currentSchema=... — cannot determine which schema to provision")
}
