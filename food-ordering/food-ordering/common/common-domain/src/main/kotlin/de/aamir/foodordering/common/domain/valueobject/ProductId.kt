package de.aamir.foodordering.common.domain.valueobject

import java.util.UUID

class ProductId(value: UUID) : BaseId<UUID>(value) {

    companion object {
        fun generate(): ProductId = ProductId(UUID.randomUUID())

        fun of(value: String): ProductId = ProductId(UUID.fromString(value))
    }
}
