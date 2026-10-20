package de.aamir.foodordering.order.application.outbox.model.approval

import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// Body of a RestaurantApprovalRequest: ids, amounts and enums, and the order notes the kitchen
// needs. No customer id, address or delivery instructions.
data class OrderApprovalEventPayload(
    val orderId: UUID,
    val restaurantId: UUID,
    val price: BigDecimal,
    val products: List<OrderApprovalEventProduct>,
    val createdAt: Instant,
    val restaurantOrderStatus: RestaurantOrderStatus,
    val notes: OrderApprovalEventNotes?,
)

// price is the unit price the order was placed at; the restaurant rejects the order if it changed.
data class OrderApprovalEventProduct(
    val id: UUID,
    val quantity: Int,
    val price: BigDecimal,
)

// The kitchen's part of the order notes. text is included because staff must read it when
// requiresStaffReview is set.
data class OrderApprovalEventNotes(
    val text: String,
    val allergies: List<String>,
    val dietaryPreferences: Set<DietaryPreference>,
    val itemInstructions: List<ItemInstruction>,
    val spiceLevel: SpiceLevel,
    val requiresStaffReview: Boolean,
    val reviewReasons: List<String>,
)

enum class RestaurantOrderStatus {
    PAID,
}
