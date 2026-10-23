package de.aamir.foodordering.order.application.outbox

// STARTED: not sent yet. COMPLETED: the broker acked it. FAILED: the last send failed; the
// scheduler sends it again on its next run, so a broker outage only delays the message.
enum class OutboxStatus {
    STARTED,
    COMPLETED,
    FAILED,
}

// What the outbox schedulers publish.
val UNPUBLISHED: Set<OutboxStatus> = setOf(OutboxStatus.STARTED, OutboxStatus.FAILED)
