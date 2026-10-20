package de.aamir.foodordering.order.application

import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import de.aamir.foodordering.order.application.ports.output.ai.OrderNotesInterpreter
import de.aamir.foodordering.order.domain.valueobject.OrderNotes
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

// Interprets the notes and turns the suggestion into OrderNotes, which apply the business rules.
// A failed interpretation never fails the order: the notes then fall back to staff review.
// Call it outside a transaction; the interpreter may take seconds.
@Singleton
class OrderNotesHelper(
    private val orderNotesInterpreter: OrderNotesInterpreter,
) {

    fun interpret(text: String): OrderNotes {
        val interpretation = try {
            orderNotesInterpreter.interpret(text)
        } catch (e: OrderNotesInterpretationException) {
            log.warn("Order notes could not be interpreted, falling back to staff review: {}", e.message)
            return OrderNotes.uninterpreted(text)
        }
        val notes = with(interpretation) {
            OrderNotes.interpreted(
                text = text,
                allergies = allergies,
                dietaryPreferences = dietaryPreferences,
                itemInstructions = itemInstructions,
                spiceLevel = spiceLevel,
                deliveryInstructions = deliveryInstructions,
                contactlessDelivery = contactlessDelivery,
                requiresStaffReview = requiresStaffReview,
                reviewReasons = reviewReasons,
            )
        }
        log.info(
            "Order notes interpreted: {} allergies, {} item instructions, staff review {}",
            notes.allergies.size,
            notes.itemInstructions.size,
            notes.requiresStaffReview,
        )
        return notes
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OrderNotesHelper::class.java)
    }
}
