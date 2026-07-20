package com.elemes.common

import org.springframework.jdbc.datasource.DelegatingDataSource
import java.sql.Connection
import javax.sql.DataSource

/**
 * Ch.12 §2: every connection checked out from the pool has the Postgres
 * session variable `app.tenant_id` set to [TenantContext]'s current value
 * before the caller ever runs a query on it — this is what pooled clusters'
 * Row-Level Security policies (see each service's RLS migration) actually
 * filter on. `set_config` (not a string-built `SET` statement) is used
 * specifically so the value goes through a normal JDBC bind parameter
 * rather than SQL string concatenation.
 *
 * `is_local = false` (the third `set_config` argument): the value persists
 * for the whole physical connection, not just one transaction — correct
 * here because [TenantAwareDataSource] already re-applies it on every
 * single `getConnection()` call, i.e. on every checkout from the pool, so
 * there's no risk of one request's setting leaking into a later checkout
 * that didn't go through this class.
 */
class TenantAwareDataSource(targetDataSource: DataSource) : DelegatingDataSource(targetDataSource) {

    override fun getConnection(): Connection = applyTenant(super.getConnection())

    override fun getConnection(username: String, password: String): Connection =
        applyTenant(super.getConnection(username, password))

    private fun applyTenant(connection: Connection): Connection {
        connection.prepareStatement("select set_config('app.tenant_id', ?, false)").use { statement ->
            statement.setString(1, TenantContext.get() ?: "")
            statement.execute()
        }
        return connection
    }
}
