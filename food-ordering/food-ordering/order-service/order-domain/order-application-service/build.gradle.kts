plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.ksp)
    alias(libs.plugins.micronaut.library)
}

micronaut {
    processing {
        incremental(true)
        annotations("de.aamir.foodordering.order.*")
    }
}

dependencies {
    ksp("io.micronaut.validation:micronaut-validation-processor")
    api(project(":order-service:order-domain:core-domain"))
    // api: the DTOs carry jakarta.validation constraints that adapters see too.
    api("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.data:micronaut-data-tx")
    implementation("org.slf4j:slf4j-api")
    testRuntimeOnly(libs.slf4j.simple)
}
