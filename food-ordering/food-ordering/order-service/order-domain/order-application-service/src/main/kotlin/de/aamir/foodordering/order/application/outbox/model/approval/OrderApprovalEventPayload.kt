package de.aamir.foodordering.order.application.outbox.model.approval

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// Body of a RestaurantApprovalRequest: ids, amounts and enums only, no personal data.
data class OrderApprovalEventPayload(
    val orderId: UUID,
    val restaurantId: UUID,
    val price: BigDecimal,
    val products: List<OrderApprovalEventProduct>,
    val createdAt: Instant,
    val restaurantOrderStatus: RestaurantOrderStatus,
)

data class OrderApprovalEventProduct(
    val id: UUID,
    val quantity: Int,
)

enum class RestaurantOrderStatus {
    PAID,
}
