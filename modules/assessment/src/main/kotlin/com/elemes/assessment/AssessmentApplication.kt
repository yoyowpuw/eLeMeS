package com.elemes.assessment

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
class AssessmentApplication {

    /** Ch.12 §2 pool-to-silo migration: no FK constraints between these two tables. `outbox` deliberately excluded — see [TenantDataMigrator]'s doc comment. */
    @Bean
    fun tenantDataMigrator(): TenantDataMigrator = TenantDataMigrator(
        tables = listOf(
            TenantTableCopy("assessment_events", "select * from assessment_events where tenant_id = ?", "delete from assessment_events where tenant_id = ?"),
            TenantTableCopy("assessment_projection", "select * from assessment_projection where tenant_id = ?", "delete from assessment_projection where tenant_id = ?"),
        ),
    )
}

fun main(args: Array<String>) {
    runApplication<AssessmentApplication>(*args)
}
