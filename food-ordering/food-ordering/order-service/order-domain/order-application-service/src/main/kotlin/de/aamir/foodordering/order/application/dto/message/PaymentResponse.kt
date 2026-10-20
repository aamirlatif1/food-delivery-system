package de.aamir.foodordering.order.application.dto.message

import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PaymentResponse(
    val sagaId: UUID,
    val orderId: UUID,
    val paymentId: UUID,
    val customerId: UUID,
    val price: BigDecimal,
    val createdAt: Instant,
    val paymentStatus: PaymentStatus,
    val failureMessages: List<String>,
)
