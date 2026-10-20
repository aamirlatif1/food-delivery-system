package de.aamir.foodordering.order.application

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.CreateOrderResponse
import de.aamir.foodordering.order.application.dto.track.TrackOrderQuery
import de.aamir.foodordering.order.application.dto.track.TrackOrderResponse
import de.aamir.foodordering.order.application.mapper.OrderDataMapper
import de.aamir.foodordering.order.application.outbox.scheduler.payment.PaymentOutboxHelper
import de.aamir.foodordering.order.application.ports.input.service.OrderApplicationService
import de.aamir.foodordering.order.application.ports.output.repository.CustomerRepository
import de.aamir.foodordering.order.application.ports.output.repository.OrderRepository
import de.aamir.foodordering.order.application.ports.output.repository.RestaurantRepository
import de.aamir.foodordering.order.application.saga.SagaStatusMapper
import de.aamir.foodordering.order.domain.OrderDomainService
import de.aamir.foodordering.order.domain.entity.Restaurant
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import de.aamir.foodordering.order.domain.exception.OrderNotFoundException
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import java.util.UUID

@Singleton
// open: Micronaut proxies @Transactional beans, and KSP does not see the all-open plugin.
open class OrderApplicationServiceImpl(
    private val orderDomainService: OrderDomainService,
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val restaurantRepository: RestaurantRepository,
    private val paymentOutboxHelper: PaymentOutboxHelper,
) : OrderApplicationService {

    // Saves the PENDING order and its payment_outbox row (saga STARTED) in one transaction.
    @Transactional
    override fun createOrder(@Valid command: CreateOrderCommand): CreateOrderResponse {
        checkCustomer(CustomerId(command.customerId))
        val restaurant = findRestaurant(RestaurantId(command.restaurantId))
        val order = OrderDataMapper.createOrderCommandToOrder(command)
        val event = orderDomainService.validateAndInitiateOrder(order, restaurant)
        orderRepository.save(order)
        paymentOutboxHelper.saveNew(
            payload = OrderDataMapper.orderCreatedEventToPaymentPayload(event),
            orderStatus = order.status,
            sagaStatus = SagaStatusMapper.fromOrderStatus(order.status),
            sagaId = UUID.randomUUID(),
        )
        log.info("Order {} is created", order.id.value)
        return OrderDataMapper.orderToCreateOrderResponse(order, "Order created")
    }

    @Transactional(readOnly = true)
    override fun trackOrder(@Valid query: TrackOrderQuery): TrackOrderResponse {
        val order = orderRepository.findByTrackingId(TrackingId(query.orderTrackingId))
            ?: throw OrderNotFoundException("Could not find order with tracking id ${query.orderTrackingId}")
        return OrderDataMapper.orderToTrackOrderResponse(order)
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
        private val log = LoggerFactory.getLogger(OrderApplicationServiceImpl::class.java)
    }
}
