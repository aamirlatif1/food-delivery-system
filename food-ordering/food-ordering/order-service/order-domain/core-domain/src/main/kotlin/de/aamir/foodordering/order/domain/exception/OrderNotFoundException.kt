package de.aamir.foodordering.order.domain.exception

import de.aamir.foodordering.common.domain.exception.DomainException

class OrderNotFoundException(message: String) : DomainException(message)
