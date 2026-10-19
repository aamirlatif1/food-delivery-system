package de.aamir.foodordering.order.domain.exception

import de.aamir.foodordering.common.domain.exception.DomainException

class OrderDomainException(message: String, cause: Throwable? = null) : DomainException(message, cause)
