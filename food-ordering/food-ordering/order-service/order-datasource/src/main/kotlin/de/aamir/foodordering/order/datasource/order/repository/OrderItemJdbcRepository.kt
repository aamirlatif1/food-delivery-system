package de.aamir.foodordering.order.datasource.order.repository

import de.aamir.foodordering.order.datasource.order.entity.OrderItemEntity
import de.aamir.foodordering.order.datasource.order.entity.OrderItemEntityId
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.MYSQL)
interface OrderItemJdbcRepository : CrudRepository<OrderItemEntity, OrderItemEntityId> {

    fun findByIdOrderId(orderId: UUID): List<OrderItemEntity>
}
