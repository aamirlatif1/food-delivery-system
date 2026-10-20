package de.aamir.foodordering.order.application.dto.message

import de.aamir.foodordering.common.domain.valueobject.OrderApprovalStatus
import java.time.Instant
import java.util.UUID

data class RestaurantApprovalResponse(
    val sagaId: UUID,
    val orderId: UUID,
    val restaurantId: UUID,
    val createdAt: Instant,
    val orderApprovalStatus: OrderApprovalStatus,
    val failureMessages: List<String>,
)
