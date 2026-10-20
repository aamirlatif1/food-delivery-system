package de.aamir.foodordering.order.messaging

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.kafka.producer.KafkaProducer
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventNotes
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventPayload
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventProduct
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.approval.RestaurantOrderStatus
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentEventPayload
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.payment.PaymentOrderStatus
import de.aamir.foodordering.order.application.saga.ORDER_SAGA_NAME
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import de.aamir.foodordering.order.messaging.config.OrderMessagingProperties
import de.aamir.foodordering.order.messaging.mapper.OrderMessagingDataMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

object MessagingFixture {
    val sagaId: UUID = UUID.fromString("0192f0c1-0000-7000-8000-000000000001")
    val orderId: UUID = UUID.fromString("0192f0c1-0000-7000-8000-000000000002")
    val customerId: UUID = UUID.fromString("0192f0c1-0000-7000-8000-000000000003")
    val restaurantId: UUID = UUID.fromString("0192f0c1-0000-7000-8000-000000000004")
    val productId: UUID = UUID.fromString("0192f0c1-0000-7000-8000-000000000005")
    val paymentId: UUID = UUID.fromString("0192f0c1-0000-7000-8000-000000000006")
    val outboxId: UUID = UUID.fromString("0192f0c1-0000-7000-8000-000000000007")
    val createdAt: Instant = Instant.parse("2026-10-20T10:15:30.123456789Z")

    val properties = OrderMessagingProperties()
    val mapper = OrderMessagingDataMapper(properties)

    fun paymentOutboxMessage(status: PaymentOrderStatus = PaymentOrderStatus.PENDING) = OrderPaymentOutboxMessage(
        id = outboxId,
        sagaId = sagaId,
        createdAt = createdAt,
        processedAt = null,
        type = ORDER_SAGA_NAME,
        payload = OrderPaymentEventPayload(orderId, customerId, BigDecimal("25.50"), createdAt, status),
        orderStatus = OrderStatus.PENDING,
        sagaStatus = SagaStatus.STARTED,
        outboxStatus = OutboxStatus.STARTED,
        version = 0,
    )

    fun approvalOutboxMessage(notes: OrderApprovalEventNotes? = null) = OrderApprovalOutboxMessage(
        id = outboxId,
        sagaId = sagaId,
        createdAt = createdAt,
        processedAt = null,
        type = ORDER_SAGA_NAME,
        payload = OrderApprovalEventPayload(
            orderId = orderId,
            restaurantId = restaurantId,
            price = BigDecimal("25.50"),
            products = listOf(OrderApprovalEventProduct(productId, 3, BigDecimal("8.50"))),
            createdAt = createdAt,
            restaurantOrderStatus = RestaurantOrderStatus.PAID,
            notes = notes,
        ),
        orderStatus = OrderStatus.PAID,
        sagaStatus = SagaStatus.PROCESSING,
        outboxStatus = OutboxStatus.STARTED,
        version = 0,
    )

    val notes = OrderApprovalEventNotes(
        text = "no peanuts please, extra hot",
        allergies = listOf("peanuts"),
        dietaryPreferences = setOf(DietaryPreference.VEGAN, DietaryPreference.HALAL),
        itemInstructions = listOf(ItemInstruction("curry", "extra hot")),
        spiceLevel = SpiceLevel.HOT,
        requiresStaffReview = true,
        reviewReasons = listOf("Allergy declared: peanuts"),
    )

    fun record(topic: String, value: ByteArray?, offset: Long = 0) =
        ConsumerRecord<String, ByteArray>(topic, 0, offset, orderId.toString(), value)
}

class Sent(val topic: String, val key: String, val value: ByteArray, val headers: Map<String, String>)

class RecordingKafkaProducer(private val failure: Exception? = null) : KafkaProducer<String, ByteArray> {
    val sent = mutableListOf<Sent>()

    override fun send(topic: String, key: String, message: ByteArray, headers: Map<String, String>): CompletableFuture<RecordMetadata> {
        if (failure != null) {
            return CompletableFuture.failedFuture(failure)
        }
        sent += Sent(topic, key, message, headers)
        return CompletableFuture.completedFuture(RecordMetadata(TopicPartition(topic, 0), 0, 0, 0, 0, 0))
    }
}
