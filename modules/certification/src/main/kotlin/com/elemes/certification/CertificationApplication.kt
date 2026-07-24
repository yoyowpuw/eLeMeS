package com.elemes.certification

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

@SpringBootApplication
@EnableKafka
@Import(TenantDataSourceConfig::class, SiloProvisioningConfig::class, CorsConfig::class)
class CertificationApplication {

    /** Ch.12 §2 pool-to-silo migration: no FK constraints between these two tables. `processed_messages` deliberately excluded — see [TenantDataMigrator]'s doc comment. */
    @Bean
    fun tenantDataMigrator(): TenantDataMigrator = TenantDataMigrator(
        tables = listOf(
            TenantTableCopy("certificate_events", "select * from certificate_events where tenant_id = ?", "delete from certificate_events where tenant_id = ?"),
            TenantTableCopy("certificate_projection", "select * from certificate_projection where tenant_id = ?", "delete from certificate_projection where tenant_id = ?"),
        ),
    )
}

fun main(args: Array<String>) {
    runApplication<CertificationApplication>(*args)
}
