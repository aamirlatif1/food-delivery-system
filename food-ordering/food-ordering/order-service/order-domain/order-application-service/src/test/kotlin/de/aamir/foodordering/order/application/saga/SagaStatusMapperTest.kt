package de.aamir.foodordering.order.application.saga

import de.aamir.foodordering.common.domain.valueobject.OrderStatus
import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class SagaStatusMapperTest {

    @Test
    fun `maps every order status to its saga status`() {
        assertEquals(
            mapOf(
                OrderStatus.PENDING to SagaStatus.STARTED,
                OrderStatus.PAID to SagaStatus.PROCESSING,
                OrderStatus.APPROVED to SagaStatus.SUCCEEDED,
                OrderStatus.CANCELING to SagaStatus.COMPENSATING,
                OrderStatus.CANCELLED to SagaStatus.COMPENSATED,
            ),
            OrderStatus.entries.associateWith(SagaStatusMapper::fromOrderStatus),
        )
    }

    @Test
    fun `a failed payment fails the saga, a cancelled payment compensates it`() {
        assertEquals(SagaStatus.FAILED, SagaStatusMapper.afterPaymentRollback(PaymentStatus.FAILED, OrderStatus.CANCELLED))
        assertEquals(SagaStatus.COMPENSATED, SagaStatusMapper.afterPaymentRollback(PaymentStatus.CANCELLED, OrderStatus.CANCELLED))
    }
}
