package de.aamir.foodordering.kafka.config

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

// kafka-consumer-config.*; auto-commit is off so offsets are committed only after the handler's
// transaction commits.
@ConfigurationProperties("kafka-consumer-config")
class KafkaConsumerConfigData {
    var autoOffsetReset: String = "earliest"
    var enableAutoCommit: Boolean = false
    var isolationLevel: String = "read_committed"
    var maxPollRecords: Int = 500
    var maxPollInterval: Duration = Duration.ofMinutes(5)
    var sessionTimeout: Duration = Duration.ofSeconds(45)
    var heartbeatInterval: Duration = Duration.ofSeconds(3)
    var maxPartitionFetchBytes: Int = 1_048_576
    var fetchMaxWait: Duration = Duration.ofMillis(500)
}
