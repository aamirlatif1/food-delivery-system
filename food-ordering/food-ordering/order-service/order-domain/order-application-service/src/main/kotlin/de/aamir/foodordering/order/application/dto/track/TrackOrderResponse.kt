package de.aamir.foodordering.order.application.dto.track

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import java.util.UUID

data class TrackOrderResponse(
    val orderTrackingId: UUID,
    val orderStatus: OrderStatus,
    val failureMessages: List<String>,
)
