plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.ksp)
    alias(libs.plugins.micronaut.library)
    // MySqlTestDatabase, shared with order-container's tests.
    `java-test-fixtures`
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("de.aamir.foodordering.order.*")
    }
}

dependencies {
    ksp("io.micronaut.data:micronaut-data-processor")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation(project(":order-service:order-domain:order-application-service"))
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    // Order notes, failure messages and outbox payloads are stored as JSON.
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.flywaydb:flyway-mysql")
    // api: MySqlTest extends TestPropertyProvider.
    testFixturesApi("io.micronaut.test:micronaut-test-core")
    testFixturesImplementation("org.testcontainers:testcontainers-mysql")
    testRuntimeOnly(libs.slf4j.simple)
}
