package de.aamir.foodordering.order.datasource.view.repository

import de.aamir.foodordering.order.datasource.view.entity.CustomerViewEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.MYSQL)
interface CustomerViewJdbcRepository : CrudRepository<CustomerViewEntity, UUID>
