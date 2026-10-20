package de.aamir.foodordering.order.application.dto.create

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import java.util.UUID

data class CreateOrderResponse(
    val orderTrackingId: UUID,
    val orderStatus: OrderStatus,
    val message: String,
)
