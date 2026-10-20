package de.aamir.foodordering.kafka.producer

import org.apache.kafka.clients.producer.RecordMetadata
import java.util.concurrent.CompletableFuture

interface KafkaProducer<K : Any, V : Any> {

    // Completes once the broker acks the record, or exceptionally with KafkaProducerException.
    // Header values are sent UTF-8 encoded.
    fun send(topic: String, key: K, message: V, headers: Map<String, String> = emptyMap()): CompletableFuture<RecordMetadata>
}
