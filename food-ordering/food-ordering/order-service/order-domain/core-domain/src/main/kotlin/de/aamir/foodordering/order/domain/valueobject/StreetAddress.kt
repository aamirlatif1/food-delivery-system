package de.aamir.foodordering.order.domain.valueobject

import de.aamir.foodordering.order.domain.exception.OrderDomainException

data class StreetAddress(
    val street: String,
    val postalCode: String,
    val city: String,
) {
    init {
        if (street.isBlank() || postalCode.isBlank() || city.isBlank()) {
            throw OrderDomainException("Street, postal code and city must not be blank")
        }
    }
}
