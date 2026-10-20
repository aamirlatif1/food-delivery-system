package de.aamir.foodordering.order.application.dto

import de.aamir.foodordering.order.application.dto.notes.InterpretOrderNotesCommand
import de.aamir.foodordering.order.application.dto.notes.MAX_NOTES_LENGTH
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.ports.input.service.OrderNotesApplicationService
import de.aamir.foodordering.order.application.ports.output.ai.OrderNotesInterpreter
import io.micronaut.context.ApplicationContext
import jakarta.validation.ConstraintViolationException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InterpretOrderNotesCommandValidationTest {

    private var calls = 0
    private val interpreter = object : OrderNotesInterpreter {
        override fun interpret(notes: String): OrderNotesInterpretation {
            calls++
            return OrderNotesInterpretation.EMPTY
        }
    }
    private val context: ApplicationContext = ApplicationContext.builder()
        .properties(mapOf("order-service.outbox.scheduler.enabled" to "false"))
        .singletons(interpreter)
        .start()
    private val service = context.getBean(OrderNotesApplicationService::class.java)

    @AfterTest
    fun close() = context.close()

    @Test
    fun `blank or too long notes are rejected before the interpreter is called`() {
        assertFailsWith<ConstraintViolationException> { service.interpretNotes(InterpretOrderNotesCommand(" ")) }
        assertFailsWith<ConstraintViolationException> {
            service.interpretNotes(InterpretOrderNotesCommand("x".repeat(MAX_NOTES_LENGTH + 1)))
        }
        assertEquals(0, calls)
    }

    @Test
    fun `notes within the limit reach the interpreter`() {
        service.interpretNotes(InterpretOrderNotesCommand("x".repeat(MAX_NOTES_LENGTH)))
        assertEquals(1, calls)
    }
}
