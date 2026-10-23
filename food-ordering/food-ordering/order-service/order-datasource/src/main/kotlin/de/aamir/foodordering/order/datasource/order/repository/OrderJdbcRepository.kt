package de.aamir.foodordering.order.datasource.order.repository

import de.aamir.foodordering.order.datasource.order.entity.OrderEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.MYSQL)
interface OrderJdbcRepository : CrudRepository<OrderEntity, UUID> {

    fun findByTrackingId(trackingId: UUID): OrderEntity?
}
