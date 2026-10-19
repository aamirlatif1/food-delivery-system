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
    implementation(project(":order-service:order-domain:order-application-service"))
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
}
