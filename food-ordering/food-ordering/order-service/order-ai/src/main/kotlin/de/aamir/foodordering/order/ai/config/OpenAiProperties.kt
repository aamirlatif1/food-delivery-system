package de.aamir.foodordering.order.ai.config

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

// order-service.ai.openai.*; apiKey is read from the OPENAI_API_KEY environment variable.
@ConfigurationProperties("order-service.ai.openai")
class OpenAiProperties {
    var apiKey: String? = null

    // Only for tests or a proxy; the SDK default is https://api.openai.com/v1.
    var baseUrl: String? = null
    var model: String = "gpt-4.1-mini"

    // 0 keeps the interpretation repeatable. Leave unset for models that reject temperature.
    var temperature: Double? = 0.0
    var maxCompletionTokens: Long = 800
    // Per attempt. With the circuit breaker's one retry, a slow OpenAI adds up to 2 x timeout to
    // POST /orders before the notes fall back to staff review.
    var timeout: Duration = Duration.ofSeconds(8)

    // The SDK's own retries. Off: the circuit breaker in OpenAiChatCompletions retries, and both
    // together would multiply the attempts.
    var maxRetries: Int = 0
}
