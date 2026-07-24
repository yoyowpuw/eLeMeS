package com.elemes.common

import java.sql.Connection
import javax.sql.DataSource

/**
 * `selectSql` must be `select * from <table> where ...` (or an explicit
 * column list, if a column needs overriding — see [TenantDataMigrator]'s
 * course-management cycle example) with exactly one `?` bound to the
 * migrating tenant's id. `deleteSql` must delete exactly the same rows
 * `selectSql` would have returned, with the same single `?` binding — used
 * by [TenantDataMigrator.purgeSource] to remove the pooled-side originals
 * once they're safely committed on the silo side, so a "migration" is an
 * actual move, not a permanent duplicate left behind in the shared
 * cluster.
 */
data class TenantTableCopy(val table: String, val selectSql: String, val deleteSql: String)

fun interface TenantMigrationBackfill {
    fun run(source: DataSource, target: Connection, tenantId: String)
}

/** Mirrors [TenantMigrationBackfill] but runs against the SOURCE only, before [TenantDataMigrator.purgeSource] starts deleting — the one place a cycle-breaking column override (nulled on the way in) needs breaking again on the way out, before its old value's row can be deleted. */
fun interface TenantMigrationPreDelete {
    fun run(source: DataSource, tenantId: String)
}

private val NO_BACKFILL = TenantMigrationBackfill { _, _, _ -> }
private val NO_PRE_DELETE = TenantMigrationPreDelete { _, _ -> }

/**
 * Ch.12 §2 pool-to-silo migration: copies one tenant's rows, table by
 * table in the order given (parent tables before any child table that
 * references them — this class has no FK introspection of its own, each
 * service's own bean wiring is responsible for ordering, since only five
 * small, static schemas exist and a runtime FK-graph solver would be
 * solving a problem that doesn't actually recur), from the pooled cluster
 * into a tenant's freshly-provisioned silo database. Column names/types
 * are read off each `selectSql`'s own `ResultSetMetaData` rather than
 * hardcoded, so this stays correct if a service's own migration adds a
 * column later. Runs as one transaction on the target connection — a
 * partial copy never commits.
 *
 * Deliberately excludes `outbox`/`processed_messages`-style tables from
 * every service's own table list: outbox rows are transient working-queue
 * state that `OutboxPoller` drains within seconds under normal operation,
 * and the tenant is write-frozen for the whole migration (see
 * `TenantStatus.MIGRATING`), so no new ones can appear mid-copy;
 * `processed_messages` is a Kafka consumer-side dedup log whose absence in
 * the new database only matters if Kafka ever redelivers an
 * already-processed message post-migration, which would at worst
 * reprocess it once — an accepted limitation, not silently unsafe, in the
 * same spirit as `SiloProvisioner`'s own documented "no retry/saga" gap.
 */
class TenantDataMigrator(
    private val tables: List<TenantTableCopy>,
    private val backfill: TenantMigrationBackfill = NO_BACKFILL,
    private val preDelete: TenantMigrationPreDelete = NO_PRE_DELETE,
) {
    fun migrate(source: DataSource, target: Connection, tenantId: String) {
        tables.forEach { copyTable(source, target, it, tenantId) }
        backfill.run(source, target, tenantId)
    }

    /**
     * Called only after [migrate]'s target copy has already committed
     * successfully. Deletes in reverse of [tables]' order — the same
     * dependency ordering [migrate] needed to insert parents before
     * children means deleting must go the other way, children before
     * parents, to avoid violating the SOURCE database's own FK
     * constraints. If this step itself fails partway, the tenant is still
     * fully and correctly served from the new silo database (already
     * committed) — a harmless leftover duplicate in the pooled cluster,
     * not data loss or corruption.
     */
    fun purgeSource(source: DataSource, tenantId: String) {
        preDelete.run(source, tenantId)
        source.connection.use { connection ->
            connection.autoCommit = false
            try {
                tables.reversed().forEach { copy ->
                    connection.prepareStatement(copy.deleteSql).use { statement ->
                        statement.setString(1, tenantId)
                        statement.executeUpdate()
                    }
                }
                connection.commit()
            } catch (ex: Exception) {
                connection.rollback()
                throw ex
            }
        }
    }

    private fun copyTable(source: DataSource, target: Connection, copy: TenantTableCopy, tenantId: String) {
        source.connection.use { sourceConn ->
            sourceConn.prepareStatement(copy.selectSql).use { statement ->
                statement.setString(1, tenantId)
                statement.executeQuery().use { rs ->
                    val columnCount = rs.metaData.columnCount
                    val columnNames = (1..columnCount).map { rs.metaData.getColumnName(it) }
                    val insertSql = "insert into ${copy.table} (${columnNames.joinToString(",")}) values (${columnNames.joinToString(",") { "?" }})"
                    target.prepareStatement(insertSql).use { insertStatement ->
                        var batched = 0
                        while (rs.next()) {
                            for (i in 1..columnCount) insertStatement.setObject(i, rs.getObject(i))
                            insertStatement.addBatch()
                            batched++
                        }
                        if (batched > 0) insertStatement.executeBatch()
                    }
                }
            }
        }
    }
}
