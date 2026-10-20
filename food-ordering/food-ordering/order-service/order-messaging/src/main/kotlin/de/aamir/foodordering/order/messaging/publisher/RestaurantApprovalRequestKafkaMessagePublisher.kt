package de.aamir.foodordering.order.messaging.publisher

import de.aamir.foodordering.kafka.producer.KafkaProducer
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.ports.output.message.publisher.RestaurantApprovalRequestMessagePublisher
import de.aamir.foodordering.order.messaging.config.OrderMessagingProperties
import de.aamir.foodordering.order.messaging.mapper.OrderMessagingDataMapper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

// Keyed by orderId, so every message of one saga lands on the same partition.
@Singleton
class RestaurantApprovalRequestKafkaMessagePublisher(
    private val kafkaProducer: KafkaProducer<String, ByteArray>,
    private val mapper: OrderMessagingDataMapper,
    private val properties: OrderMessagingProperties,
) : RestaurantApprovalRequestMessagePublisher {

    override fun publish(message: OrderApprovalOutboxMessage) {
        val request = mapper.toRestaurantApprovalRequest(message)
        val orderId = message.payload.orderId
        val headers = cloudEventHeaders(message.id, request, properties.source, orderId, message.payload.createdAt)
        kafkaProducer.sendAndAwait(properties.restaurantApprovalRequestTopic, orderId, request, headers)
        log.info("Published restaurant approval request for order {}, saga {}", orderId, message.sagaId)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(RestaurantApprovalRequestKafkaMessagePublisher::class.java)
    }
}
