package de.aamir.foodordering.order.messaging.publisher

import com.google.protobuf.Message
import de.aamir.foodordering.kafka.model.CloudEventHeaders
import de.aamir.foodordering.kafka.producer.KafkaProducer
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ExecutionException

// The outbox row id is the message id, so a re-sent row keeps its ce_id.
internal fun cloudEventHeaders(id: UUID, message: Message, source: String, subject: UUID, time: Instant): Map<String, String> =
    mapOf(
        CloudEventHeaders.ID to id.toString(),
        CloudEventHeaders.TYPE to message.descriptorForType.fullName,
        CloudEventHeaders.SOURCE to source,
        CloudEventHeaders.SUBJECT to subject.toString(),
        CloudEventHeaders.TIME to time.toString(),
        CloudEventHeaders.SCHEMA_VERSION to message.descriptorForType.file.`package`.substringAfterLast('.'),
    )

// Blocks until the broker acks, as the publisher ports require. delivery.timeout.ms bounds the wait.
internal fun KafkaProducer<String, ByteArray>.sendAndAwait(topic: String, key: UUID, message: Message, headers: Map<String, String>) {
    try {
        send(topic, key.toString(), message.toByteArray(), headers).get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}
