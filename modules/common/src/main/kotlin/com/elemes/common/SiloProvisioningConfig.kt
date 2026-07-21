package com.elemes.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Ch.12 §2 silo tier: opted into by every data-plane service via
 * `@Import(SiloProvisioningConfig::class)`, same mechanism as
 * `TenantDataSourceConfig` — registers `SiloProvisioningController` as a
 * plain bean (Spring MVC's handler mapping discovers `@RequestMapping`
 * methods on any bean in the context, not only component-scanned ones, so
 * this works without `common` being on any service's component-scan base
 * package).
 */
@Configuration
class SiloProvisioningConfig {

    @Bean
    fun tenantSiloMigrator(): TenantSiloMigrator = TenantSiloMigrator()

    @Bean
    fun siloProvisioningController(
        migrator: TenantSiloMigrator,
        authorizer: OpaAuthorizer,
        @Value("\${spring.datasource.url}") poolJdbcUrl: String,
    ): SiloProvisioningController = SiloProvisioningController(migrator, authorizer, poolJdbcUrl)
}
