package de.aamir.foodordering.kafka.producer

import de.aamir.foodordering.kafka.config.KafkaProducerConfigData
import io.micronaut.configuration.kafka.config.AbstractKafkaProducerConfiguration
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import org.apache.kafka.clients.producer.ProducerConfig

// Applies kafka-producer-config.* to every Micronaut producer. Settings under
// kafka.producers.<client-id>.* are kept, so a single client can still override a default.
@Singleton
class KafkaProducerConfigApplier(
    private val producerConfigData: KafkaProducerConfigData,
) : BeanCreatedEventListener<AbstractKafkaProducerConfiguration<*, *>> {

    override fun onCreated(
        event: BeanCreatedEvent<AbstractKafkaProducerConfiguration<*, *>>,
    ): AbstractKafkaProducerConfiguration<*, *> =
        event.bean.apply {
            producerConfigData.toProperties().forEach { (name, value) -> config.putIfAbsent(name, value) }
        }
}

internal fun KafkaProducerConfigData.toProperties(): Map<String, String> = mapOf(
    ProducerConfig.ACKS_CONFIG to acks,
    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to enableIdempotence.toString(),
    ProducerConfig.COMPRESSION_TYPE_CONFIG to compressionType,
    ProducerConfig.BATCH_SIZE_CONFIG to batchSize.toString(),
    ProducerConfig.LINGER_MS_CONFIG to linger.toMillis().toString(),
    ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to requestTimeout.toMillis().toString(),
    ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to deliveryTimeout.toMillis().toString(),
    ProducerConfig.RETRIES_CONFIG to retries.toString(),
    ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to maxInFlightRequestsPerConnection.toString(),
)
