package de.aamir.foodordering.order.messaging.listener

import com.google.protobuf.InvalidProtocolBufferException
import de.aamir.foodordering.order.messaging.mapper.InvalidMessageException
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.sql.SQLRecoverableException
import java.sql.SQLTransientException

const val CONSUMER_GROUP_ID = "\${order-service.messaging.consumer-group-id:order-service}"

// Fixed names (REQUIREMENTS §5.1): the retry topic and DLQ are derived from them.
const val PAYMENT_RESPONSE_TOPIC = "payment-response-topic"
const val RESTAURANT_APPROVAL_RESPONSE_TOPIC = "restaurant-approval-response-topic"

// <topic>.retry and <topic>.dlq exist for every topic (infrastructure/kafka/create-topics.sh).
// Micronaut uses one retry topic per attempt, so a single .retry topic means one delayed retry.
const val RETRY_TOPIC_SUFFIX = ".retry"
const val DLQ_TOPIC_SUFFIX = ".dlq"
const val RETRY_DELAY = "10s"

// The only exception the listeners' error strategy retries; everything else goes straight to the
// DLQ. Micronaut matches the exact class, so this must stay final.
class RetryableMessageException(cause: Throwable) : RuntimeException(cause.message, cause)

// An empty or undecodable record can never succeed: InvalidMessageException sends it to the DLQ.
internal inline fun <T : Any> ConsumerRecord<String, ByteArray>.decode(decode: (ByteArray) -> T): T {
    val bytes: ByteArray = value() ?: throw InvalidMessageException("Empty record with key ${key()}")
    return try {
        decode(bytes)
    } catch (e: InvalidProtocolBufferException) {
        throw InvalidMessageException("Undecodable record with key ${key()}", e)
    }
}

// Deadlocks, lock wait timeouts and lost connections may succeed later (§6.4); invariant
// violations and other errors will not.
internal inline fun retryOnTransientFailure(handle: () -> Unit) {
    try {
        handle()
    } catch (e: Exception) {
        throw if (e.isTransient()) RetryableMessageException(e) else e
    }
}

internal fun Throwable.isTransient(): Boolean =
    generateSequence(this) { it.cause }.any { it is SQLTransientException || it is SQLRecoverableException }
