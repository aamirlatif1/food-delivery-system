package de.aamir.foodordering.order.messaging.publisher

import de.aamir.foodordering.kafka.model.CloudEventHeaders
import de.aamir.foodordering.kafka.model.payment.v1.PaymentOrderStatus
import de.aamir.foodordering.kafka.model.payment.v1.PaymentRequest
import de.aamir.foodordering.kafka.model.restaurant.v1.RestaurantApprovalRequest
import de.aamir.foodordering.kafka.producer.KafkaProducerException
import de.aamir.foodordering.order.messaging.MessagingFixture
import de.aamir.foodordering.order.messaging.MessagingFixture.mapper
import de.aamir.foodordering.order.messaging.MessagingFixture.orderId
import de.aamir.foodordering.order.messaging.MessagingFixture.properties
import de.aamir.foodordering.order.messaging.RecordingKafkaProducer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KafkaMessagePublisherTest {

    @Test
    fun `payment request is keyed by order id and carries the envelope headers`() {
        val producer = RecordingKafkaProducer()

        PaymentRequestKafkaMessagePublisher(producer, mapper, properties).publish(MessagingFixture.paymentOutboxMessage())

        val sent = producer.sent.single()
        assertEquals("payment-request-topic", sent.topic)
        assertEquals(orderId.toString(), sent.key)
        assertEquals(PaymentOrderStatus.PAYMENT_ORDER_STATUS_PENDING, PaymentRequest.parseFrom(sent.value).paymentOrderStatus)
        assertEquals(
            mapOf(
                CloudEventHeaders.ID to MessagingFixture.outboxId.toString(),
                CloudEventHeaders.TYPE to "payment.v1.PaymentRequest",
                CloudEventHeaders.SOURCE to "order-service",
                CloudEventHeaders.SUBJECT to orderId.toString(),
                CloudEventHeaders.TIME to "2026-10-20T10:15:30.123456789Z",
                CloudEventHeaders.SCHEMA_VERSION to "v1",
            ),
            sent.headers,
        )
    }

    @Test
    fun `approval request goes to its topic`() {
        val producer = RecordingKafkaProducer()

        RestaurantApprovalRequestKafkaMessagePublisher(producer, mapper, properties)
            .publish(MessagingFixture.approvalOutboxMessage())

        val sent = producer.sent.single()
        assertEquals("restaurant-approval-request-topic", sent.topic)
        assertEquals("restaurant.v1.RestaurantApprovalRequest", sent.headers[CloudEventHeaders.TYPE])
        assertEquals(orderId.toString(), RestaurantApprovalRequest.parseFrom(sent.value).orderId)
    }

    // The outbox scheduler marks the row FAILED when publish throws.
    @Test
    fun `a failed send is thrown to the caller unwrapped`() {
        val producer = RecordingKafkaProducer(failure = KafkaProducerException("broker down"))

        assertFailsWith<KafkaProducerException> {
            PaymentRequestKafkaMessagePublisher(producer, mapper, properties).publish(MessagingFixture.paymentOutboxMessage())
        }
    }
}
