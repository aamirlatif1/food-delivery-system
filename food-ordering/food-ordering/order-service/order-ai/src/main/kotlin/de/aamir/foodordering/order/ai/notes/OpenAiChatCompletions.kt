package de.aamir.foodordering.order.ai.notes

import com.openai.client.OpenAIClient
import com.openai.errors.OpenAIException
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import de.aamir.foodordering.order.ai.config.OpenAiApiKeyConfigured
import io.micronaut.context.annotation.Requires
import io.micronaut.retry.annotation.CircuitBreaker
import jakarta.inject.Singleton

// The raw Chat Completions call behind a circuit breaker, which also owns the retry: a failed
// call is retried once after a short delay (the SDK's own retries are off, see OpenAiProperties).
// If the retry fails too, the circuit opens: for the reset period every call rethrows that failure
// at once instead of making POST /orders wait for OpenAI. The first call after the reset goes
// through and closes the circuit again if it succeeds.
//
// Only failures to get an answer count. A refusal or an unusable answer means OpenAI is up; the
// interpreter rejects those after this call returns, so they are neither retried nor open the circuit.
@Singleton
@Requires(condition = OpenAiApiKeyConfigured::class)
// open: Micronaut proxies @CircuitBreaker beans, and KSP does not see the all-open plugin.
open class OpenAiChatCompletions(private val client: OpenAIClient) {

    @CircuitBreaker(
        // Retries after the first call; Micronaut does not allow 0.
        attempts = "1",
        delay = "\${order-service.ai.openai.circuit-breaker.retry-delay:500ms}",
        reset = "\${order-service.ai.openai.circuit-breaker.reset:30s}",
        includes = [OpenAIException::class],
    )
    open fun create(params: ChatCompletionCreateParams): ChatCompletion =
        client.chat().completions().create(params)
}
