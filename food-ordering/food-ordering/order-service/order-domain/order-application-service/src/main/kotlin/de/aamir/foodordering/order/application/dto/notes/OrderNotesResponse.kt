package de.aamir.foodordering.order.application.dto.notes

import de.aamir.foodordering.order.domain.valueobject.InterpretationSource

data class OrderNotesResponse(
    // Kept so that staff can always read what the customer actually wrote.
    val text: String,
    val interpretation: OrderNotesInterpretation,
    val source: InterpretationSource,
)
