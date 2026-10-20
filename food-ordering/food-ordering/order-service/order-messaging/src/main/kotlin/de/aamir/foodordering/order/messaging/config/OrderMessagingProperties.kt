package de.aamir.foodordering.order.messaging.config

import io.micronaut.context.annotation.ConfigurationProperties

// order-service.messaging.*; topic names follow REQUIREMENTS.md §5.1. The listeners read their
// group id from the same prefix (consumer-group-id); their topics are fixed, see ListenerSupport.kt.
@ConfigurationProperties("order-service.messaging")
class OrderMessagingProperties {
    var paymentRequestTopic: String = "payment-request-topic"
    var restaurantApprovalRequestTopic: String = "restaurant-approval-request-topic"

    // ce_source of every message this service publishes.
    var source: String = "order-service"

    // The domain's Money has no currency; every amount on the wire is in this one.
    var currencyCode: String = "EUR"
}
