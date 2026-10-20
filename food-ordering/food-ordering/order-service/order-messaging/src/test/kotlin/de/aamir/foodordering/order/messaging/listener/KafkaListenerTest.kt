package de.aamir.foodordering.order.messaging.listener

import de.aamir.foodordering.kafka.model.common.v1.money
import de.aamir.foodordering.kafka.model.payment.v1.PaymentStatus
import de.aamir.foodordering.kafka.model.payment.v1.paymentResponse
import de.aamir.foodordering.kafka.model.restaurant.v1.OrderApprovalStatus
import de.aamir.foodordering.kafka.model.restaurant.v1.restaurantApprovalResponse
import de.aamir.foodordering.order.application.dto.message.PaymentResponse
import de.aamir.foodordering.order.application.dto.message.RestaurantApprovalResponse
import de.aamir.foodordering.order.application.ports.input.message.listener.PaymentResponseMessageListener
import de.aamir.foodordering.order.application.ports.input.message.listener.RestaurantApprovalResponseMessageListener
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.messaging.MessagingFixture
import de.aamir.foodordering.order.messaging.MessagingFixture.mapper
import de.aamir.foodordering.order.messaging.MessagingFixture.record
import de.aamir.foodordering.order.messaging.mapper.InvalidMessageException
import java.sql.SQLTransactionRollbackException
import java.sql.SQLTransientConnectionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

// Micronaut routes what receive throws: RetryableMessageException to <topic>.retry, anything
// else to <topic>.dlq. These tests pin down which is which.
class KafkaListenerTest {

    private class RecordingPaymentListener(private val failure: Exception? = null) : PaymentResponseMessageListener {
        val calls = mutableListOf<String>()

        override fun paymentCompleted(response: PaymentResponse) {
            failure?.let { throw it }
            calls += "completed ${response.sagaId}"
        }

        override fun paymentCancelled(response: PaymentResponse) {
            calls += "cancelled ${response.paymentStatus}"
        }
    }

    private class RecordingApprovalListener : RestaurantApprovalResponseMessageListener {
        val calls = mutableListOf<String>()

        override fun orderApproved(response: RestaurantApprovalResponse) {
            calls += "approved"
        }

        override fun orderRejected(response: RestaurantApprovalResponse) {
            calls += "rejected ${response.failureMessages}"
        }
    }

    private fun paymentResponse(status: PaymentStatus) = paymentResponse {
        sagaId = MessagingFixture.sagaId.toString()
        orderId = MessagingFixture.orderId.toString()
        paymentId = MessagingFixture.paymentId.toString()
        customerId = MessagingFixture.customerId.toString()
        price = money {
            amountMinor = 2_550
            currencyCode = "EUR"
        }
        paymentStatus = status
    }.toByteArray()

    private fun receivePayment(listener: PaymentResponseMessageListener, value: ByteArray?) =
        PaymentResponseKafkaListener(listener, mapper).receive(record(PAYMENT_RESPONSE_TOPIC, value))

    @Test
    fun `routes payment responses by status`() {
        val ports = RecordingPaymentListener()

        receivePayment(ports, paymentResponse(PaymentStatus.PAYMENT_STATUS_COMPLETED))
        receivePayment(ports, paymentResponse(PaymentStatus.PAYMENT_STATUS_FAILED))
        receivePayment(ports, paymentResponse(PaymentStatus.PAYMENT_STATUS_CANCELLED))

        assertEquals(listOf("completed ${MessagingFixture.sagaId}", "cancelled FAILED", "cancelled CANCELLED"), ports.calls)
    }

    // Drill D3: poison messages go to the DLQ without a retry.
    @Test
    fun `undecodable, invalid and empty records are not retryable`() {
        val ports = RecordingPaymentListener()

        assertFailsWith<InvalidMessageException> { receivePayment(ports, byteArrayOf(0x0A, 0x7F, 0x01)) }
        assertFailsWith<InvalidMessageException> { receivePayment(ports, paymentResponse(PaymentStatus.PAYMENT_STATUS_UNSPECIFIED)) }
        assertFailsWith<InvalidMessageException> { receivePayment(ports, null) }
        assertEquals(emptyList(), ports.calls)
    }

    @Test
    fun `deadlocks and lost connections are retryable`() {
        val deadlock = IllegalStateException("tx failed", SQLTransactionRollbackException("Deadlock found"))
        val connection = SQLTransientConnectionException("Connection is not available")

        for (failure in listOf(deadlock, connection)) {
            val thrown = assertFailsWith<RetryableMessageException> {
                receivePayment(RecordingPaymentListener(failure), paymentResponse(PaymentStatus.PAYMENT_STATUS_COMPLETED))
            }
            assertSame(failure, thrown.cause)
        }
    }

    @Test
    fun `invariant violations are not retryable`() {
        assertFailsWith<OrderDomainException> {
            receivePayment(
                RecordingPaymentListener(OrderDomainException("Order is not in the correct state")),
                paymentResponse(PaymentStatus.PAYMENT_STATUS_COMPLETED),
            )
        }
    }

    @Test
    fun `routes approval responses by status`() {
        val ports = RecordingApprovalListener()
        val listener = RestaurantApprovalResponseKafkaListener(ports, mapper)
        fun response(status: OrderApprovalStatus) = restaurantApprovalResponse {
            sagaId = MessagingFixture.sagaId.toString()
            orderId = MessagingFixture.orderId.toString()
            restaurantId = MessagingFixture.restaurantId.toString()
            orderApprovalStatus = status
            if (status == OrderApprovalStatus.ORDER_APPROVAL_STATUS_REJECTED) failureMessages += "Restaurant closed"
        }.toByteArray()

        listener.receive(record(RESTAURANT_APPROVAL_RESPONSE_TOPIC, response(OrderApprovalStatus.ORDER_APPROVAL_STATUS_APPROVED)))
        listener.receive(record(RESTAURANT_APPROVAL_RESPONSE_TOPIC, response(OrderApprovalStatus.ORDER_APPROVAL_STATUS_REJECTED)))

        assertEquals(listOf("approved", "rejected [Restaurant closed]"), ports.calls)
    }
}
