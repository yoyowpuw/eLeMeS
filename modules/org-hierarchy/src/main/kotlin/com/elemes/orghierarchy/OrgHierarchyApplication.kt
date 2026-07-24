package com.elemes.orghierarchy

import com.elemes.common.CorsConfig
import com.elemes.common.SiloProvisioningConfig
import com.elemes.common.TenantDataMigrator
import com.elemes.common.TenantDataSourceConfig
import com.elemes.common.TenantTableCopy
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@Import(TenantDataSourceConfig::class, SiloProvisioningConfig::class, CorsConfig::class)
class OrgHierarchyApplication {

    /**
     * Ch.12 §2 pool-to-silo migration: `org_closure` has no `tenant_id`
     * column of its own (deliberate, per its own migration's comment — pure
     * topology, protected transitively) — filtered instead via a semi-join
     * against the already-tenant-scoped `org_units` rows. `org_units` must
     * be copied first since the target database's own FK constraints on
     * `org_closure.ancestor_id`/`descendant_id` require it. `outbox`
     * deliberately excluded — see [TenantDataMigrator]'s doc comment.
     */
    @Bean
    fun tenantDataMigrator(): TenantDataMigrator = TenantDataMigrator(
        tables = listOf(
            TenantTableCopy("org_units", "select * from org_units where tenant_id = ?", "delete from org_units where tenant_id = ?"),
            TenantTableCopy(
                "org_closure",
                "select * from org_closure where ancestor_id in (select org_unit_id from org_units where tenant_id = ?)",
                "delete from org_closure where ancestor_id in (select org_unit_id from org_units where tenant_id = ?)",
            ),
        ),
    )
}

fun main(args: Array<String>) {
    runApplication<OrgHierarchyApplication>(*args)
}
