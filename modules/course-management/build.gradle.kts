// Ch.11 #7 Course & Content Management — Supporting tier (Ch.10 §3): plain
// CRUD, no event sourcing. Trimmed scope: no SCORM/xAPI import (Ch.22 ADR-035)
// or content versioning (Ch.12 §7) yet — just enough for Enrollment to
// validate a courseId reference against something real.
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(kotlin("test"))
}
