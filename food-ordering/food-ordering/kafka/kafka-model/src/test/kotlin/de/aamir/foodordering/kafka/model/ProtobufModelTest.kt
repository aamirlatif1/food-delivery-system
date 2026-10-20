package de.aamir.foodordering.kafka.model

import com.google.protobuf.timestamp
import de.aamir.foodordering.kafka.model.common.v1.money
import de.aamir.foodordering.kafka.model.payment.v1.PaymentOrderStatus
import de.aamir.foodordering.kafka.model.payment.v1.PaymentRequest
import de.aamir.foodordering.kafka.model.payment.v1.paymentRequest
import de.aamir.foodordering.kafka.model.restaurant.v1.RestaurantApprovalRequest
import de.aamir.foodordering.kafka.model.restaurant.v1.SpiceLevel
import de.aamir.foodordering.kafka.model.restaurant.v1.orderNotes
import de.aamir.foodordering.kafka.model.restaurant.v1.orderProduct
import de.aamir.foodordering.kafka.model.restaurant.v1.restaurantApprovalRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProtobufModelTest {

    @Test
    fun `payment request survives a round trip through bytes`() {
        val request = paymentRequest {
            sagaId = "0192f0c1-0000-7000-8000-000000000001"
            orderId = "0192f0c1-0000-7000-8000-000000000002"
            customerId = "0192f0c1-0000-7000-8000-000000000003"
            price = money {
                amountMinor = 2_550
                currencyCode = "EUR"
            }
            createdAt = timestamp { seconds = 1_760_000_000 }
            paymentOrderStatus = PaymentOrderStatus.PAYMENT_ORDER_STATUS_PENDING
        }

        assertEquals(request, PaymentRequest.parseFrom(request.toByteArray()))
    }

    @Test
    fun `approval request without notes leaves them unset`() {
        val request = restaurantApprovalRequest {
            orderId = "0192f0c1-0000-7000-8000-000000000002"
            products += orderProduct {
                productId = "0192f0c1-0000-7000-8000-000000000004"
                quantity = 2
            }
        }
        val parsed = RestaurantApprovalRequest.parseFrom(request.toByteArray())

        assertFalse(parsed.hasNotes())
        assertEquals(2, parsed.getProducts(0).quantity)
    }

    @Test
    fun `notes keep their structured values`() {
        val request = restaurantApprovalRequest {
            notes = orderNotes {
                text = "extra spicy, nut allergy"
                allergies += "nuts"
                spiceLevel = SpiceLevel.SPICE_LEVEL_HOT
                requiresStaffReview = true
            }
        }
        val notes = RestaurantApprovalRequest.parseFrom(request.toByteArray()).notes

        assertEquals(listOf("nuts"), notes.allergiesList)
        assertEquals(SpiceLevel.SPICE_LEVEL_HOT, notes.spiceLevel)
    }
}
