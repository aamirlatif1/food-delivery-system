package de.aamir.foodordering.order.messaging.listener

import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import de.aamir.foodordering.kafka.consumer.KafkaConsumer
import de.aamir.foodordering.kafka.model.payment.v1.PaymentResponse as PaymentResponseProto
import de.aamir.foodordering.order.application.ports.input.message.listener.PaymentResponseMessageListener
import de.aamir.foodordering.order.messaging.mapper.OrderMessagingDataMapper
import io.micronaut.configuration.kafka.annotation.ErrorStrategy
import io.micronaut.configuration.kafka.annotation.ErrorStrategyValue
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

// Also consumes payment-response-topic.retry. A transient failure moves the record there, to be
// handled again after RETRY_DELAY; a second failure, or any other error, moves it to
// payment-response-topic.dlq. Either way the partition keeps moving (drill D3). Redelivered
// records are skipped by the saga's outbox lookup.
@KafkaListener(
    groupId = CONSUMER_GROUP_ID,
    offsetStrategy = OffsetStrategy.SYNC_PER_RECORD,
    errorStrategy = ErrorStrategy(
        value = ErrorStrategyValue.RETRY_TOPIC_ON_ERROR,
        retryTopicSuffixes = [RETRY_TOPIC_SUFFIX],
        retryTopicDelays = [RETRY_DELAY],
        exceptionTypes = [RetryableMessageException::class],
        dlq = PAYMENT_RESPONSE_TOPIC + DLQ_TOPIC_SUFFIX,
    ),
)
class PaymentResponseKafkaListener(
    private val listener: PaymentResponseMessageListener,
    private val mapper: OrderMessagingDataMapper,
) : KafkaConsumer<String, ByteArray> {

    @Topic(PAYMENT_RESPONSE_TOPIC)
    override fun receive(record: ConsumerRecord<String, ByteArray>) {
        val response = record.decode { mapper.toPaymentResponse(PaymentResponseProto.parseFrom(it)) }
        log.info("Received {} payment response for order {} from {}", response.paymentStatus, response.orderId, record.topic())
        retryOnTransientFailure {
            when (response.paymentStatus) {
                PaymentStatus.COMPLETED -> listener.paymentCompleted(response)
                PaymentStatus.CANCELLED, PaymentStatus.FAILED -> listener.paymentCancelled(response)
            }
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PaymentResponseKafkaListener::class.java)
    }
}
