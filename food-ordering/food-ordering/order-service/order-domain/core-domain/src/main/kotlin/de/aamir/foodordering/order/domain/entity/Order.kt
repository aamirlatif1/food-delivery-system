package de.aamir.foodordering.order.domain.entity

import de.aamir.foodordering.common.domain.entity.AggregateRoot
import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.valueobject.OrderItemId
import de.aamir.foodordering.order.domain.valueobject.OrderNotes
import de.aamir.foodordering.order.domain.valueobject.StreetAddress
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import java.util.UUID

class Order private constructor(
    id: OrderId?,
    val customerId: CustomerId,
    val restaurantId: RestaurantId,
    val deliveryAddress: StreetAddress,
    val price: Money,
    val items: List<OrderItem>,
    // Null when the customer wrote no notes.
    val notes: OrderNotes?,
    trackingId: TrackingId?,
    status: OrderStatus?,
    failureMessages: List<String>,
) : AggregateRoot<OrderId>(id) {

    private var _trackingId: TrackingId? = trackingId
    val trackingId: TrackingId get() = checkNotNull(_trackingId) { "Order has no tracking id yet" }

    // Null until initializeOrder() for a new order.
    private var _status: OrderStatus? = status
    val status: OrderStatus get() = checkNotNull(_status) { "Order has no status yet" }

    private val _failureMessages = failureMessages.toMutableList()
    val failureMessages: List<String> get() = _failureMessages.toList()

    // Checks a new order before initializeOrder(): it is not initialized yet, its total price is > 0,
    // and its items are priced correctly and add up to the total.
    fun validateOrder() {
        validateInitialOrder()
        validateTotalPrice()
        validateItemsPrice()
    }

    private fun validateInitialOrder() {
        if (hasId || _status != null) {
            throw OrderDomainException("Order is not in the correct state for initialization")
        }
    }

    private fun validateTotalPrice() {
        if (!price.isGreaterThanZero()) {
            throw OrderDomainException("Order price must be greater than zero, was $price")
        }
    }

    // Every item's price equals its product's price and subTotal = price x quantity;
    // the subtotals add up to the order price.
    private fun validateItemsPrice() {
        if (items.isEmpty()) {
            throw OrderDomainException("Order must contain at least one item")
        }
        items.forEachIndexed { index, item ->
            if (!item.isPriceValid()) {
                throw OrderDomainException(
                    "Order item ${index + 1} for product ${item.product.id.value} has invalid price ${item.price} " +
                        "(product price ${item.product.price}), quantity ${item.quantity} or subtotal ${item.subTotal}",
                )
            }
        }
        val itemsTotal = items.fold(Money.ZERO) { sum, item -> sum + item.subTotal }
        if (price != itemsTotal) {
            throw OrderDomainException("Order price $price does not match the item total $itemsTotal")
        }
    }

    // Gives a new order its OrderId and TrackingId (random UUIDs), numbers the items from 1
    // and sets the status to PENDING.
    fun initializeOrder() {
        if (hasId) {
            throw OrderDomainException("Order ${id.value} is already initialized")
        }
        id = OrderId(UUID.randomUUID())
        _trackingId = TrackingId(UUID.randomUUID())
        _status = OrderStatus.PENDING
        items.forEachIndexed { index, item -> item.initialize(OrderItemId(index + 1L)) }
    }

    // PENDING -> PAID on PaymentCompleted.
    fun pay() {
        requireStatus(OrderStatus.PENDING, "pay")
        _status = OrderStatus.PAID
    }

    // PAID -> APPROVED on OrderApproved.
    fun approve() {
        requireStatus(OrderStatus.PAID, "approve")
        _status = OrderStatus.APPROVED
    }

    // PAID -> CANCELING on OrderRejected; the payment is refunded next.
    fun initCancel(failureMessages: List<String>) {
        requireStatus(OrderStatus.PAID, "start cancelling")
        _status = OrderStatus.CANCELING
        addFailureMessages(failureMessages)
    }

    // PENDING -> CANCELLED on PaymentFailed, CANCELING -> CANCELLED on PaymentCancelled.
    fun cancel(failureMessages: List<String>) {
        val current = currentStatus()
        if (current != OrderStatus.PENDING && current != OrderStatus.CANCELING) {
            throw OrderDomainException("Order ${id.value} cannot be cancelled in status $current")
        }
        _status = OrderStatus.CANCELLED
        addFailureMessages(failureMessages)
    }

    private fun currentStatus(): OrderStatus =
        _status ?: throw OrderDomainException("Order is not initialized")

    private fun requireStatus(expected: OrderStatus, action: String) {
        val current = currentStatus()
        if (current != expected) {
            throw OrderDomainException("Order ${id.value} cannot $action in status $current, expected $expected")
        }
    }

    private fun addFailureMessages(messages: List<String>) {
        _failureMessages += messages.filter { it.isNotBlank() }
    }

    companion object {

        // Builds, validates and initializes a new PENDING order. Items come without ids.
        fun create(
            customerId: CustomerId,
            restaurantId: RestaurantId,
            deliveryAddress: StreetAddress,
            price: Money,
            items: List<OrderItem>,
            notes: OrderNotes? = null,
        ): Order = builder()
            .customerId(customerId)
            .restaurantId(restaurantId)
            .deliveryAddress(deliveryAddress)
            .price(price)
            .items(items)
            .notes(notes)
            .build()
            .apply {
                validateOrder()
                initializeOrder()
            }

        fun builder(): Builder = Builder()
    }

    // Sets the fields as given and runs no business checks. Leave id, trackingId and status
    // unset for a new order and call validateOrder() and initializeOrder() (or use create());
    // set them all to rebuild an existing order, e.g. from the database.
    class Builder {
        private var id: OrderId? = null
        private var customerId: CustomerId? = null
        private var restaurantId: RestaurantId? = null
        private var deliveryAddress: StreetAddress? = null
        private var price: Money? = null
        private var items: List<OrderItem>? = null
        private var notes: OrderNotes? = null
        private var trackingId: TrackingId? = null
        private var status: OrderStatus? = null
        private var failureMessages: List<String> = emptyList()

        fun id(id: OrderId) = apply { this.id = id }

        fun customerId(customerId: CustomerId) = apply { this.customerId = customerId }

        fun restaurantId(restaurantId: RestaurantId) = apply { this.restaurantId = restaurantId }

        fun deliveryAddress(deliveryAddress: StreetAddress) = apply { this.deliveryAddress = deliveryAddress }

        fun price(price: Money) = apply { this.price = price }

        fun items(items: List<OrderItem>) = apply { this.items = items.toList() }

        fun notes(notes: OrderNotes?) = apply { this.notes = notes }

        fun trackingId(trackingId: TrackingId) = apply { this.trackingId = trackingId }

        fun status(status: OrderStatus) = apply { this.status = status }

        fun failureMessages(failureMessages: List<String>) = apply { this.failureMessages = failureMessages.toList() }

        fun build(): Order = Order(
            id = id,
            customerId = checkNotNull(customerId) { "Order customerId is required" },
            restaurantId = checkNotNull(restaurantId) { "Order restaurantId is required" },
            deliveryAddress = checkNotNull(deliveryAddress) { "Order deliveryAddress is required" },
            price = checkNotNull(price) { "Order price is required" },
            items = checkNotNull(items) { "Order items are required" },
            notes = notes,
            trackingId = trackingId,
            status = status,
            failureMessages = failureMessages,
        )
    }
}
