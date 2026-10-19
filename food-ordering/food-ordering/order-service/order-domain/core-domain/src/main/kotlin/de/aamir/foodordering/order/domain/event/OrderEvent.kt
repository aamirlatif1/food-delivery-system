package de.aamir.foodordering.order.domain.event

import de.aamir.foodordering.common.domain.event.DomainEvent
import de.aamir.foodordering.order.domain.entity.Order
import java.time.Instant

abstract class OrderEvent(
    override val aggregate: Order,
    override val occurredAt: Instant,
) : DomainEvent<Order>
