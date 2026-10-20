package de.aamir.foodordering.order.ai.notes

import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderNotesResponseParserTest {

    @Test
    fun `parses a schema-conformant answer`() {
        val interpretation = OrderNotesResponseParser.parse(VALID_ANSWER)

        assertEquals(listOf("peanuts"), interpretation.allergies)
        assertEquals(setOf(DietaryPreference.VEGETARIAN), interpretation.dietaryPreferences)
        assertEquals(listOf(ItemInstruction("pizza", "no onions")), interpretation.itemInstructions)
        assertEquals(SpiceLevel.MILD, interpretation.spiceLevel)
        assertEquals("Ring twice", interpretation.deliveryInstructions)
        assertTrue(interpretation.contactlessDelivery)
        assertTrue(interpretation.requiresStaffReview)
        assertEquals(listOf("Allergy"), interpretation.reviewReasons)
    }

    @Test
    fun `null delivery instructions stay null`() {
        val interpretation = OrderNotesResponseParser.parse(
            VALID_ANSWER.replace("\"Ring twice\"", "null"),
        )
        assertNull(interpretation.deliveryInstructions)
    }

    @Test
    fun `rejects anything outside the schema`() {
        listOf(
            "not json",
            "[]",
            VALID_ANSWER.replace("\"MILD\"", "\"VOLCANIC\""),
            VALID_ANSWER.replace("\"VEGETARIAN\"", "\"PALEO\""),
            VALID_ANSWER.replace("\"contactlessDelivery\": true", "\"contactlessDelivery\": \"yes\""),
            VALID_ANSWER.replace("\"reviewReasons\": [\"Allergy\"]", "\"reviewReasons\": [1]"),
            VALID_ANSWER.replace(",\n  \"reviewReasons\": [\"Allergy\"]", ""),
            VALID_ANSWER.replace("\"reviewReasons\"", "\"discount\": 50, \"reviewReasons\""),
        ).forEach { json ->
            assertFailsWith<OrderNotesInterpretationException>(json) { OrderNotesResponseParser.parse(json) }
        }
    }

    @Test
    fun `schema requires every field and allows no others`() {
        @Suppress("UNCHECKED_CAST")
        val item = (OrderNotesSchema.properties.getValue("itemInstructions") as Map<String, Any>)["items"] as Map<String, Any>
        assertEquals(listOf("item", "instruction"), item["required"])
        assertEquals(false, item["additionalProperties"])
        assertEquals(SpiceLevel.entries.map { it.name }, (OrderNotesSchema.properties.getValue("spiceLevel") as Map<*, *>)["enum"])
    }

    companion object {
        val VALID_ANSWER = """
            {
              "allergies": ["peanuts"],
              "dietaryPreferences": ["VEGETARIAN"],
              "itemInstructions": [{"item": "pizza", "instruction": "no onions"}],
              "spiceLevel": "MILD",
              "deliveryInstructions": "Ring twice",
              "contactlessDelivery": true,
              "requiresStaffReview": true,
              "reviewReasons": ["Allergy"]
            }
        """.trimIndent()
    }
}
