package com.elemes.tenancy.infrastructure

import com.elemes.common.ProvisionSiloRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.sql.DriverManager

/**
 * Ch.12 §2 silo tier / Ch.18 §4's "Provision cluster" sequence-diagram
 * step: for a tenant created with `isolationTier = SILO`, this does two
 * things, synchronously, before `TenantController.create()` returns —
 * matching the sequence diagram's ordering (provision *before* activation).
 *
 * 1. Creates the tenant's own dedicated database on the silo Postgres
 *    instance. `CREATE DATABASE` is a genuinely privileged operation no
 *    scoped application role should hold — this connects with the silo
 *    cluster's own superuser credentials for exactly this one statement,
 *    the same bootstrap-vs-runtime separation Vault's AppRole setup
 *    already established (root token for one-time setup, scoped
 *    credentials for everything ongoing). It immediately grants the
 *    ordinary `elemes_app` role CREATE+CONNECT on the new database and
 *    never touches it again with elevated privileges after that.
 * 2. Calls every data-plane service's `POST /internal/silo/provision`
 *    (`SiloProvisioningController`, in `common`) so each one applies its
 *    own schema — relaying the *caller's own* token (token-relay, the same
 *    pattern as every other cross-service call in this codebase), since
 *    this always runs inside a real HTTP request with a real platform-admin
 *    bearer token, never from a Kafka listener.
 *
 * No retry/saga handling if a data-plane service is unreachable mid-loop —
 * this fails loudly (the whole tenant-creation request fails) rather than
 * leaving a tenant half-provisioned silently. Acceptable for this scope,
 * called out in README as the real limitation it is.
 */
@Component
class SiloProvisioner(
    @Value("\${silo.postgres.host}") private val siloHost: String,
    @Value("\${silo.postgres.port}") private val siloPort: Int,
    @Value("\${silo.postgres.admin-username}") private val adminUsername: String,
    @Value("\${silo.postgres.admin-password}") private val adminPassword: String,
    @Value("\${silo.postgres.app-username}") private val appUsername: String,
    @Value("#{'\${silo.data-plane-services}'.split(',')}") private val dataPlaneServiceBaseUrls: List<String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Returns the base JDBC URL (no schema) — persisted as `Tenant.siloDatabase` and, from there, pushed into OPA for every service's [com.elemes.common.SiloRoutingClient] to read. */
    fun provision(tenantId: String, bearerToken: String): String {
        val dbName = "tenant_" + tenantId.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        createDatabase(dbName)
        val siloDatabaseUrl = "jdbc:postgresql://$siloHost:$siloPort/$dbName"

        val restClient = RestClient.create()
        dataPlaneServiceBaseUrls.forEach { baseUrl ->
            log.info("Provisioning silo schema for tenant {} at {}", tenantId, baseUrl)
            restClient.post()
                .uri("$baseUrl/internal/silo/provision")
                .header("Authorization", "Bearer $bearerToken")
                .body(ProvisionSiloRequest(tenantId, siloDatabaseUrl))
                .retrieve()
                .toBodilessEntity()
        }
        return siloDatabaseUrl
    }

    private fun createDatabase(dbName: String) {
        DriverManager.getConnection("jdbc:postgresql://$siloHost:$siloPort/postgres", adminUsername, adminPassword).use { connection ->
            // CREATE DATABASE cannot run inside a transaction block.
            connection.autoCommit = true
            connection.createStatement().use { it.execute("create database \"$dbName\"") }
            connection.createStatement().use { it.execute("grant create, connect on database \"$dbName\" to $appUsername") }
        }
    }
}
