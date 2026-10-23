package de.aamir.foodordering.order.datasource.view.adapter

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.order.application.ports.output.repository.CustomerRepository
import de.aamir.foodordering.order.datasource.view.repository.CustomerViewJdbcRepository
import de.aamir.foodordering.order.domain.entity.Customer
import jakarta.inject.Singleton

@Singleton
class CustomerRepositoryImpl(private val customers: CustomerViewJdbcRepository) : CustomerRepository {

    override fun findCustomer(customerId: CustomerId): Customer? =
        customers.findById(customerId.value).orElse(null)?.let { Customer(CustomerId(it.id)) }
}
