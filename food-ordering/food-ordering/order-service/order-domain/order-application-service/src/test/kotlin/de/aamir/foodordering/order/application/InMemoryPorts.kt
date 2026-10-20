package de.aamir.foodordering.order.application

import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException
import de.aamir.foodordering.order.application.ports.output.ai.OrderNotesInterpreter
import de.aamir.foodordering.common.domain.valueobject.CustomerId
import de.aamir.foodordering.common.domain.valueobject.OrderId
import de.aamir.foodordering.common.domain.valueobject.RestaurantId
import de.aamir.foodordering.order.application.outbox.OutboxStatus
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.ports.output.message.publisher.PaymentRequestMessagePublisher
import de.aamir.foodordering.order.application.ports.output.message.publisher.RestaurantApprovalRequestMessagePublisher
import de.aamir.foodordering.order.application.ports.output.repository.ApprovalOutboxRepository
import de.aamir.foodordering.order.application.ports.output.repository.CustomerRepository
import de.aamir.foodordering.order.application.ports.output.repository.OrderRepository
import de.aamir.foodordering.order.application.ports.output.repository.PaymentOutboxRepository
import de.aamir.foodordering.order.application.ports.output.repository.RestaurantRepository
import de.aamir.foodordering.order.application.saga.SagaStatus
import de.aamir.foodordering.order.domain.entity.Customer
import de.aamir.foodordering.order.domain.entity.Order
import de.aamir.foodordering.order.domain.entity.Restaurant
import de.aamir.foodordering.order.domain.valueobject.TrackingId
import java.time.Instant
import java.util.UUID

class InMemoryOrderRepository : OrderRepository {
    val orders = linkedMapOf<OrderId, Order>()

    override fun save(order: Order): Order = order.also { orders[it.id] = it }

    override fun findById(orderId: OrderId): Order? = orders[orderId]

    override fun findByTrackingId(trackingId: TrackingId): Order? = orders.values.firstOrNull { it.trackingId == trackingId }
}

class InMemoryCustomerRepository(vararg customers: Customer) : CustomerRepository {
    private val customers = customers.associateBy { it.id }

    override fun findCustomer(customerId: CustomerId): Customer? = customers[customerId]
}

class InMemoryRestaurantRepository(vararg restaurants: Restaurant) : RestaurantRepository {
    private val restaurants = restaurants.associateBy { it.id }

    override fun findRestaurantInformation(restaurantId: RestaurantId): Restaurant? = restaurants[restaurantId]
}

// Shared outbox behaviour of the real tables: unique (type, sagaId, sagaStatus) and an
// optimistic lock on version.
abstract class InMemoryOutbox<M : Any>(
    private val id: (M) -> UUID,
    private val key: (M) -> Triple<String, UUID, SagaStatus>,
    private val version: (M) -> Int,
    private val withVersion: (M, Int) -> M,
    private val type: (M) -> String,
    private val outboxStatus: (M) -> OutboxStatus,
    private val sagaId: (M) -> UUID,
    private val sagaStatus: (M) -> SagaStatus,
    private val createdAt: (M) -> Instant,
) {
    val rows = linkedMapOf<UUID, M>()

    fun save(message: M): M {
        check(rows.values.none { id(it) != id(message) && key(it) == key(message) }) { "Duplicate outbox row ${key(message)}" }
        val stored = rows[id(message)]
        if (stored != null) {
            check(version(stored) == version(message)) { "Optimistic lock failure on outbox row ${id(message)}" }
        }
        return withVersion(message, version(message) + if (stored == null) 0 else 1).also { rows[id(it)] = it }
    }

    fun find(type: String, outboxStatus: OutboxStatus, sagaStatuses: Collection<SagaStatus>): List<M> =
        rows.values.filter { type(it) == type && outboxStatus(it) == outboxStatus && sagaStatus(it) in sagaStatuses }

    fun find(type: String, sagaId: UUID, sagaStatuses: Collection<SagaStatus>): M? =
        rows.values.firstOrNull { type(it) == type && sagaId(it) == sagaId && sagaStatus(it) in sagaStatuses }

    fun delete(type: String, outboxStatus: OutboxStatus, createdBefore: Instant): Int {
        val doomed = rows.values.filter { type(it) == type && outboxStatus(it) == outboxStatus && createdAt(it) < createdBefore }
        doomed.forEach { rows.remove(id(it)) }
        return doomed.size
    }
}

class InMemoryPaymentOutboxRepository :
    InMemoryOutbox<OrderPaymentOutboxMessage>(
        id = { it.id },
        key = { Triple(it.type, it.sagaId, it.sagaStatus) },
        version = { it.version },
        withVersion = { m, v -> m.copy(version = v) },
        type = { it.type },
        outboxStatus = { it.outboxStatus },
        sagaId = { it.sagaId },
        sagaStatus = { it.sagaStatus },
        createdAt = { it.createdAt },
    ),
    PaymentOutboxRepository {
    override fun findByTypeAndOutboxStatusAndSagaStatus(type: String, outboxStatus: OutboxStatus, sagaStatuses: Collection<SagaStatus>) =
        find(type, outboxStatus, sagaStatuses)

    override fun findByTypeAndSagaIdAndSagaStatus(type: String, sagaId: UUID, sagaStatuses: Collection<SagaStatus>) =
        find(type, sagaId, sagaStatuses)

    override fun deleteByTypeAndOutboxStatusAndCreatedAtBefore(type: String, outboxStatus: OutboxStatus, createdBefore: Instant) =
        delete(type, outboxStatus, createdBefore)
}

class InMemoryApprovalOutboxRepository :
    InMemoryOutbox<OrderApprovalOutboxMessage>(
        id = { it.id },
        key = { Triple(it.type, it.sagaId, it.sagaStatus) },
        version = { it.version },
        withVersion = { m, v -> m.copy(version = v) },
        type = { it.type },
        outboxStatus = { it.outboxStatus },
        sagaId = { it.sagaId },
        sagaStatus = { it.sagaStatus },
        createdAt = { it.createdAt },
    ),
    ApprovalOutboxRepository {
    override fun findByTypeAndOutboxStatusAndSagaStatus(type: String, outboxStatus: OutboxStatus, sagaStatuses: Collection<SagaStatus>) =
        find(type, outboxStatus, sagaStatuses)

    override fun findByTypeAndSagaIdAndSagaStatus(type: String, sagaId: UUID, sagaStatuses: Collection<SagaStatus>) =
        find(type, sagaId, sagaStatuses)

    override fun deleteByTypeAndOutboxStatusAndCreatedAtBefore(type: String, outboxStatus: OutboxStatus, createdBefore: Instant) =
        delete(type, outboxStatus, createdBefore)
}

class RecordingPaymentPublisher(var failing: Boolean = false) : PaymentRequestMessagePublisher {
    val published = mutableListOf<OrderPaymentOutboxMessage>()

    override fun publish(message: OrderPaymentOutboxMessage) {
        check(!failing) { "Broker unavailable" }
        published += message
    }
}

class RecordingApprovalPublisher(var failing: Boolean = false) : RestaurantApprovalRequestMessagePublisher {
    val published = mutableListOf<OrderApprovalOutboxMessage>()

    override fun publish(message: OrderApprovalOutboxMessage) {
        check(!failing) { "Broker unavailable" }
        published += message
    }
}

// Answers with the given interpretation, or throws when it is set to fail.
class FakeOrderNotesInterpreter(
    var answer: OrderNotesInterpretation = OrderNotesInterpretation.EMPTY,
    var failure: OrderNotesInterpretationException? = null,
) : OrderNotesInterpreter {
    val received = mutableListOf<String>()

    override fun interpret(notes: String): OrderNotesInterpretation {
        received += notes
        failure?.let { throw it }
        return answer
    }
}
