package de.aamir.foodordering.order.application.saga

import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import de.aamir.foodordering.order.application.OrderSagaHelper
import de.aamir.foodordering.order.application.dto.message.PaymentResponse
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
open class OrderPaymentSaga(
    private val orderDomainService: OrderDomainService,
    private val orderSagaHelper: OrderSagaHelper,
    private val paymentOutboxHelper: PaymentOutboxHelper,
    private val approvalOutboxHelper: ApprovalOutboxHelper,
) : SagaStep<PaymentResponse> {

    // PaymentCompleted: order PAID, payment outbox PROCESSING, approval request STARTED.
    @Transactional
    override fun process(data: PaymentResponse) {
        val outboxMessage = paymentOutboxHelper.findBySagaIdAndSagaStatus(data.sagaId, SagaStatus.STARTED)
        if (outboxMessage == null) {
            log.info("Payment response for saga {} is already processed", data.sagaId)
            return
        }
        val order = orderSagaHelper.findOrder(data.orderId)
        val event = orderDomainService.payOrder(order)
        orderSagaHelper.saveOrder(order)

        val sagaStatus = SagaStatusMapper.fromOrderStatus(order.status)
        paymentOutboxHelper.updateSagaStatus(outboxMessage, order.status, sagaStatus)
        approvalOutboxHelper.saveNew(
            payload = OrderDataMapper.orderPaidEventToApprovalPayload(event),
            orderStatus = order.status,
            sagaStatus = sagaStatus,
            sagaId = data.sagaId,
        )
    }

    // PaymentFailed (saga FAILED) or PaymentCancelled after a refund (saga COMPENSATED): order CANCELLED.
    @Transactional
    override fun rollback(data: PaymentResponse) {
        val expected = SagaStatusMapper.expectedForPaymentResponse(data.paymentStatus)
        val outboxMessage = paymentOutboxHelper.findBySagaIdAndSagaStatus(data.sagaId, expected)
        if (outboxMessage == null) {
            log.info("Payment {} response for saga {} is already processed", data.paymentStatus, data.sagaId)
            return
        }
        val order = orderSagaHelper.findOrder(data.orderId)
        orderDomainService.cancelOrder(order, data.failureMessages)
        orderSagaHelper.saveOrder(order)

        val sagaStatus = SagaStatusMapper.afterPaymentRollback(data.paymentStatus, order.status)
        paymentOutboxHelper.updateSagaStatus(outboxMessage, order.status, sagaStatus)
        if (data.paymentStatus == PaymentStatus.CANCELLED) {
            approvalOutboxHelper.findBySagaIdAndSagaStatus(data.sagaId, SagaStatus.COMPENSATING)
                ?.let { approvalOutboxHelper.updateSagaStatus(it, order.status, sagaStatus) }
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OrderPaymentSaga::class.java)
    }
}
