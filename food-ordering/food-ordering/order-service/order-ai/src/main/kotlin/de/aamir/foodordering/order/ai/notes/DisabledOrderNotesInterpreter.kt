package de.aamir.foodordering.order.ai.notes

import de.aamir.foodordering.order.ai.config.OpenAiApiKeyMissing
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import de.aamir.foodordering.order.application.ports.output.ai.OrderNotesInterpreter
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

// Used when OPENAI_API_KEY is not set, e.g. locally and in tests; every order then
// falls back to staff review of the raw notes.
@Singleton
@Requires(condition = OpenAiApiKeyMissing::class)
class DisabledOrderNotesInterpreter : OrderNotesInterpreter {
    override fun interpret(notes: String): OrderNotesInterpretation =
        throw OrderNotesInterpretationException("AI order notes interpreter is disabled: OPENAI_API_KEY is not set")
}
