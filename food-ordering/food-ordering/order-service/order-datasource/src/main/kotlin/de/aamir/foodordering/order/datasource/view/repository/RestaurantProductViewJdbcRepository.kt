package de.aamir.foodordering.order.datasource.view.repository

import de.aamir.foodordering.order.datasource.view.entity.RestaurantProductViewEntity
import de.aamir.foodordering.order.datasource.view.entity.RestaurantProductViewId
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.MYSQL)
interface RestaurantProductViewJdbcRepository : CrudRepository<RestaurantProductViewEntity, RestaurantProductViewId> {

    fun findByIdRestaurantId(restaurantId: UUID): List<RestaurantProductViewEntity>
}
