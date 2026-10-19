package de.aamir.foodordering.common.domain.valueobject

import java.util.UUID

class RestaurantId(value: UUID) : BaseId<UUID>(value) {

    companion object {
        fun generate(): RestaurantId = RestaurantId(UUID.randomUUID())

        fun of(value: String): RestaurantId = RestaurantId(UUID.fromString(value))
    }
}
