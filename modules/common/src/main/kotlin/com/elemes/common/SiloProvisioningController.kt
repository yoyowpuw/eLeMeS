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
    private val authorizer: OpaAuthorizer,
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

    /** Same schema this service already uses on the pooled cluster (`?currentSchema=X` in `spring.datasource.url`) — every silo tenant's database gets an identically-named schema, just in a different physical database. */
    private fun ownSchema(): String =
        Regex("currentSchema=([^&]+)").find(poolJdbcUrl)?.groupValues?.get(1)
            ?: error("spring.datasource.url is missing ?currentSchema=... — cannot determine which schema to provision")
}
