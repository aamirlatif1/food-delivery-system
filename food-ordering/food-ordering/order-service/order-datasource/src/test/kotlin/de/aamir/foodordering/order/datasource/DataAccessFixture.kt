package de.aamir.foodordering.order.datasource

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventNotes
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventPayload
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventProduct
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.approval.RestaurantOrderStatus
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentEventPayload
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.payment.PaymentOrderStatus
import de.aamir.foodordering.order.application.saga.ORDER_SAGA_NAME
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.OrderItem
import de.aamir.foodordering.order.domain.entity.Product
import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.domain.valueobject.OrderNotes
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import de.aamir.foodordering.order.domain.valueobject.StreetAddress
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object DataAccessFixture {

    val customerId: UUID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb41")
    val restaurantId: UUID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb45")
    val pizzaId: UUID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb47")
    val saladId: UUID = UUID.fromString("d215b5f8-0249-4dc5-89a3-51fd148cfb48")

    // DATETIME(6) keeps microseconds, so test timestamps have no nanoseconds.
    val createdAt: Instant = Instant.parse("2026-10-20T10:15:30.123456Z")

    val notes: OrderNotes = OrderNotes.interpreted(
        text = "No onions on the pizza, I am allergic to peanuts. Leave it at the door.",
        allergies = listOf("peanuts"),
        dietaryPreferences = setOf(DietaryPreference.VEGETARIAN),
        itemInstructions = listOf(ItemInstruction("pizza", "no onions")),
        spiceLevel = SpiceLevel.MILD,
        deliveryInstructions = "Leave it at the door",
        contactlessDelivery = true,
        requiresStaffReview = false,
        reviewReasons = emptyList(),
    )

    // A new PENDING order: 2 x pizza at 12.50 + 1 x salad at 7.25.
    fun order(notes: OrderNotes? = DataAccessFixture.notes): Order = Order.create(
        customerId = CustomerId(customerId),
        restaurantId = RestaurantId(restaurantId),
        deliveryAddress = StreetAddress("Hauptstraße 1", "10115", "Berlin"),
        price = Money.of("32.25"),
        items = listOf(
            item(pizzaId, "Pizza", "12.50", 2),
            item(saladId, "Salad", "7.25", 1),
        ),
        notes = notes,
    )

    private fun item(productId: UUID, name: String, price: String, quantity: Int) = OrderItem(
        product = Product(ProductId(productId), name, Money.of(price)),
        quantity = quantity,
        price = Money.of(price),
        subTotal = Money.of(price) * quantity,
    )

    fun paymentOutboxMessage(
        sagaId: UUID = UUID.randomUUID(),
        sagaStatus: SagaStatus = SagaStatus.STARTED,
        outboxStatus: OutboxStatus = OutboxStatus.STARTED,
        createdAt: Instant = DataAccessFixture.createdAt,
    ) = OrderPaymentOutboxMessage(
        id = UUID.randomUUID(),
        sagaId = sagaId,
        createdAt = createdAt,
        processedAt = null,
        type = ORDER_SAGA_NAME,
        payload = OrderPaymentEventPayload(
            orderId = UUID.randomUUID(),
            customerId = customerId,
            price = BigDecimal("32.25"),
            createdAt = createdAt,
            paymentOrderStatus = PaymentOrderStatus.PENDING,
        ),
        orderStatus = OrderStatus.PENDING,
        sagaStatus = sagaStatus,
        outboxStatus = outboxStatus,
        version = 0,
    )

    fun approvalOutboxMessage(sagaId: UUID = UUID.randomUUID()) = OrderApprovalOutboxMessage(
        id = UUID.randomUUID(),
        sagaId = sagaId,
        createdAt = createdAt,
        processedAt = null,
        type = ORDER_SAGA_NAME,
        payload = OrderApprovalEventPayload(
            orderId = UUID.randomUUID(),
            restaurantId = restaurantId,
            price = BigDecimal("32.25"),
            products = listOf(
                OrderApprovalEventProduct(pizzaId, 2, BigDecimal("12.50")),
                OrderApprovalEventProduct(saladId, 1, BigDecimal("7.25")),
            ),
            createdAt = createdAt,
            restaurantOrderStatus = RestaurantOrderStatus.PAID,
            notes = OrderApprovalEventNotes(
                text = notes.text,
                allergies = notes.allergies,
                dietaryPreferences = notes.dietaryPreferences,
                itemInstructions = notes.itemInstructions,
                spiceLevel = notes.spiceLevel,
                requiresStaffReview = notes.requiresStaffReview,
                reviewReasons = notes.reviewReasons,
            ),
        ),
        orderStatus = OrderStatus.PAID,
        sagaStatus = SagaStatus.PROCESSING,
        outboxStatus = OutboxStatus.STARTED,
        version = 0,
    )
}
