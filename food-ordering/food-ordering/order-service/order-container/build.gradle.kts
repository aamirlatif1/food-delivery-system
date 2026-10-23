plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.ksp)
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.micronaut.aot)
    alias(libs.plugins.shadow)
}

dependencies {
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation(project(":order-service:order-domain:order-application-service"))
    implementation(project(":order-service:order-application"))
    implementation(project(":order-service:order-messaging"))
    implementation(project(":order-service:order-datasource"))
    implementation(project(":order-service:order-ai"))
    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("tools.jackson.module:jackson-module-kotlin")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation(libs.konsist)
    testImplementation(testFixtures(project(":order-service:order-datasource")))
}

application {
    mainClass = "de.aamir.foodordering.order.ApplicationKt"
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("de.aamir.foodordering.order.*")
    }
    aot {
        optimizeClassLoading = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
    baseImage = "eclipse-temurin:25-jre"
    // micronaut.server.port in application.properties
    exposedPorts = listOf(8181)
}

// ./gradlew :order-service:order-container:dockerBuild, then docker compose up in infrastructure/.
tasks.named<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("dockerBuild") {
    images = listOf("food-delivery/order-service:${project.version}", "food-delivery/order-service:latest")
}
