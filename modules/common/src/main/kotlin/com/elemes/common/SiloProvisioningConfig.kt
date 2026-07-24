package com.elemes.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Ch.12 §2 silo tier: opted into by every data-plane service via
 * `@Import(SiloProvisioningConfig::class)`, same mechanism as
 * `TenantDataSourceConfig` — registers `SiloProvisioningController` as a
 * plain bean (Spring MVC's handler mapping discovers `@RequestMapping`
 * methods on any bean in the context, not only component-scanned ones, so
 * this works without `common` being on any service's component-scan base
 * package).
 *
 * Deliberately does NOT provide a default `TenantDataMigrator` bean the way
 * it does for `TenantSiloMigrator` — schema provisioning (Flyway) is
 * identical everywhere, but *which tables, in what order* to copy for
 * pool-to-silo data migration is genuinely per-service knowledge (each
 * service owns a different schema). Every one of the five data-plane
 * services must declare its own `TenantDataMigrator` bean (see each
 * `*Application.kt`) for this config's `siloProvisioningController` bean
 * to wire at all — a missing one is a startup-time failure, not a silent
 * gap.
 */
@Configuration
class SiloProvisioningConfig {

    @Bean
    fun tenantSiloMigrator(): TenantSiloMigrator = TenantSiloMigrator()

    @Bean
    fun siloProvisioningController(
        migrator: TenantSiloMigrator,
        dataMigrator: TenantDataMigrator,
        authorizer: OpaAuthorizer,
        dataSource: DataSource,
        @Value("\${spring.datasource.url}") poolJdbcUrl: String,
    ): SiloProvisioningController = SiloProvisioningController(migrator, dataMigrator, authorizer, dataSource, poolJdbcUrl)
}
