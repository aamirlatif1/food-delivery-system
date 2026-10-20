package de.aamir.foodordering.order.application.ports.output.repository

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.order.domain.entity.Customer

// Reads the customer view.
interface CustomerRepository {
    fun findCustomer(customerId: CustomerId): Customer?
}
