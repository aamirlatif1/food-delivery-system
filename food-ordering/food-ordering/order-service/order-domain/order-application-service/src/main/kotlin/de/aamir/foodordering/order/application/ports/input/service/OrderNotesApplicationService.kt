package de.aamir.foodordering.order.application.ports.input.service

import de.aamir.foodordering.order.application.dto.notes.InterpretOrderNotesCommand
import de.aamir.foodordering.order.application.dto.notes.OrderNotesResponse
import jakarta.validation.Valid

// Invalid input throws jakarta.validation.ConstraintViolationException before the interpreter is called.
interface OrderNotesApplicationService {
    fun interpretNotes(@Valid command: InterpretOrderNotesCommand): OrderNotesResponse
}
