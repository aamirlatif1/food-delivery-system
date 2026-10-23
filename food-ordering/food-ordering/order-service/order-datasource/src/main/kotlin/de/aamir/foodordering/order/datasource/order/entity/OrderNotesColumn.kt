package de.aamir.foodordering.order.datasource.order.entity

import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.InterpretationSource
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import io.micronaut.serde.annotation.Serdeable

// JSON form of OrderNotes in orders.notes. Owned by this adapter, so the stored format does not
// change when the domain class does.
@Serdeable
data class OrderNotesColumn(
    val text: String,
    val allergies: List<String>,
    val dietaryPreferences: Set<DietaryPreference>,
    val itemInstructions: List<ItemInstructionColumn>,
    val spiceLevel: SpiceLevel,
    val deliveryInstructions: String?,
    val contactlessDelivery: Boolean,
    val requiresStaffReview: Boolean,
    val reviewReasons: List<String>,
    val source: InterpretationSource,
)

@Serdeable
data class ItemInstructionColumn(
    val item: String,
    val instruction: String,
)
