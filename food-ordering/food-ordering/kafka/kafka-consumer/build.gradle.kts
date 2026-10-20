plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.ksp)
    alias(libs.plugins.micronaut.library)
}

micronaut {
    processing {
        incremental(true)
        annotations("de.aamir.foodordering.kafka.*")
    }
}

dependencies {
    api(project(":kafka:kafka-config-data"))
    api(project(":kafka:kafka-model"))
    // api: the public types use Kafka client classes, and listeners use Micronaut Kafka annotations.
    api("io.micronaut.kafka:micronaut-kafka")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("org.slf4j:slf4j-api")
    testRuntimeOnly(libs.slf4j.simple)
}
