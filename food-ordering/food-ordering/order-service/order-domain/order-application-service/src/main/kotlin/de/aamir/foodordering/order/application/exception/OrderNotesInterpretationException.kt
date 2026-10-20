package de.aamir.foodordering.order.application.exception

// Thrown by an OrderNotesInterpreter when it cannot produce a trustworthy interpretation:
// the model is unavailable, refused, was cut off or returned output that breaks the schema.
class OrderNotesInterpretationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
