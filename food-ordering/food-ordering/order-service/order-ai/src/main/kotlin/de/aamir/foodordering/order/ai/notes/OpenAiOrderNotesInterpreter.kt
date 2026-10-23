package de.aamir.foodordering.order.ai.notes

import com.openai.errors.OpenAIException
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import de.aamir.foodordering.order.ai.config.OpenAiApiKeyConfigured
import de.aamir.foodordering.order.ai.config.OpenAiProperties
import de.aamir.foodordering.order.ai.prompt.OrderNotesPrompts
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import de.aamir.foodordering.order.application.ports.output.ai.OrderNotesInterpreter
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

// Calls the OpenAI Chat Completions API with the system prompt, the rendered user template
// and a strict JSON schema, then parses the answer. Every failure, an open circuit included,
// becomes OrderNotesInterpretationException, so the order falls back to staff review. Notes are never logged: they can hold
// personal and health data.
@Singleton
@Requires(condition = OpenAiApiKeyConfigured::class)
class OpenAiOrderNotesInterpreter(
    private val chatCompletions: OpenAiChatCompletions,
    private val properties: OpenAiProperties,
) : OrderNotesInterpreter {

    override fun interpret(notes: String): OrderNotesInterpretation {
        val started = System.nanoTime()
        val completion = try {
            chatCompletions.create(params(notes))
        } catch (e: OpenAIException) {
            throw OrderNotesInterpretationException("OpenAI call failed: ${e.javaClass.simpleName}", e)
        }
        val choice = completion.choices().firstOrNull()
            ?: throw OrderNotesInterpretationException("OpenAI returned no choices")
        log.info(
            "OpenAI order notes call: model {}, prompt {}, finish reason {}, {} ms",
            completion.model(),
            OrderNotesPrompts.VERSION,
            choice.finishReason(),
            (System.nanoTime() - started) / 1_000_000,
        )
        choice.message().refusal().ifPresent {
            throw OrderNotesInterpretationException("OpenAI refused to interpret the notes")
        }
        if (choice.finishReason() != ChatCompletion.Choice.FinishReason.STOP) {
            throw OrderNotesInterpretationException("OpenAI stopped with finish reason ${choice.finishReason()}")
        }
        val content = choice.message().content().orElse("")
        if (content.isBlank()) {
            throw OrderNotesInterpretationException("OpenAI returned no content")
        }
        return OrderNotesResponseParser.parse(content)
    }

    private fun params(notes: String): ChatCompletionCreateParams =
        ChatCompletionCreateParams.builder()
            .model(properties.model)
            .addSystemMessage(OrderNotesPrompts.systemMessage())
            .addUserMessage(OrderNotesPrompts.userMessage(notes))
            .responseFormat(OrderNotesSchema.responseFormat)
            .maxCompletionTokens(properties.maxCompletionTokens)
            .n(1)
            // The notes can hold health data; do not keep them at OpenAI.
            .store(false)
            .apply { properties.temperature?.let { temperature(it) } }
            .build()

    private companion object {
        private val log = LoggerFactory.getLogger(OpenAiOrderNotesInterpreter::class.java)
    }
}
