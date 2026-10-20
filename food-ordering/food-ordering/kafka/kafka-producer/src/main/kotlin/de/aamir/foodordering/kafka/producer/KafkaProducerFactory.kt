package de.aamir.foodordering.kafka.producer

import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import org.apache.kafka.clients.producer.Producer

@Factory
class KafkaProducerFactory {

    // String keys (orderId, customerId, restaurantId) and Protobuf-encoded payloads, per REQUIREMENTS.md §5.
    @Singleton
    fun kafkaProducer(@KafkaClient producer: Producer<String, ByteArray>): KafkaProducer<String, ByteArray> =
        KafkaProducerImpl(producer)
}
