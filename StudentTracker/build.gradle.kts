plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "1.9.0"
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.3"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation(kotlin("test"))

    implementation("com.google.apis:google-api-services-sheets:v4-rev20250509-2.0.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")

    implementation("com.google.api-client:google-api-client:2.7.2")

    implementation("com.google.apis:google-api-services-drive:v3-rev20250511-2.0.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.bootRun {
    systemProperty("spring.devtools.restart.enabled", "true")
    systemProperty("spring.devtools.restart.poll-interval", "2s")
    systemProperty("spring.devtools.restart.quiet-period", "1s")
}