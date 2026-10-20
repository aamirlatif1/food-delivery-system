package de.aamir.foodordering.order.messaging.mapper

// A message that decoded but cannot be handled: a malformed id, an unknown enum value or a
// foreign currency. Retrying cannot fix it.
class InvalidMessageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
