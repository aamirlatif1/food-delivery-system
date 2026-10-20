package de.aamir.foodordering.order.application

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.mapper.OrderDataMapper
import de.aamir.foodordering.order.application.outbox.scheduler.payment.PaymentOutboxHelper
import de.aamir.foodordering.order.application.ports.output.repository.CustomerRepository
import de.aamir.foodordering.order.application.ports.output.repository.OrderRepository
import de.aamir.foodordering.order.application.ports.output.repository.RestaurantRepository
import de.aamir.foodordering.order.application.saga.SagaStatusMapper
import de.aamir.foodordering.order.domain.OrderDomainService
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.Restaurant
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.valueobject.OrderNotes
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID

// The transactional part of createOrder, in its own bean so that the notes are interpreted
// before the transaction starts: an AI call must not hold a database connection.
@Singleton
// open: Micronaut proxies @Transactional beans, and KSP does not see the all-open plugin.
open class OrderCreateHelper(
    private val orderDomainService: OrderDomainService,
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val restaurantRepository: RestaurantRepository,
    private val paymentOutboxHelper: PaymentOutboxHelper,
) {

    // Saves the PENDING order and its payment_outbox row (saga STARTED) in one transaction.
    @Transactional
    open fun persistOrder(command: CreateOrderCommand, notes: OrderNotes?): Order {
        checkCustomer(CustomerId(command.customerId))
        val restaurant = findRestaurant(RestaurantId(command.restaurantId))
        val order = OrderDataMapper.createOrderCommandToOrder(command, notes)
        val event = orderDomainService.validateAndInitiateOrder(order, restaurant)
        orderRepository.save(order)
        paymentOutboxHelper.saveNew(
            payload = OrderDataMapper.orderCreatedEventToPaymentPayload(event),
            orderStatus = order.status,
            sagaStatus = SagaStatusMapper.fromOrderStatus(order.status),
            sagaId = UUID.randomUUID(),
        )
        log.info("Order {} is created", order.id.value)
        return order
    }

    private fun checkCustomer(customerId: CustomerId) {
        if (customerRepository.findCustomer(customerId) == null) {
            throw OrderDomainException("Could not find customer with id ${customerId.value}")
        }
    }

    private fun findRestaurant(restaurantId: RestaurantId): Restaurant =
        restaurantRepository.findRestaurantInformation(restaurantId)
            ?: throw OrderDomainException("Could not find restaurant with id ${restaurantId.value}")

    private companion object {
        private val log = LoggerFactory.getLogger(OrderCreateHelper::class.java)
    }
}
