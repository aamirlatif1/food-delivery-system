package de.aamir.foodordering.order.datasource.order.mapper

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.datasource.common.readColumn
import de.aamir.foodordering.order.datasource.order.entity.ItemInstructionColumn
import de.aamir.foodordering.order.datasource.order.entity.OrderEntity
import de.aamir.foodordering.order.datasource.order.entity.OrderItemEntity
import de.aamir.foodordering.order.datasource.order.entity.OrderItemEntityId
import de.aamir.foodordering.order.datasource.order.entity.OrderNotesColumn
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.OrderItem
import de.aamir.foodordering.order.domain.entity.Product
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.domain.valueobject.OrderItemId
import de.aamir.foodordering.order.domain.valueobject.OrderNotes
import de.aamir.foodordering.order.domain.valueobject.StreetAddress
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import io.micronaut.core.type.Argument
import io.micronaut.json.JsonMapper
import jakarta.inject.Singleton

@Singleton
class OrderDataAccessMapper(private val jsonMapper: JsonMapper) {

    // version is set by the caller: 0 for a new order, the stored version for an update.
    fun orderToEntity(order: Order, version: Int): OrderEntity = OrderEntity(
        id = order.id.value,
        trackingId = order.trackingId.value,
        customerId = order.customerId.value,
        restaurantId = order.restaurantId.value,
        price = order.price.amount,
        orderStatus = order.status,
        street = order.deliveryAddress.street,
        postalCode = order.deliveryAddress.postalCode,
        city = order.deliveryAddress.city,
        notes = order.notes?.let { jsonMapper.writeValueAsString(notesToColumn(it)) },
        failureMessages = jsonMapper.writeValueAsString(order.failureMessages),
        version = version,
    )

    fun orderItemsToEntities(order: Order): List<OrderItemEntity> = order.items.map { item ->
        OrderItemEntity(
            id = OrderItemEntityId(orderId = order.id.value, id = item.id.value),
            productId = item.product.id.value,
            productName = item.product.name,
            price = item.price.amount,
            quantity = item.quantity,
            subTotal = item.subTotal.amount,
        )
    }

    fun entityToOrder(entity: OrderEntity, items: List<OrderItemEntity>): Order = Order.builder()
        .id(OrderId(entity.id))
        .trackingId(TrackingId(entity.trackingId))
        .customerId(CustomerId(entity.customerId))
        .restaurantId(RestaurantId(entity.restaurantId))
        .price(Money(entity.price))
        .status(entity.orderStatus)
        .deliveryAddress(StreetAddress(entity.street, entity.postalCode, entity.city))
        .notes(entity.notes?.let { columnToNotes(jsonMapper.readColumn(it, OrderNotesColumn::class.java)) })
        .failureMessages(jsonMapper.readColumn(entity.failureMessages, STRING_LIST))
        .items(items.sortedBy { it.id.id }.map(::entityToOrderItem))
        .build()

    private fun entityToOrderItem(entity: OrderItemEntity): OrderItem = OrderItem.builder()
        .id(OrderItemId(entity.id.id))
        .product(Product(ProductId(entity.productId), entity.productName, Money(entity.price)))
        .quantity(entity.quantity)
        .price(Money(entity.price))
        .subTotal(Money(entity.subTotal))
        .build()

    private fun notesToColumn(notes: OrderNotes) = OrderNotesColumn(
        text = notes.text,
        allergies = notes.allergies,
        dietaryPreferences = notes.dietaryPreferences,
        itemInstructions = notes.itemInstructions.map { ItemInstructionColumn(it.item, it.instruction) },
        spiceLevel = notes.spiceLevel,
        deliveryInstructions = notes.deliveryInstructions,
        contactlessDelivery = notes.contactlessDelivery,
        requiresStaffReview = notes.requiresStaffReview,
        reviewReasons = notes.reviewReasons,
        source = notes.source,
    )

    private fun columnToNotes(column: OrderNotesColumn) = OrderNotes.restore(
        text = column.text,
        allergies = column.allergies,
        dietaryPreferences = column.dietaryPreferences,
        itemInstructions = column.itemInstructions.map { ItemInstruction(it.item, it.instruction) },
        spiceLevel = column.spiceLevel,
        deliveryInstructions = column.deliveryInstructions,
        contactlessDelivery = column.contactlessDelivery,
        requiresStaffReview = column.requiresStaffReview,
        reviewReasons = column.reviewReasons,
        source = column.source,
    )

    private companion object {
        private val STRING_LIST: Argument<List<String>> = Argument.listOf(String::class.java)
    }
}
