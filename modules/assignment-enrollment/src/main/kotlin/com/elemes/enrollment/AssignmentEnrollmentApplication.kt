package com.elemes.enrollment

import com.elemes.common.CorsConfig
import com.elemes.common.SiloProvisioningConfig
import com.elemes.common.TenantDataMigrator
import com.elemes.common.TenantDataSourceConfig
import com.elemes.common.TenantTableCopy
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableKafka
@EnableScheduling
@Import(TenantDataSourceConfig::class, SiloProvisioningConfig::class, CorsConfig::class)
class AssignmentEnrollmentApplication {

    /** Ch.12 §2 pool-to-silo migration: no FK constraints between any of this schema's tables, so order is arbitrary. `outbox`/`processed_messages` deliberately excluded — see [TenantDataMigrator]'s doc comment. */
    @Bean
    fun tenantDataMigrator(): TenantDataMigrator = TenantDataMigrator(
        tables = listOf(
            TenantTableCopy("enrollment_events", "select * from enrollment_events where tenant_id = ?", "delete from enrollment_events where tenant_id = ?"),
            TenantTableCopy("enrollment_projection", "select * from enrollment_projection where tenant_id = ?", "delete from enrollment_projection where tenant_id = ?"),
            TenantTableCopy("path_progress", "select * from path_progress where tenant_id = ?", "delete from path_progress where tenant_id = ?"),
        ),
    )
}

fun main(args: Array<String>) {
    runApplication<AssignmentEnrollmentApplication>(*args)
}
