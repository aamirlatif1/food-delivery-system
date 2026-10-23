package de.aamir.foodordering.order.datasource.order.adapter

import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.order.application.ports.output.repository.OrderRepository
import de.aamir.foodordering.order.datasource.order.entity.OrderEntity
import de.aamir.foodordering.order.datasource.order.mapper.OrderDataAccessMapper
import de.aamir.foodordering.order.datasource.order.repository.OrderItemJdbcRepository
import de.aamir.foodordering.order.datasource.order.repository.OrderJdbcRepository
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import jakarta.inject.Singleton

@Singleton
class OrderRepositoryImpl(
    private val orders: OrderJdbcRepository,
    private val orderItems: OrderItemJdbcRepository,
    private val mapper: OrderDataAccessMapper,
) : OrderRepository {

    // Items never change after creation, so only a new order writes them. Order carries no
    // version, so an update cannot detect a change made since the order was read; in the saga the
    // outbox row's optimistic lock rejects the losing handler instead (REQUIREMENTS §6.2).
    override fun save(order: Order): Order {
        val stored = orders.findById(order.id.value).orElse(null)
        if (stored == null) {
            orders.save(mapper.orderToEntity(order, version = 0))
            orderItems.saveAll(mapper.orderItemsToEntities(order))
        } else {
            orders.update(mapper.orderToEntity(order, version = stored.version))
        }
        return order
    }

    override fun findById(orderId: OrderId): Order? =
        orders.findById(orderId.value).orElse(null)?.let(::toOrder)

    override fun findByTrackingId(trackingId: TrackingId): Order? =
        orders.findByTrackingId(trackingId.value)?.let(::toOrder)

    private fun toOrder(entity: OrderEntity): Order =
        mapper.entityToOrder(entity, orderItems.findByIdOrderId(entity.id))
}
