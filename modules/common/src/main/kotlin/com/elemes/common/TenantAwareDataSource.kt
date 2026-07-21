package com.elemes.common

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.jdbc.datasource.DelegatingDataSource
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

/**
 * Ch.12 §2: every connection checked out has the Postgres session variable
 * `app.tenant_id` set to [TenantContext]'s current value before the caller
 * ever runs a query on it — this is what every schema's Row-Level Security
 * policies actually filter on. `set_config` (not a string-built `SET`
 * statement) is used specifically so the value goes through a normal JDBC
 * bind parameter rather than SQL string concatenation.
 *
 * Ch.12 §2 silo tier: which *physical* database a connection comes from is
 * also decided here, per checkout, via [SiloRoutingClient] — a SILO
 * tenant's connections are transparently routed to a dedicated, lazily-built
 * connection pool against its own database rather than the shared pooled
 * one `targetDataSource` represents. This is the one place in each service
 * that needs to know about the pooled/silo distinction at all — everything
 * above it (`JdbcTemplate`, repositories, Flyway at startup) is unaware,
 * exactly like RLS's `app.tenant_id` injection already is.
 *
 * `is_local = false` (the third `set_config` argument): the value persists
 * for the whole physical connection, not just one transaction — correct
 * here because this class re-applies it on every single `getConnection()`
 * call, i.e. on every checkout from the pool, so there's no risk of one
 * request's setting leaking into a later checkout that didn't go through it.
 */
class TenantAwareDataSource(
    targetDataSource: DataSource,
    private val siloRoutingClient: SiloRoutingClient,
    /** This service's own schema, extracted once from the pooled datasource's own JDBC URL — reused to build the identically-schemad silo connection string for a SILO tenant. */
    private val ownSchema: String,
) : DelegatingDataSource(targetDataSource) {

    private val siloPools = ConcurrentHashMap<String, DataSource>()

    override fun getConnection(): Connection = applyTenant(resolveDataSource().connection)

    override fun getConnection(username: String, password: String): Connection =
        applyTenant(resolveDataSource().getConnection(username, password))

    private fun resolveDataSource(): DataSource {
        val tenantId = TenantContext.get()
        if (tenantId.isNullOrEmpty() || tenantId == TenantContext.BYPASS) return targetDataSource!!
        val routing = siloRoutingClient.resolve(tenantId)
        val siloUrl = routing.siloDatabaseUrl ?: return targetDataSource!!
        return siloPools.computeIfAbsent(tenantId) { buildSiloPool(tenantId, siloUrl) }
    }

    private fun buildSiloPool(tenantId: String, baseJdbcUrl: String): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "$baseJdbcUrl?currentSchema=$ownSchema"
                username = "elemes_app"
                password = "elemes_app_local_dev"
                maximumPoolSize = 5
                poolName = "silo-$tenantId"
            }
        )

    private fun applyTenant(connection: Connection): Connection {
        connection.prepareStatement("select set_config('app.tenant_id', ?, false)").use { statement ->
            statement.setString(1, TenantContext.get() ?: "")
            statement.execute()
        }
        return connection
    }
}
