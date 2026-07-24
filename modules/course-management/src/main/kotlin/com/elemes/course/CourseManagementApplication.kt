package com.elemes.course

import com.elemes.common.CorsConfig
import com.elemes.common.SiloProvisioningConfig
import com.elemes.common.TenantDataMigrator
import com.elemes.common.TenantDataSourceConfig
import com.elemes.common.TenantMigrationBackfill
import com.elemes.common.TenantMigrationPreDelete
import com.elemes.common.TenantTableCopy
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication
@EnableKafka
@Import(TenantDataSourceConfig::class, SiloProvisioningConfig::class, CorsConfig::class)
class CourseManagementApplication {

    /**
     * Ch.12 §2 pool-to-silo migration's per-service table list. `courses`
     * and `content_versions` form the only genuine FK cycle across all five
     * services' schemas (`courses.current_version_id` -> `content_versions`,
     * `content_versions.course_id` -> `courses`) — resolved by inserting
     * `courses` with `current_version_id` nulled out first, then
     * backfilling it once `content_versions` exists in the target database.
     * `learning_paths`/`path_versions`/`path_steps` have no DB-level FK
     * constraints at all (see each migration's own comments), so their
     * relative order here is for readability only, not correctness.
     */
    @Bean
    fun tenantDataMigrator(): TenantDataMigrator = TenantDataMigrator(
        tables = listOf(
            TenantTableCopy(
                "courses",
                "select course_id, tenant_id, code, title, created_at, null::uuid as current_version_id, org_unit_id from courses where tenant_id = ?",
                "delete from courses where tenant_id = ?",
            ),
            TenantTableCopy("content_versions", "select * from content_versions where tenant_id = ?", "delete from content_versions where tenant_id = ?"),
            TenantTableCopy("learning_paths", "select * from learning_paths where tenant_id = ?", "delete from learning_paths where tenant_id = ?"),
            TenantTableCopy("path_versions", "select * from path_versions where tenant_id = ?", "delete from path_versions where tenant_id = ?"),
            TenantTableCopy("path_steps", "select * from path_steps where tenant_id = ?", "delete from path_steps where tenant_id = ?"),
        ),
        backfill = TenantMigrationBackfill { source, target, tenantId ->
            source.connection.use { sourceConnection ->
                sourceConnection.prepareStatement(
                    "select course_id, current_version_id from courses where tenant_id = ? and current_version_id is not null",
                ).use { statement ->
                    statement.setString(1, tenantId)
                    statement.executeQuery().use { rs ->
                        target.prepareStatement("update courses set current_version_id = ? where course_id = ?").use { update ->
                            var batched = 0
                            while (rs.next()) {
                                update.setObject(1, rs.getObject("current_version_id"))
                                update.setObject(2, rs.getObject("course_id"))
                                update.addBatch()
                                batched++
                            }
                            if (batched > 0) update.executeBatch()
                        }
                    }
                }
            }
        },
        // Mirrors `backfill` above but on the SOURCE side, before purgeSource's
        // reverse-order deletes reach `courses` — courses.current_version_id
        // still points at a content_versions row at that point, and deleting
        // content_versions first (children-before-parents order) would violate
        // that FK unless it's nulled out here first.
        preDelete = TenantMigrationPreDelete { source, tenantId ->
            source.connection.use { connection ->
                connection.prepareStatement("update courses set current_version_id = null where tenant_id = ?").use { statement ->
                    statement.setString(1, tenantId)
                    statement.executeUpdate()
                }
            }
        },
    )
}

fun main(args: Array<String>) {
    runApplication<CourseManagementApplication>(*args)
}
