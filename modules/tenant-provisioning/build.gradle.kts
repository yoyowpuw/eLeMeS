// Ch.18 Multi-tenancy — Tenancy & Provisioning bounded context (Ch.11 #3):
// the control plane. Owns the tenant registry and provisioning/offboarding
// lifecycle; the actual silo/pooled database clusters (data plane) are
// Ch.12's concern, provisioned per-tenant elsewhere — this service only
// tracks which tier a tenant is on, it doesn't stand up infrastructure.
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":modules:common"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
}
