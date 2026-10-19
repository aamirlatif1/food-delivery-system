package de.aamir.foodordering.common.domain.valueobject

import java.util.UUID

class CustomerId(value: UUID) : BaseId<UUID>(value) {

    companion object {
        fun generate(): CustomerId = CustomerId(UUID.randomUUID())

        fun of(value: String): CustomerId = CustomerId(UUID.fromString(value))
    }
}
