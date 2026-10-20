package de.aamir.foodordering.order.domain.valueobject

import de.aamir.foodordering.order.domain.exception.OrderDomainException

// The customer's free-text notes on an order and their structured form. The text is always kept,
// because it is what the customer actually wrote. An interpreter (e.g. an LLM) only suggests the
// structured values: interpreted() bounds them and decides when staff must review the notes.
@ConsistentCopyVisibility
data class OrderNotes private constructor(
    val text: String,
    val allergies: List<String>,
    val dietaryPreferences: Set<DietaryPreference>,
    val itemInstructions: List<ItemInstruction>,
    val spiceLevel: SpiceLevel,
    val deliveryInstructions: String?,
    val contactlessDelivery: Boolean,
    val requiresStaffReview: Boolean,
    val reviewReasons: List<String>,
    val source: InterpretationSource,
) {
    companion object {
        const val MAX_TEXT_LENGTH = 1000
        private const val MAX_LIST_SIZE = 10
        private const val MAX_VALUE_LENGTH = 200

        // An allergy always needs staff review, whatever the interpreter says.
        fun interpreted(
            text: String,
            allergies: List<String>,
            dietaryPreferences: Set<DietaryPreference>,
            itemInstructions: List<ItemInstruction>,
            spiceLevel: SpiceLevel,
            deliveryInstructions: String?,
            contactlessDelivery: Boolean,
            requiresStaffReview: Boolean,
            reviewReasons: List<String>,
        ): OrderNotes {
            val cleanAllergies = allergies.cleaned()
            val reasons = reviewReasons.cleaned().toMutableList()
            if (cleanAllergies.isNotEmpty()) {
                reasons += "Allergy declared: ${cleanAllergies.joinToString()}"
            }
            return OrderNotes(
                text = checkedText(text),
                allergies = cleanAllergies,
                dietaryPreferences = dietaryPreferences.toSet(),
                itemInstructions = itemInstructions
                    .map { ItemInstruction(it.item.bounded(), it.instruction.bounded()) }
                    .filter { it.item.isNotEmpty() && it.instruction.isNotEmpty() }
                    .distinct()
                    .take(MAX_LIST_SIZE),
                spiceLevel = spiceLevel,
                deliveryInstructions = deliveryInstructions?.bounded()?.ifEmpty { null },
                contactlessDelivery = contactlessDelivery,
                requiresStaffReview = requiresStaffReview || reasons.isNotEmpty(),
                reviewReasons = reasons.distinct(),
                source = InterpretationSource.AI,
            )
        }

        // The notes could not be interpreted; staff must read the text.
        fun uninterpreted(text: String): OrderNotes = OrderNotes(
            text = checkedText(text),
            allergies = emptyList(),
            dietaryPreferences = emptySet(),
            itemInstructions = emptyList(),
            spiceLevel = SpiceLevel.NOT_SPECIFIED,
            deliveryInstructions = null,
            contactlessDelivery = false,
            requiresStaffReview = true,
            reviewReasons = listOf("Order notes could not be interpreted automatically"),
            source = InterpretationSource.FALLBACK,
        )

        // Rebuilds notes that were checked before, e.g. from the database. Runs no rules.
        fun restore(
            text: String,
            allergies: List<String>,
            dietaryPreferences: Set<DietaryPreference>,
            itemInstructions: List<ItemInstruction>,
            spiceLevel: SpiceLevel,
            deliveryInstructions: String?,
            contactlessDelivery: Boolean,
            requiresStaffReview: Boolean,
            reviewReasons: List<String>,
            source: InterpretationSource,
        ) = OrderNotes(
            text, allergies.toList(), dietaryPreferences.toSet(), itemInstructions.toList(), spiceLevel,
            deliveryInstructions, contactlessDelivery, requiresStaffReview, reviewReasons.toList(), source,
        )

        private fun checkedText(text: String): String {
            val trimmed = text.trim()
            if (trimmed.isEmpty() || trimmed.length > MAX_TEXT_LENGTH) {
                throw OrderDomainException("Order notes must have 1 to $MAX_TEXT_LENGTH characters")
            }
            return trimmed
        }

        private fun List<String>.cleaned(): List<String> =
            map { it.bounded() }.filter { it.isNotEmpty() }.distinct().take(MAX_LIST_SIZE)

        private fun String.bounded(): String = trim().take(MAX_VALUE_LENGTH)
    }
}

// A change to one dish, e.g. item "pizza", instruction "no onions".
data class ItemInstruction(
    val item: String,
    val instruction: String,
)

enum class DietaryPreference {
    VEGETARIAN,
    VEGAN,
    HALAL,
    KOSHER,
    GLUTEN_FREE,
    LACTOSE_FREE,
}

enum class SpiceLevel {
    NOT_SPECIFIED,
    NONE,
    MILD,
    MEDIUM,
    HOT,
}

enum class InterpretationSource {
    // An interpreter produced the structured values.
    AI,

    // The interpreter failed or is disabled; only the text is known.
    FALLBACK,
}
