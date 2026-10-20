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
    var timeout: Duration = Duration.ofSeconds(15)
    var maxRetries: Int = 1
}
