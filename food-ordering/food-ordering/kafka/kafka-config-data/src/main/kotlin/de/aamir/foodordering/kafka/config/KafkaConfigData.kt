package de.aamir.foodordering.kafka.config

import io.micronaut.context.annotation.ConfigurationProperties

// kafka-config.*; the single source of the broker list for every Micronaut Kafka client.
// Micronaut Kafka itself stays off until a kafka.* property is set, so services also set kafka.enabled=true.
@ConfigurationProperties("kafka-config")
class KafkaConfigData {
    // Host ports of the local brokers in infrastructure/docker-compose.yml.
    var bootstrapServers: String = "localhost:9092,localhost:9094,localhost:9096"
}
