package de.aamir.foodordering.order.application.ports.output.ai

import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException

// Turns free-text order notes into structured data, e.g. with an LLM.
interface OrderNotesInterpreter {

    // Throws OrderNotesInterpretationException when no trustworthy interpretation is possible.
    fun interpret(notes: String): OrderNotesInterpretation
}
