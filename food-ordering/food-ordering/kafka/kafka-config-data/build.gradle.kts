// Typed Kafka settings (kafka-config.*, kafka-producer-config.*, kafka-consumer-config.*)
// shared by producer and consumer.
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
    implementation("io.micronaut.kafka:micronaut-kafka")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
}
