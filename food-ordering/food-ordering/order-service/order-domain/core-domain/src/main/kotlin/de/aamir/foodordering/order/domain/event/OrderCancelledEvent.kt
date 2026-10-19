package de.aamir.foodordering.order.domain.event

import de.aamir.foodordering.order.domain.entity.Order
import java.time.Instant

class OrderCancelledEvent(order: Order, occurredAt: Instant) : OrderEvent(order, occurredAt)
