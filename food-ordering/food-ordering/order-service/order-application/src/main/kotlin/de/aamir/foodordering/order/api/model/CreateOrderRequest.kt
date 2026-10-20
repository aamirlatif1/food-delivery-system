package de.aamir.foodordering.order.api.model

import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.OrderAddressDto
import de.aamir.foodordering.order.application.dto.create.OrderItemDto
import io.micronaut.serde.annotation.Serdeable
import java.math.BigDecimal
import java.util.UUID

// Body of POST /orders. Only the JSON shape lives here; the application service validates the
// command and Micronaut turns its ConstraintViolationException into a 400.
@Serdeable
data class CreateOrderRequest(
    val customerId: UUID,
    val restaurantId: UUID,
    val address: AddressRequest,
    val price: BigDecimal,
    val items: List<OrderItemRequest>,
    val notes: String? = null,
) {
    fun toCommand() = CreateOrderCommand(
        customerId = customerId,
        restaurantId = restaurantId,
        price = price,
        items = items.map { OrderItemDto(it.productId, it.quantity, it.price, it.subTotal) },
        address = OrderAddressDto(address.street, address.postalCode, address.city),
        notes = notes,
    )
}

@Serdeable
data class OrderItemRequest(
    val productId: UUID,
    val quantity: Int,
    val price: BigDecimal,
    val subTotal: BigDecimal,
)

@Serdeable
data class AddressRequest(
    val street: String,
    val postalCode: String,
    val city: String,
)
