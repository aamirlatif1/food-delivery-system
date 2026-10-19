package de.aamir.foodordering.common.domain.valueobject

import java.util.UUID

class OrderId(value: UUID) : BaseId<UUID>(value) {

    companion object {
        fun generate(): OrderId = OrderId(UUID.randomUUID())

        fun of(value: String): OrderId = OrderId(UUID.fromString(value))
    }
}
