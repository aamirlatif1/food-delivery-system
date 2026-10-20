package de.aamir.foodordering.order.messaging.publisher

import de.aamir.foodordering.kafka.producer.KafkaProducer
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.ports.output.message.publisher.PaymentRequestMessagePublisher
import de.aamir.foodordering.order.messaging.config.OrderMessagingProperties
import de.aamir.foodordering.order.messaging.mapper.OrderMessagingDataMapper
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

// Keyed by orderId, so every message of one saga lands on the same partition.
@Singleton
class PaymentRequestKafkaMessagePublisher(
    private val kafkaProducer: KafkaProducer<String, ByteArray>,
    private val mapper: OrderMessagingDataMapper,
    private val properties: OrderMessagingProperties,
) : PaymentRequestMessagePublisher {

    override fun publish(message: OrderPaymentOutboxMessage) {
        val request = mapper.toPaymentRequest(message)
        val orderId = message.payload.orderId
        val headers = cloudEventHeaders(message.id, request, properties.source, orderId, message.payload.createdAt)
        kafkaProducer.sendAndAwait(properties.paymentRequestTopic, orderId, request, headers)
        log.info("Published {} payment request for order {}, saga {}", request.paymentOrderStatus, orderId, message.sagaId)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PaymentRequestKafkaMessagePublisher::class.java)
    }
}
