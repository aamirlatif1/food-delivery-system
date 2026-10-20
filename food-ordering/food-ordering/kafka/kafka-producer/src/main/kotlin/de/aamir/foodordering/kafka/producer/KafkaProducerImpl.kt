package de.aamir.foodordering.kafka.producer

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

// Wraps a Micronaut-managed Producer; Micronaut closes it on shutdown.
class KafkaProducerImpl<K : Any, V : Any>(
    private val producer: Producer<K, V>,
) : KafkaProducer<K, V> {

    override fun send(topic: String, key: K, message: V, headers: Map<String, String>): CompletableFuture<RecordMetadata> {
        log.info("Sending message with key {} to topic {}", key, topic)
        val recordHeaders = headers.map { (name, value) -> RecordHeader(name, value.toByteArray()) }
        val record = ProducerRecord(topic, null, key, message, recordHeaders)
        val result = CompletableFuture<RecordMetadata>()
        try {
            producer.send(record) { metadata, exception ->
                if (exception == null) {
                    log.info(
                        "Message with key {} sent to topic {}, partition {}, offset {}",
                        key, metadata.topic(), metadata.partition(), metadata.offset(),
                    )
                    result.complete(metadata)
                } else {
                    log.error("Error sending message with key {} to topic {}", key, topic, exception)
                    result.completeExceptionally(
                        KafkaProducerException("Error sending message with key $key to topic $topic", exception),
                    )
                }
            }
        } catch (e: KafkaException) {
            // send() itself throws on serialization failures or when the buffer stays full past max.block.ms.
            log.error("Error sending message with key {} to topic {}", key, topic, e)
            result.completeExceptionally(KafkaProducerException("Error sending message with key $key to topic $topic", e))
        }
        return result
    }

    private companion object {
        private val log = LoggerFactory.getLogger(KafkaProducerImpl::class.java)
    }
}
