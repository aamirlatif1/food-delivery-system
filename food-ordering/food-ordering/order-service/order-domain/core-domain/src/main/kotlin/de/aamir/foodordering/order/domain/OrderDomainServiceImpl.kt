package de.aamir.foodordering.order.domain

import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.Restaurant
import de.aamir.foodordering.order.domain.event.OrderCancelledEvent
import de.aamir.foodordering.order.domain.event.OrderCreatedEvent
import de.aamir.foodordering.order.domain.event.OrderPaidEvent
import de.aamir.foodordering.order.domain.exception.OrderDomainException
import org.slf4j.LoggerFactory
import java.time.Clock

class OrderDomainServiceImpl(private val clock: Clock) : OrderDomainService {

    override fun validateAndInitiateOrder(order: Order, restaurant: Restaurant): OrderCreatedEvent {
        validateRestaurant(order, restaurant)
        confirmProducts(order, restaurant)
        order.validateOrder()
        order.initializeOrder()
        log.info("Order {} is initiated for restaurant {}", order.id.value, restaurant.id.value)
        return OrderCreatedEvent(order, clock.instant())
    }

    override fun payOrder(order: Order): OrderPaidEvent {
        order.pay()
        log.info("Order {} is paid", order.id.value)
        return OrderPaidEvent(order, clock.instant())
    }

    override fun approveOrder(order: Order) {
        order.approve()
        log.info("Order {} is approved", order.id.value)
    }

    override fun cancelOrderPayment(order: Order, failureMessages: List<String>): OrderCancelledEvent {
        order.initCancel(failureMessages)
        log.info("Payment for order {} is being cancelled", order.id.value)
        return OrderCancelledEvent(order, clock.instant())
    }

    override fun cancelOrder(order: Order, failureMessages: List<String>) {
        order.cancel(failureMessages)
        log.info("Order {} is cancelled", order.id.value)
    }

    private fun validateRestaurant(order: Order, restaurant: Restaurant) {
        if (order.restaurantId != restaurant.id) {
            throw OrderDomainException(
                "Order is for restaurant ${order.restaurantId.value}, not ${restaurant.id.value}",
            )
        }
        if (!restaurant.active) {
            throw OrderDomainException("Restaurant ${restaurant.id.value} is currently not active")
        }
    }

    // Every ordered product must be on the menu and available; the item then takes the
    // menu's product, so validateOrder() checks the item price against the menu price.
    private fun confirmProducts(order: Order, restaurant: Restaurant) {
        order.items.forEach { item ->
            val menuProduct = restaurant.findProduct(item.product.id)
                ?: throw OrderDomainException(
                    "Product ${item.product.id.value} is not on the menu of restaurant ${restaurant.id.value}",
                )
            if (!menuProduct.available) {
                throw OrderDomainException("Product '${menuProduct.name}' is not available")
            }
            item.confirmProduct(menuProduct)
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OrderDomainServiceImpl::class.java)
    }
}
