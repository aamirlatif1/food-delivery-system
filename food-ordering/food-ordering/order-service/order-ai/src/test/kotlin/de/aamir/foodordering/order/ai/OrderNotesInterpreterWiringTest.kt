package de.aamir.foodordering.order.ai

import de.aamir.foodordering.order.ai.config.API_KEY_PROPERTY
import de.aamir.foodordering.order.ai.notes.DisabledOrderNotesInterpreter
import de.aamir.foodordering.order.ai.notes.OpenAiOrderNotesInterpreter
import de.aamir.foodordering.order.application.ports.output.ai.OrderNotesInterpreter
import io.micronaut.context.ApplicationContext
import kotlin.test.Test
import kotlin.test.assertIs

// The key arrives as order-service.ai.openai.api-key=${OPENAI_API_KEY:}, so it is blank, not missing,
// when the variable is unset.
class OrderNotesInterpreterWiringTest {

    private fun interpreterFor(apiKey: String?): OrderNotesInterpreter {
        val properties = buildMap {
            put("order-service.outbox.scheduler.enabled", "false")
            apiKey?.let { put(API_KEY_PROPERTY, it) }
        }
        return ApplicationContext.run(properties).use { it.getBean(OrderNotesInterpreter::class.java) }
    }

    @Test
    fun `uses OpenAI when the key has a value`() {
        assertIs<OpenAiOrderNotesInterpreter>(interpreterFor("sk-test"))
    }

    @Test
    fun `is disabled when the key is blank or missing`() {
        assertIs<DisabledOrderNotesInterpreter>(interpreterFor(""))
        assertIs<DisabledOrderNotesInterpreter>(interpreterFor("  "))
        assertIs<DisabledOrderNotesInterpreter>(interpreterFor(null))
    }
}
