package de.aamir.foodordering.order.application.saga

import de.aamir.foodordering.order.application.OrderSagaHelper
import de.aamir.foodordering.order.application.dto.message.RestaurantApprovalResponse
import de.aamir.foodordering.order.application.mapper.OrderDataMapper
import de.aamir.foodordering.order.application.outbox.scheduler.approval.ApprovalOutboxHelper
import de.aamir.foodordering.order.application.outbox.scheduler.payment.PaymentOutboxHelper
import de.aamir.foodordering.order.domain.OrderDomainService
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

// Each handler first looks up the outbox row in the saga status it expects. If there is none,
// the response was already handled and is skipped (REQUIREMENTS §6.2).
@Singleton
// open: Micronaut proxies @Transactional beans, and KSP does not see the all-open plugin.
open class OrderApprovalSaga(
    private val orderDomainService: OrderDomainService,
    private val orderSagaHelper: OrderSagaHelper,
    private val paymentOutboxHelper: PaymentOutboxHelper,
    private val approvalOutboxHelper: ApprovalOutboxHelper,
) : SagaStep<RestaurantApprovalResponse> {

    // OrderApproved: order APPROVED, both outbox rows SUCCEEDED.
    @Transactional
    override fun process(data: RestaurantApprovalResponse) {
        val outboxMessage = approvalOutboxHelper.findBySagaIdAndSagaStatus(data.sagaId, SagaStatus.PROCESSING)
        if (outboxMessage == null) {
            log.info("Approval response for saga {} is already processed", data.sagaId)
            return
        }
        val order = orderSagaHelper.findOrder(data.orderId)
        orderDomainService.approveOrder(order)
        orderSagaHelper.saveOrder(order)

        val sagaStatus = SagaStatusMapper.fromOrderStatus(order.status)
        approvalOutboxHelper.updateSagaStatus(outboxMessage, order.status, sagaStatus)
        val paymentOutboxMessage = checkNotNull(
            paymentOutboxHelper.findBySagaIdAndSagaStatus(data.sagaId, SagaStatus.PROCESSING),
        ) { "Payment outbox message for saga ${data.sagaId} is not in PROCESSING" }
        paymentOutboxHelper.updateSagaStatus(paymentOutboxMessage, order.status, sagaStatus)
    }

    // OrderRejected: order CANCELING, approval outbox COMPENSATING, and a new payment
    // request with paymentOrderStatus CANCELLED so payment-service refunds the credit.
    @Transactional
    override fun rollback(data: RestaurantApprovalResponse) {
        val outboxMessage = approvalOutboxHelper.findBySagaIdAndSagaStatus(data.sagaId, SagaStatus.PROCESSING)
        if (outboxMessage == null) {
            log.info("Approval response for saga {} is already processed", data.sagaId)
            return
        }
        val order = orderSagaHelper.findOrder(data.orderId)
        val event = orderDomainService.cancelOrderPayment(order, data.failureMessages)
        orderSagaHelper.saveOrder(order)

        val sagaStatus = SagaStatusMapper.fromOrderStatus(order.status)
        approvalOutboxHelper.updateSagaStatus(outboxMessage, order.status, sagaStatus)
        paymentOutboxHelper.saveNew(
            payload = OrderDataMapper.orderCancelledEventToPaymentPayload(event),
            orderStatus = order.status,
            sagaStatus = sagaStatus,
            sagaId = data.sagaId,
        )
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OrderApprovalSaga::class.java)
    }
}
