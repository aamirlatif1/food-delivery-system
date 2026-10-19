// Pure Kotlin: no Micronaut, no jakarta. Enforced by ArchitectureTest in order-container.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":common:common-domain"))
}
