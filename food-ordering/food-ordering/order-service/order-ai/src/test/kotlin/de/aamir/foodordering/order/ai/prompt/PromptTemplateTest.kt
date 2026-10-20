package de.aamir.foodordering.order.ai.prompt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PromptTemplateTest {

    private val template = PromptTemplate("test", "Hello {{name}}, you ordered {{dish}}.")

    @Test
    fun `renders every placeholder`() {
        assertEquals(setOf("name", "dish"), template.placeholders)
        assertEquals("Hello Ann, you ordered pizza.", template.render(mapOf("name" to "Ann", "dish" to "pizza")))
    }

    @Test
    fun `a value is inserted as is and not rendered again`() {
        assertEquals(
            "Hello {{dish}}, you ordered pizza.",
            template.render(mapOf("name" to "{{dish}}", "dish" to "pizza")),
        )
    }

    @Test
    fun `fails on a missing or an unknown value`() {
        assertFailsWith<IllegalArgumentException> { template.render(mapOf("name" to "Ann")) }
        assertFailsWith<IllegalArgumentException> {
            template.render(mapOf("name" to "Ann", "dish" to "pizza", "extra" to "x"))
        }
    }

    @Test
    fun `loads a template from the classpath`() {
        val user = PromptTemplate.fromClasspath("prompts/order-notes/user.txt")
        assertEquals(setOf("notes"), user.placeholders)
        assertTrue(PromptTemplate.fromClasspath("prompts/order-notes/system.txt").placeholders.isEmpty())
    }
}
