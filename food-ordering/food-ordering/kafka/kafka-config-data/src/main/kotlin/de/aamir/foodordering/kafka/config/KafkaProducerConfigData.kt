package de.aamir.foodordering.kafka.config

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

// kafka-producer-config.*; defaults give the idempotent producer the outbox publisher relies on.
@ConfigurationProperties("kafka-producer-config")
class KafkaProducerConfigData {
    var acks: String = "all"
    var enableIdempotence: Boolean = true
    var compressionType: String = "lz4"
    var batchSize: Int = 16_384
    var linger: Duration = Duration.ofMillis(5)
    var requestTimeout: Duration = Duration.ofSeconds(30)

    // Upper bound on a send including retries; the callback fails after this.
    var deliveryTimeout: Duration = Duration.ofMinutes(2)
    var retries: Int = Int.MAX_VALUE

    // Idempotence keeps ordering per partition for up to 5 in-flight requests.
    var maxInFlightRequestsPerConnection: Int = 5
}
