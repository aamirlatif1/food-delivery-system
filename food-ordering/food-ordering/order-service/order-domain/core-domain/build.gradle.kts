// Pure Kotlin: no Micronaut, no jakarta. Enforced by ArchitectureTest in order-container.
// Logging goes through the SLF4J API only; the container supplies Logback at runtime.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":common:common-domain"))
    implementation(libs.slf4j.api)
    testRuntimeOnly(libs.slf4j.simple)
}
