package de.aamir.foodordering.order.application.outbox.model.payment

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// Body of a PaymentRequest: ids, amounts and enums only, no personal data.
data class OrderPaymentEventPayload(
    val orderId: UUID,
    val customerId: UUID,
    val price: BigDecimal,
    val createdAt: Instant,
    val paymentOrderStatus: PaymentOrderStatus,
)

enum class PaymentOrderStatus {
    PENDING,
    CANCELLED,
}
