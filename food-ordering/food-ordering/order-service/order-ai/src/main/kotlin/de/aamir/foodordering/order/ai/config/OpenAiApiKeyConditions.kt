package de.aamir.foodordering.order.ai.config

import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext

// The key comes from OPENAI_API_KEY via a placeholder with an empty default, so the property
// always exists; these conditions check that it has a value rather than that it is present.
class OpenAiApiKeyConfigured : Condition {
    override fun matches(context: ConditionContext<*>): Boolean = hasApiKey(context)
}

class OpenAiApiKeyMissing : Condition {
    override fun matches(context: ConditionContext<*>): Boolean = !hasApiKey(context)
}

private fun hasApiKey(context: ConditionContext<*>): Boolean =
    context.getProperty(API_KEY_PROPERTY, String::class.java).map { it.isNotBlank() }.orElse(false)
