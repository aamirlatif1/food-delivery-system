plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.ksp)
    alias(libs.plugins.micronaut.library)
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("de.aamir.foodordering.order.*")
    }
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.validation:micronaut-validation-processor")
    implementation(project(":common:common-application"))
    implementation(project(":order-service:order-domain:order-application-service"))
    implementation("io.micronaut:micronaut-http")
    implementation("io.micronaut:micronaut-http-server")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")
    testImplementation("io.micronaut:micronaut-http-client")
    testRuntimeOnly("io.micronaut:micronaut-http-server-netty")
    testRuntimeOnly(libs.slf4j.simple)
}
