package de.aamir.foodordering.order.application.mapper

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.CreateOrderResponse
import de.aamir.foodordering.order.application.dto.track.TrackOrderResponse
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventPayload
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventProduct
import de.aamir.foodordering.order.application.outbox.model.approval.RestaurantOrderStatus
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentEventPayload
import de.aamir.foodordering.order.application.outbox.model.payment.PaymentOrderStatus
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.OrderItem
import de.aamir.foodordering.order.domain.entity.Product
import de.aamir.foodordering.order.domain.event.OrderCancelledEvent
import de.aamir.foodordering.order.domain.event.OrderCreatedEvent
import de.aamir.foodordering.order.domain.event.OrderEvent
import de.aamir.foodordering.order.domain.event.OrderPaidEvent
import de.aamir.foodordering.order.domain.valueobject.StreetAddress

object OrderDataMapper {

    // The request only knows each product's id. The product name stays empty until
    // OrderDomainService replaces the product with the restaurant's menu product.
    fun createOrderCommandToOrder(command: CreateOrderCommand): Order = Order.builder()
        .customerId(CustomerId(command.customerId))
        .restaurantId(RestaurantId(command.restaurantId))
        .deliveryAddress(StreetAddress(command.address.street, command.address.postalCode, command.address.city))
        .price(Money(command.price))
        .items(
            command.items.map {
                OrderItem.builder()
                    .product(Product(ProductId(it.productId), name = "", price = Money(it.price)))
                    .quantity(it.quantity)
                    .price(Money(it.price))
                    .subTotal(Money(it.subTotal))
                    .build()
            },
        )
        .build()

    fun orderToCreateOrderResponse(order: Order, message: String) =
        CreateOrderResponse(order.trackingId.value, order.status, message)

    fun orderToTrackOrderResponse(order: Order) =
        TrackOrderResponse(order.trackingId.value, order.status, order.failureMessages)

    fun orderCreatedEventToPaymentPayload(event: OrderCreatedEvent) =
        paymentPayload(event, PaymentOrderStatus.PENDING)

    fun orderCancelledEventToPaymentPayload(event: OrderCancelledEvent) =
        paymentPayload(event, PaymentOrderStatus.CANCELLED)

    fun orderPaidEventToApprovalPayload(event: OrderPaidEvent): OrderApprovalEventPayload {
        val order = event.aggregate
        return OrderApprovalEventPayload(
            orderId = order.id.value,
            restaurantId = order.restaurantId.value,
            price = order.price.amount,
            products = order.items.map { OrderApprovalEventProduct(it.product.id.value, it.quantity) },
            createdAt = event.occurredAt,
            restaurantOrderStatus = RestaurantOrderStatus.PAID,
        )
    }

    private fun paymentPayload(event: OrderEvent, status: PaymentOrderStatus) =
        OrderPaymentEventPayload(
            orderId = event.aggregate.id.value,
            customerId = event.aggregate.customerId.value,
            price = event.aggregate.price.amount,
            createdAt = event.occurredAt,
            paymentOrderStatus = status,
        )
}
