package de.aamir.foodordering.common.domain.event

import de.aamir.foodordering.common.domain.entity.AggregateRoot
import java.time.Instant

interface DomainEvent<T : AggregateRoot<*>> {
    val aggregate: T
    val occurredAt: Instant
}
