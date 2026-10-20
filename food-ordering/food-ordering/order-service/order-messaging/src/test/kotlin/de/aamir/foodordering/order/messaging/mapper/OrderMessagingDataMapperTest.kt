package de.aamir.foodordering.order.messaging.mapper

import de.aamir.foodordering.common.domain.valueobject.OrderApprovalStatus
import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import de.aamir.foodordering.kafka.model.common.v1.money
import de.aamir.foodordering.kafka.model.payment.v1.PaymentOrderStatus
import de.aamir.foodordering.kafka.model.payment.v1.PaymentStatus as PaymentStatusProto
import de.aamir.foodordering.kafka.model.payment.v1.paymentResponse
import de.aamir.foodordering.kafka.model.restaurant.v1.DietaryPreference
import de.aamir.foodordering.kafka.model.restaurant.v1.OrderApprovalStatus as OrderApprovalStatusProto
import de.aamir.foodordering.kafka.model.restaurant.v1.SpiceLevel
import de.aamir.foodordering.kafka.model.restaurant.v1.restaurantApprovalResponse
import de.aamir.foodordering.order.application.outbox.model.payment.PaymentOrderStatus as PaymentOrderStatusDto
import de.aamir.foodordering.order.messaging.MessagingFixture
import de.aamir.foodordering.order.messaging.MessagingFixture.mapper
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class OrderMessagingDataMapperTest {

    @Test
    fun `payment request carries ids, price in cents and the status`() {
        val request = mapper.toPaymentRequest(MessagingFixture.paymentOutboxMessage(PaymentOrderStatusDto.CANCELLED))

        assertEquals(MessagingFixture.sagaId.toString(), request.sagaId)
        assertEquals(MessagingFixture.customerId.toString(), request.customerId)
        assertEquals(2_550, request.price.amountMinor)
        assertEquals("EUR", request.price.currencyCode)
        assertEquals(MessagingFixture.createdAt.nano, request.createdAt.nanos)
        assertEquals(PaymentOrderStatus.PAYMENT_ORDER_STATUS_CANCELLED, request.paymentOrderStatus)
    }

    @Test
    fun `approval request carries unit prices and the kitchen notes`() {
        val request = mapper.toRestaurantApprovalRequest(MessagingFixture.approvalOutboxMessage(MessagingFixture.notes))

        val product = request.productsList.single()
        assertEquals(3, product.quantity)
        assertEquals(850, product.price.amountMinor)
        assertEquals(listOf("peanuts"), request.notes.allergiesList)
        assertEquals(
            setOf(DietaryPreference.DIETARY_PREFERENCE_VEGAN, DietaryPreference.DIETARY_PREFERENCE_HALAL),
            request.notes.dietaryPreferencesList.toSet(),
        )
        assertEquals("extra hot", request.notes.itemInstructionsList.single().instruction)
        assertEquals(SpiceLevel.SPICE_LEVEL_HOT, request.notes.spiceLevel)
    }

    @Test
    fun `approval request without notes leaves them unset`() {
        assertFalse(mapper.toRestaurantApprovalRequest(MessagingFixture.approvalOutboxMessage()).hasNotes())
    }

    @Test
    fun `payment response maps to the application dto`() {
        val response = mapper.toPaymentResponse(paymentResponseProto())

        assertEquals(MessagingFixture.paymentId, response.paymentId)
        assertEquals(BigDecimal("25.50"), response.price)
        assertEquals(PaymentStatus.FAILED, response.paymentStatus)
        assertEquals(listOf("Not enough credit"), response.failureMessages)
    }

    @Test
    fun `approval response maps to the application dto`() {
        val response = mapper.toRestaurantApprovalResponse(
            restaurantApprovalResponse {
                sagaId = MessagingFixture.sagaId.toString()
                orderId = MessagingFixture.orderId.toString()
                restaurantId = MessagingFixture.restaurantId.toString()
                orderApprovalStatus = OrderApprovalStatusProto.ORDER_APPROVAL_STATUS_REJECTED
                failureMessages += "Product unavailable"
            },
        )

        assertEquals(OrderApprovalStatus.REJECTED, response.orderApprovalStatus)
        assertEquals(MessagingFixture.restaurantId, response.restaurantId)
    }

    @Test
    fun `rejects malformed ids, unknown statuses and foreign currencies`() {
        assertFailsWith<InvalidMessageException> { mapper.toPaymentResponse(paymentResponseProto().toBuilder().setOrderId("nope").build()) }
        assertFailsWith<InvalidMessageException> {
            mapper.toPaymentResponse(paymentResponseProto().toBuilder().setPaymentStatusValue(42).build())
        }
        assertFailsWith<InvalidMessageException> {
            mapper.toPaymentResponse(paymentResponseProto().toBuilder().setPrice(money { amountMinor = 1; currencyCode = "USD" }).build())
        }
    }

    private fun paymentResponseProto() = paymentResponse {
        sagaId = MessagingFixture.sagaId.toString()
        orderId = MessagingFixture.orderId.toString()
        paymentId = MessagingFixture.paymentId.toString()
        customerId = MessagingFixture.customerId.toString()
        price = money {
            amountMinor = 2_550
            currencyCode = "EUR"
        }
        paymentStatus = PaymentStatusProto.PAYMENT_STATUS_FAILED
        failureMessages += "Not enough credit"
    }
}
