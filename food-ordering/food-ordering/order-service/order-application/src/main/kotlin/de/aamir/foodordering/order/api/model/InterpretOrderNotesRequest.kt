package de.aamir.foodordering.order.api.model

import de.aamir.foodordering.order.application.dto.notes.InterpretOrderNotesCommand
import io.micronaut.serde.annotation.Serdeable

// Body of POST /order-notes/interpretation; validated by the application service.
@Serdeable
data class InterpretOrderNotesRequest(val notes: String) {
    fun toCommand() = InterpretOrderNotesCommand(notes)
}
