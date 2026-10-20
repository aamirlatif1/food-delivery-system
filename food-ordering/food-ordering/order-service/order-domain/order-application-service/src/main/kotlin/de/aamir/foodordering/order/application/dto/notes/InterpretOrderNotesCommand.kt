package de.aamir.foodordering.order.application.dto.notes

import de.aamir.foodordering.order.domain.valueobject.OrderNotes
import io.micronaut.core.annotation.Introspected
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// The customer's free-text order notes, e.g. "no onions, I'm allergic to peanuts, ring twice",
// interpreted without placing an order, e.g. to preview them.
@Introspected
data class InterpretOrderNotesCommand(
    @field:NotBlank
    @field:Size(max = MAX_NOTES_LENGTH)
    val notes: String,
)

// Bounds the prompt size, and so the cost and the room for prompt injection.
const val MAX_NOTES_LENGTH = OrderNotes.MAX_TEXT_LENGTH
