package de.aamir.foodordering.order.application.dto.create

import de.aamir.foodordering.order.application.dto.notes.MAX_NOTES_LENGTH
import io.micronaut.core.annotation.Introspected
import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

// Shape checks only. Business rules (menu price, subtotal = price x quantity, total = sum of
// subtotals) are the domain's job.
@Introspected
data class CreateOrderCommand(
    val customerId: UUID,
    val restaurantId: UUID,
    @field:Positive
    @field:Digits(integer = MONEY_INTEGER_DIGITS, fraction = MONEY_FRACTION_DIGITS)
    val price: BigDecimal,
    @field:NotEmpty
    @field:Size(max = MAX_ITEMS)
    val items: List<@Valid OrderItemDto>,
    @field:Valid
    val address: OrderAddressDto,
    // Free text such as "no onions, peanut allergy, ring twice"; interpreted into OrderNotes.
    @field:Size(max = MAX_NOTES_LENGTH)
    val notes: String? = null,
)

@Introspected
data class OrderItemDto(
    val productId: UUID,
    @field:Positive
    val quantity: Int,
    @field:Positive
    @field:Digits(integer = MONEY_INTEGER_DIGITS, fraction = MONEY_FRACTION_DIGITS)
    val price: BigDecimal,
    @field:Positive
    @field:Digits(integer = MONEY_INTEGER_DIGITS, fraction = MONEY_FRACTION_DIGITS)
    val subTotal: BigDecimal,
)

@Introspected
data class OrderAddressDto(
    @field:NotBlank
    @field:Size(max = 100)
    val street: String,
    @field:NotBlank
    @field:Size(max = 10)
    val postalCode: String,
    @field:NotBlank
    @field:Size(max = 50)
    val city: String,
)

// Money has two decimals (see common-domain Money); 10 integer digits is far beyond any order.
private const val MONEY_INTEGER_DIGITS = 10
private const val MONEY_FRACTION_DIGITS = 2
private const val MAX_ITEMS = 100
