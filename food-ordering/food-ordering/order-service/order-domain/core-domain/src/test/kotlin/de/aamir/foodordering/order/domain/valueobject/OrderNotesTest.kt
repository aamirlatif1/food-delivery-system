package de.aamir.foodordering.order.domain.valueobject

import de.aamir.foodordering.order.domain.exception.OrderDomainException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderNotesTest {

    private fun interpreted(
        text: String = "Pizza without onions, extra hot",
        allergies: List<String> = emptyList(),
        itemInstructions: List<ItemInstruction> = emptyList(),
        deliveryInstructions: String? = null,
        requiresStaffReview: Boolean = false,
    ) = OrderNotes.interpreted(
        text = text,
        allergies = allergies,
        dietaryPreferences = setOf(DietaryPreference.VEGETARIAN),
        itemInstructions = itemInstructions,
        spiceLevel = SpiceLevel.HOT,
        deliveryInstructions = deliveryInstructions,
        contactlessDelivery = false,
        requiresStaffReview = requiresStaffReview,
        reviewReasons = emptyList(),
    )

    @Test
    fun `keeps the interpretation and the trimmed text`() {
        val notes = interpreted(text = "  Pizza without onions  ", itemInstructions = listOf(ItemInstruction("pizza", "no onions")))

        assertEquals("Pizza without onions", notes.text)
        assertEquals(InterpretationSource.AI, notes.source)
        assertEquals(SpiceLevel.HOT, notes.spiceLevel)
        assertEquals(listOf(ItemInstruction("pizza", "no onions")), notes.itemInstructions)
        assertFalse(notes.requiresStaffReview)
    }

    @Test
    fun `an allergy always needs staff review, even when the interpreter says it does not`() {
        val notes = interpreted(allergies = listOf("peanuts"), requiresStaffReview = false)

        assertTrue(notes.requiresStaffReview)
        assertEquals(listOf("Allergy declared: peanuts"), notes.reviewReasons)
    }

    @Test
    fun `values are trimmed, deduplicated and bounded`() {
        val notes = interpreted(
            allergies = listOf(" peanuts ", "peanuts", " ") + (1..20).map { "a$it" },
            itemInstructions = listOf(ItemInstruction(" pizza ", " no onions "), ItemInstruction("", "x")),
            deliveryInstructions = "x".repeat(500),
        )

        assertEquals(10, notes.allergies.size)
        assertEquals("peanuts", notes.allergies.first())
        assertEquals(listOf(ItemInstruction("pizza", "no onions")), notes.itemInstructions)
        assertEquals(200, notes.deliveryInstructions?.length)
    }

    @Test
    fun `blank delivery instructions become null`() {
        assertNull(interpreted(deliveryInstructions = "  ").deliveryInstructions)
    }

    @Test
    fun `uninterpreted notes keep the text and need staff review`() {
        val notes = OrderNotes.uninterpreted(" No nuts please ")

        assertEquals("No nuts please", notes.text)
        assertEquals(InterpretationSource.FALLBACK, notes.source)
        assertTrue(notes.requiresStaffReview)
        assertTrue(notes.allergies.isEmpty())
    }

    @Test
    fun `text must not be blank or too long`() {
        assertFailsWith<OrderDomainException> { OrderNotes.uninterpreted(" ") }
        assertFailsWith<OrderDomainException> { interpreted(text = "x".repeat(OrderNotes.MAX_TEXT_LENGTH + 1)) }
    }
}
