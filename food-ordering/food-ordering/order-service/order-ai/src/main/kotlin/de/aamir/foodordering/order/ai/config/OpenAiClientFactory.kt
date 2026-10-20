package de.aamir.foodordering.order.ai.config

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires

@Factory
class OpenAiClientFactory {

    @Bean(preDestroy = "close")
    @Requires(condition = OpenAiApiKeyConfigured::class)
    fun openAiClient(properties: OpenAiProperties): OpenAIClient =
        OpenAIOkHttpClient.builder()
            .apiKey(checkNotNull(properties.apiKey))
            .apply { properties.baseUrl?.let { baseUrl(it) } }
            .timeout(properties.timeout)
            .maxRetries(properties.maxRetries)
            .build()
}

// Set from the OPENAI_API_KEY environment variable in application.properties.
const val API_KEY_PROPERTY = "order-service.ai.openai.api-key"
