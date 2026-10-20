package de.aamir.foodordering.order.application.dto

import de.aamir.foodordering.order.application.FakeOrderNotesInterpreter
import de.aamir.foodordering.order.application.InMemoryApprovalOutboxRepository
import de.aamir.foodordering.order.application.InMemoryCustomerRepository
import de.aamir.foodordering.order.application.InMemoryOrderRepository
import de.aamir.foodordering.order.application.InMemoryPaymentOutboxRepository
import de.aamir.foodordering.order.application.InMemoryRestaurantRepository
import de.aamir.foodordering.order.application.OrderTestFixture
import de.aamir.foodordering.order.application.dto.create.CreateOrderCommand
import de.aamir.foodordering.order.application.dto.create.OrderAddressDto
import de.aamir.foodordering.order.application.dto.create.OrderItemDto
import de.aamir.foodordering.order.application.ports.input.service.OrderApplicationService
import de.aamir.foodordering.order.domain.OrderDomainServiceImpl
import io.micronaut.context.ApplicationContext
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Runs in a real Micronaut context so the constraints and the @Valid interception are exercised.
class CreateOrderCommandValidationTest {

    private val fixture = OrderTestFixture()
    private val orderRepository = InMemoryOrderRepository()
    private val context: ApplicationContext = ApplicationContext.builder()
        .properties(mapOf("order-service.outbox.scheduler.enabled" to "false"))
        .singletons(
            fixture.clock,
            OrderDomainServiceImpl(fixture.clock),
            orderRepository,
            InMemoryCustomerRepository(),
            InMemoryRestaurantRepository(),
            InMemoryPaymentOutboxRepository(),
            InMemoryApprovalOutboxRepository(),
            FakeOrderNotesInterpreter(),
        )
        .start()
    private val validator = context.getBean(Validator::class.java)

    @AfterTest
    fun close() = context.close()

    private fun violations(command: CreateOrderCommand): Set<String> =
        validator.validate(command).map { it.propertyPath.toString() }.toSet()

    @Test
    fun `a well-formed command has no violations`() {
        assertTrue(violations(fixture.createOrderCommand()).isEmpty())
    }

    @Test
    fun `order price must be positive with at most two decimals`() {
        val command = fixture.createOrderCommand()
        assertEquals(setOf("price"), violations(command.copy(price = BigDecimal.ZERO)))
        assertEquals(setOf("price"), violations(command.copy(price = BigDecimal("20.001"))))
    }

    @Test
    fun `items must not be empty and each item must be well-formed`() {
        val command = fixture.createOrderCommand()
        val item = command.items.single()

        assertEquals(setOf("items"), violations(command.copy(items = emptyList())))
        assertEquals(setOf("notes"), violations(command.copy(notes = "x".repeat(1001))))
        assertEquals(
            setOf("items[0].quantity", "items[0].price", "items[0].subTotal"),
            violations(command.copy(items = listOf(item.copy(quantity = 0, price = BigDecimal("-1"), subTotal = BigDecimal.ZERO)))),
        )
    }

    @Test
    fun `address fields must not be blank or too long`() {
        val command = fixture.createOrderCommand()
        assertEquals(
            setOf("address.street", "address.postalCode", "address.city"),
            violations(command.copy(address = OrderAddressDto(" ", "", "x".repeat(51)))),
        )
    }

    @Test
    fun `the application service rejects an invalid command before doing any work`() {
        val service = context.getBean(OrderApplicationService::class.java)
        val invalid = fixture.createOrderCommand().copy(
            items = listOf(OrderItemDto(fixture.pizza.id.value, quantity = 0, price = BigDecimal("10.00"), subTotal = BigDecimal("0.00"))),
        )

        val error = assertFailsWith<ConstraintViolationException> { service.createOrder(invalid) }

        assertTrue(error.constraintViolations.any { it.propertyPath.toString().endsWith("quantity") })
        assertTrue(orderRepository.orders.isEmpty())
    }
}
