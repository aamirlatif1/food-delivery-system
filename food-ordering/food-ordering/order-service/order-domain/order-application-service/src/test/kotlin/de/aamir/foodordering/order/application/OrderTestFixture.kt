package de.aamir.foodordering.order.application

import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.Money
import de.aamir.foodordering.common.domain.valueobject.OrderApprovalStatus
import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import de.aamir.foodordering.common.domain.valueobject.ProductId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.OrderAddressDto
import de.aamir.foodordering.order.application.dto.create.OrderItemDto
import de.aamir.foodordering.order.application.dto.message.PaymentResponse
import de.aamir.foodordering.order.application.dto.message.RestaurantApprovalResponse
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.outbox.scheduler.approval.ApprovalOutboxHelper
import de.aamir.foodordering.order.application.outbox.scheduler.payment.PaymentOutboxHelper
import de.aamir.foodordering.order.application.saga.OrderApprovalSaga
import de.aamir.foodordering.order.application.saga.OrderPaymentSaga
import de.aamir.foodordering.order.domain.OrderDomainServiceImpl
import de.aamir.foodordering.order.domain.entity.Customer
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.Product
import de.aamir.foodordering.order.domain.entity.Restaurant
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

// Wires the application layer by hand with in-memory ports and a fixed clock.
class OrderTestFixture {
    val now: Instant = Instant.parse("2026-10-19T10:15:30Z")
    val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    val customerId: CustomerId = CustomerId.generate()
    val restaurantId: RestaurantId = RestaurantId.generate()
    val pizza = Product(ProductId.generate(), "Pizza", Money.of("10.00"))
    val pasta = Product(ProductId.generate(), "Pasta", Money.of("8.50"), available = false)

    val orderRepository = InMemoryOrderRepository()
    val customerRepository = InMemoryCustomerRepository(Customer(customerId))
    val restaurantRepository = InMemoryRestaurantRepository(Restaurant(restaurantId, active = true, products = listOf(pizza, pasta)))
    val paymentOutboxRepository = InMemoryPaymentOutboxRepository()
    val approvalOutboxRepository = InMemoryApprovalOutboxRepository()

    private val domainService = OrderDomainServiceImpl(clock)
    val paymentOutboxHelper = PaymentOutboxHelper(paymentOutboxRepository, clock)
    val approvalOutboxHelper = ApprovalOutboxHelper(approvalOutboxRepository, clock)
    private val sagaHelper = OrderSagaHelper(orderRepository)

    val orderApplicationService = OrderApplicationServiceImpl(
        domainService,
        orderRepository,
        customerRepository,
        restaurantRepository,
        paymentOutboxHelper,
    )
    val paymentSaga = OrderPaymentSaga(domainService, sagaHelper, paymentOutboxHelper, approvalOutboxHelper)
    val approvalSaga = OrderApprovalSaga(domainService, sagaHelper, paymentOutboxHelper, approvalOutboxHelper)

    fun createOrderCommand(
        customerId: UUID = this.customerId.value,
        restaurantId: UUID = this.restaurantId.value,
        productId: UUID = pizza.id.value,
        price: String = "20.00",
    ) = CreateOrderCommand(
        customerId = customerId,
        restaurantId = restaurantId,
        price = BigDecimal(price),
        items = listOf(OrderItemDto(productId, quantity = 2, price = BigDecimal("10.00"), subTotal = BigDecimal("20.00"))),
        address = OrderAddressDto("Hauptstr. 1", "10115", "Berlin"),
    )

    // Creates an order and returns it with the saga id of its payment outbox row.
    fun createOrder(): Pair<Order, UUID> {
        orderApplicationService.createOrder(createOrderCommand())
        val order = orderRepository.orders.values.last()
        return order to paymentOutboxRepository.rows.values.single { it.payload.orderId == order.id.value }.sagaId
    }

    fun paymentOutbox(sagaId: UUID): List<OrderPaymentOutboxMessage> =
        paymentOutboxRepository.rows.values.filter { it.sagaId == sagaId }

    fun paymentResponse(order: Order, sagaId: UUID, status: PaymentStatus, failureMessages: List<String> = emptyList()) =
        PaymentResponse(
            sagaId = sagaId,
            orderId = order.id.value,
            paymentId = UUID.randomUUID(),
            customerId = order.customerId.value,
            price = order.price.amount,
            createdAt = now,
            paymentStatus = status,
            failureMessages = failureMessages,
        )

    fun approvalResponse(order: Order, sagaId: UUID, status: OrderApprovalStatus, failureMessages: List<String> = emptyList()) =
        RestaurantApprovalResponse(
            sagaId = sagaId,
            orderId = order.id.value,
            restaurantId = order.restaurantId.value,
            createdAt = now,
            orderApprovalStatus = status,
            failureMessages = failureMessages,
        )
}
