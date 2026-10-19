package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.entity.AggregateRoot
import de.aamir.foodordering.common.domain.valueobject.CustomerId

// Read-only copy of a customer-service customer, built from the customer view.
// The order domain only needs to know that the customer exists.
class Customer(id: CustomerId) : AggregateRoot<CustomerId>(id)
