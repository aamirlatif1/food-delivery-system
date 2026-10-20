package de.aamir.foodordering.order.api

import de.aamir.foodordering.order.application.dto.create.CreateOrderResponse
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.dto.notes.OrderNotesResponse
import de.aamir.foodordering.order.application.dto.track.TrackOrderResponse
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import io.micronaut.serde.annotation.SerdeImport

// Responses are the application's DTOs, imported here to keep JSON annotations out of the
// application layer. Requests have their own models (api.model): the commands are already
// @Introspected for validation, and that introspection would win over an import.
@SerdeImport(CreateOrderResponse::class)
@SerdeImport(TrackOrderResponse::class)
@SerdeImport(OrderNotesResponse::class)
@SerdeImport(OrderNotesInterpretation::class)
@SerdeImport(ItemInstruction::class)
class SerdeConfiguration
