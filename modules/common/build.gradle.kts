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

    testImplementation(kotlin("test"))
}
