package com.elemes.common

import org.flywaydb.core.Flyway

/**
 * Ch.12 §2 silo tier: runs *this* service's own bundled Flyway migrations
 * (the same `classpath:db/migration` scripts already applied to the pooled
 * cluster at startup — Flyway's default location, unchanged) against a
 * freshly-created per-tenant database on the silo Postgres instance. Every
 * consuming service owns and applies its own schema this way — this class
 * never reaches into another service's migrations, mirroring the same
 * schema-ownership boundary the pooled cluster already has (one schema per
 * service, Ch.15 §8).
 */
class TenantSiloMigrator {
    fun migrate(jdbcUrl: String, username: String, password: String, schema: String) {
        Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .schemas(schema)
            .defaultSchema(schema)
            .createSchemas(true)
            .load()
            .migrate()
    }
}
