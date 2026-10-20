package de.aamir.foodordering.order.messaging.listener

import de.aamir.foodordering.common.domain.valueobject.OrderApprovalStatus
import de.aamir.foodordering.kafka.consumer.KafkaConsumer
import de.aamir.foodordering.kafka.model.restaurant.v1.RestaurantApprovalResponse as RestaurantApprovalResponseProto
import de.aamir.foodordering.order.application.ports.input.message.listener.RestaurantApprovalResponseMessageListener
import de.aamir.foodordering.order.messaging.mapper.OrderMessagingDataMapper
import io.micronaut.configuration.kafka.annotation.ErrorStrategy
import io.micronaut.configuration.kafka.annotation.ErrorStrategyValue
import io.micronaut.configuration.kafka.annotation.KafkaListener
import io.micronaut.configuration.kafka.annotation.OffsetStrategy
import io.micronaut.configuration.kafka.annotation.Topic
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

// Same retry and DLQ handling as PaymentResponseKafkaListener.
@KafkaListener(
    groupId = CONSUMER_GROUP_ID,
    offsetStrategy = OffsetStrategy.SYNC_PER_RECORD,
    errorStrategy = ErrorStrategy(
        value = ErrorStrategyValue.RETRY_TOPIC_ON_ERROR,
        retryTopicSuffixes = [RETRY_TOPIC_SUFFIX],
        retryTopicDelays = [RETRY_DELAY],
        exceptionTypes = [RetryableMessageException::class],
        dlq = RESTAURANT_APPROVAL_RESPONSE_TOPIC + DLQ_TOPIC_SUFFIX,
    ),
)
class RestaurantApprovalResponseKafkaListener(
    private val listener: RestaurantApprovalResponseMessageListener,
    private val mapper: OrderMessagingDataMapper,
) : KafkaConsumer<String, ByteArray> {

    @Topic(RESTAURANT_APPROVAL_RESPONSE_TOPIC)
    override fun receive(record: ConsumerRecord<String, ByteArray>) {
        val response = record.decode { mapper.toRestaurantApprovalResponse(RestaurantApprovalResponseProto.parseFrom(it)) }
        log.info(
            "Received {} approval response for order {} from {}",
            response.orderApprovalStatus, response.orderId, record.topic(),
        )
        retryOnTransientFailure {
            when (response.orderApprovalStatus) {
                OrderApprovalStatus.APPROVED -> listener.orderApproved(response)
                OrderApprovalStatus.REJECTED -> listener.orderRejected(response)
            }
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RestaurantApprovalResponseKafkaListener::class.java)
    }
}
