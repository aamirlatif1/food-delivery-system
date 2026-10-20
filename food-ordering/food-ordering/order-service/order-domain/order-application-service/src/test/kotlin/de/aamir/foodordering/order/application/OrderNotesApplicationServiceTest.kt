package de.aamir.foodordering.order.application

import de.aamir.foodordering.order.application.dto.notes.InterpretOrderNotesCommand
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import de.aamir.foodordering.order.domain.valueobject.InterpretationSource
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The value rules themselves are covered by OrderNotesTest in core-domain.
class OrderNotesApplicationServiceTest {

    private val interpreter = FakeOrderNotesInterpreter()
    private val service = OrderNotesApplicationServiceImpl(OrderNotesHelper(interpreter))

    @Test
    fun `passes the notes to the interpreter and returns the checked interpretation`() {
        interpreter.answer = OrderNotesInterpretation.EMPTY.copy(
            itemInstructions = listOf(ItemInstruction("pizza", "no onions")),
            spiceLevel = SpiceLevel.HOT,
            deliveryInstructions = "Ring twice",
        )

        val response = service.interpretNotes(InterpretOrderNotesCommand("Pizza without onions, extra hot, ring twice"))

        assertEquals(listOf("Pizza without onions, extra hot, ring twice"), interpreter.received)
        assertEquals(InterpretationSource.AI, response.source)
        assertEquals("Pizza without onions, extra hot, ring twice", response.text)
        assertEquals(SpiceLevel.HOT, response.interpretation.spiceLevel)
        assertFalse(response.interpretation.requiresStaffReview)
    }

    @Test
    fun `the domain rules apply to the interpreter's answer`() {
        interpreter.answer = OrderNotesInterpretation.EMPTY.copy(allergies = listOf("peanuts"), requiresStaffReview = false)

        val response = service.interpretNotes(InterpretOrderNotesCommand("I am allergic to peanuts"))

        assertTrue(response.interpretation.requiresStaffReview)
        assertEquals(listOf("Allergy declared: peanuts"), response.interpretation.reviewReasons)
    }

    @Test
    fun `a failed interpretation falls back to staff review of the text`() {
        interpreter.failure = OrderNotesInterpretationException("model unavailable")

        val response = service.interpretNotes(InterpretOrderNotesCommand("No nuts please"))

        assertEquals(InterpretationSource.FALLBACK, response.source)
        assertEquals("No nuts please", response.text)
        assertTrue(response.interpretation.requiresStaffReview)
    }
}
