// Ch.18 Multi-tenancy — Tenancy & Provisioning bounded context (Ch.11 #3):
// the control plane. Owns the tenant registry and provisioning/offboarding
// lifecycle. For a SILO tenant (Ch.12 §2), SiloProvisioner also creates the
// dedicated per-tenant database on the silo Postgres instance (a genuinely
// privileged, one-time bootstrap action — see its own doc comment) and
// triggers each data-plane service's own schema migration against it —
// this service still never runs another service's migrations itself.
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
