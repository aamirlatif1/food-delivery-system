package de.aamir.foodordering.order.domain.valueobject

import de.aamir.foodordering.common.domain.valueobject.BaseId
import java.util.UUID

class TrackingId(value: UUID) : BaseId<UUID>(value) {

    companion object {
        fun generate(): TrackingId = TrackingId(UUID.randomUUID())

        fun of(value: String): TrackingId = TrackingId(UUID.fromString(value))
    }
}
