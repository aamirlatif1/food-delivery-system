package de.aamir.foodordering.order.application.ports.output.repository

import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.valueobject.TrackingId

interface OrderRepository {
    fun save(order: Order): Order

    fun findById(orderId: OrderId): Order?

    fun findByTrackingId(trackingId: TrackingId): Order?
}
