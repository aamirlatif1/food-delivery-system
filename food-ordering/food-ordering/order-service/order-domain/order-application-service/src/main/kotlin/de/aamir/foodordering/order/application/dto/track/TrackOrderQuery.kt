package de.aamir.foodordering.order.application.dto.track

import io.micronaut.core.annotation.Introspected
import java.util.UUID

// orderTrackingId is non-null by type; nothing else to check.
@Introspected
data class TrackOrderQuery(val orderTrackingId: UUID)
