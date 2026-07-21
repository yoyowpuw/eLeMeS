plugins {
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
    }
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework:spring-web")
    // Ch.12 §2 silo tier: TenantSiloMigrator runs each consuming service's own
    // bundled Flyway migrations against a dynamically-created per-tenant
    // database — the Postgres JDBC driver itself is provided at runtime by
    // whichever service depends on this module (all of them already declare
    // it), so it's deliberately not redeclared here.
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    // For TenantAwareDataSource's lazily-built per-silo-tenant connection pool.
    implementation("com.zaxxer:HikariCP")
    // Servlet API only: needed for TenantContextFilter's HttpServletRequest/Response
    // types. Provided at runtime by each consuming service's embedded Tomcat.
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation(kotlin("test"))
}
