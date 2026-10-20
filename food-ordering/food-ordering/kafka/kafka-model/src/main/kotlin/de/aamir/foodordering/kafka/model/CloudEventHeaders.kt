package de.aamir.foodordering.kafka.model

// Kafka header names of the message envelope (REQUIREMENTS.md §5.2), modelled on the CloudEvents
// Kafka binding. The payload itself is a Protobuf message.
object CloudEventHeaders {
    // Unique per message and stable across re-sends: the consumer's idempotency key.
    const val ID = "ce_id"

    // Fully qualified Protobuf message name, e.g. payment.v1.PaymentRequest.
    const val TYPE = "ce_type"

    // Name of the publishing service.
    const val SOURCE = "ce_source"

    // The id the message is about, usually the orderId.
    const val SUBJECT = "ce_subject"

    // ISO-8601 time of the occurrence.
    const val TIME = "ce_time"

    // Version segment of the Protobuf package, e.g. v1.
    const val SCHEMA_VERSION = "ce_schemaversion"
}
