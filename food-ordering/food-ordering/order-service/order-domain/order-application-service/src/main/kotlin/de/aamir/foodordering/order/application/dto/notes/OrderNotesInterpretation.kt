package de.aamir.foodordering.order.application.dto.notes

import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel

// What an OrderNotesInterpreter suggests for the notes. It is untrusted until
// OrderNotes.interpreted() has applied the business rules to it.
data class OrderNotesInterpretation(
    val allergies: List<String>,
    val dietaryPreferences: Set<DietaryPreference>,
    val itemInstructions: List<ItemInstruction>,
    val spiceLevel: SpiceLevel,
    val deliveryInstructions: String?,
    val contactlessDelivery: Boolean,
    val requiresStaffReview: Boolean,
    val reviewReasons: List<String>,
) {
    companion object {
        val EMPTY = OrderNotesInterpretation(
            allergies = emptyList(),
            dietaryPreferences = emptySet(),
            itemInstructions = emptyList(),
            spiceLevel = SpiceLevel.NOT_SPECIFIED,
            deliveryInstructions = null,
            contactlessDelivery = false,
            requiresStaffReview = false,
            reviewReasons = emptyList(),
        )
    }
}
