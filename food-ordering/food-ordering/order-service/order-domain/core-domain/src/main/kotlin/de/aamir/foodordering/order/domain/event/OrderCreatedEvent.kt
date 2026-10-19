package de.aamir.foodordering.order.domain.event

import de.aamir.foodordering.common.domain.event.DomainEvent
import de.aamir.foodordering.order.domain.entity.Order
import java.time.Instant

class OrderCreatedEvent(
    override val aggregate: Order,
    override val occurredAt: Instant,
) : DomainEvent<Order>
