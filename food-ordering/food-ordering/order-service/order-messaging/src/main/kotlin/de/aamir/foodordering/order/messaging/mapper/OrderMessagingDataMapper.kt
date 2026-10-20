package de.aamir.foodordering.order.messaging.mapper

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import de.aamir.foodordering.common.domain.valueobject.OrderApprovalStatus
import de.aamir.foodordering.common.domain.valueobject.PaymentStatus
import de.aamir.foodordering.kafka.model.common.v1.Money
import de.aamir.foodordering.kafka.model.common.v1.money
import de.aamir.foodordering.kafka.model.payment.v1.PaymentOrderStatus as PaymentOrderStatusProto
import de.aamir.foodordering.kafka.model.payment.v1.PaymentRequest
import de.aamir.foodordering.kafka.model.payment.v1.PaymentResponse as PaymentResponseProto
import de.aamir.foodordering.kafka.model.payment.v1.PaymentStatus as PaymentStatusProto
import de.aamir.foodordering.kafka.model.payment.v1.paymentRequest
import de.aamir.foodordering.kafka.model.restaurant.v1.DietaryPreference as DietaryPreferenceProto
import de.aamir.foodordering.kafka.model.restaurant.v1.OrderApprovalStatus as OrderApprovalStatusProto
import de.aamir.foodordering.kafka.model.restaurant.v1.RestaurantApprovalRequest
import de.aamir.foodordering.kafka.model.restaurant.v1.RestaurantApprovalResponse as RestaurantApprovalResponseProto
import de.aamir.foodordering.kafka.model.restaurant.v1.RestaurantOrderStatus as RestaurantOrderStatusProto
import de.aamir.foodordering.kafka.model.restaurant.v1.SpiceLevel as SpiceLevelProto
import de.aamir.foodordering.kafka.model.restaurant.v1.itemInstruction
import de.aamir.foodordering.kafka.model.restaurant.v1.orderNotes
import de.aamir.foodordering.kafka.model.restaurant.v1.orderProduct
import de.aamir.foodordering.kafka.model.restaurant.v1.restaurantApprovalRequest
import de.aamir.foodordering.order.application.dto.message.PaymentResponse
import de.aamir.foodordering.order.application.dto.message.RestaurantApprovalResponse
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventNotes
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.approval.RestaurantOrderStatus
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentOutboxMessage
import de.aamir.foodordering.order.application.outbox.model.payment.PaymentOrderStatus
import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import de.aamir.foodordering.order.messaging.config.OrderMessagingProperties
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// Translates between the application's outbox rows and DTOs and the Protobuf contracts.
@Singleton
class OrderMessagingDataMapper(properties: OrderMessagingProperties) {

    private val currencyCode = properties.currencyCode

    fun toPaymentRequest(message: OrderPaymentOutboxMessage): PaymentRequest {
        val payload = message.payload
        return paymentRequest {
            sagaId = message.sagaId.toString()
            orderId = payload.orderId.toString()
            customerId = payload.customerId.toString()
            price = payload.price.toMoney()
            createdAt = payload.createdAt.toTimestamp()
            paymentOrderStatus = when (payload.paymentOrderStatus) {
                PaymentOrderStatus.PENDING -> PaymentOrderStatusProto.PAYMENT_ORDER_STATUS_PENDING
                PaymentOrderStatus.CANCELLED -> PaymentOrderStatusProto.PAYMENT_ORDER_STATUS_CANCELLED
            }
        }
    }

    fun toRestaurantApprovalRequest(message: OrderApprovalOutboxMessage): RestaurantApprovalRequest {
        val payload = message.payload
        return restaurantApprovalRequest {
            sagaId = message.sagaId.toString()
            orderId = payload.orderId.toString()
            restaurantId = payload.restaurantId.toString()
            price = payload.price.toMoney()
            products += payload.products.map {
                orderProduct {
                    productId = it.id.toString()
                    quantity = it.quantity
                    price = it.price.toMoney()
                }
            }
            createdAt = payload.createdAt.toTimestamp()
            restaurantOrderStatus = when (payload.restaurantOrderStatus) {
                RestaurantOrderStatus.PAID -> RestaurantOrderStatusProto.RESTAURANT_ORDER_STATUS_PAID
            }
            payload.notes?.let { notes = it.toProto() }
        }
    }

    fun toPaymentResponse(message: PaymentResponseProto): PaymentResponse =
        PaymentResponse(
            sagaId = message.sagaId.toUuid("saga_id"),
            orderId = message.orderId.toUuid("order_id"),
            paymentId = message.paymentId.toUuid("payment_id"),
            customerId = message.customerId.toUuid("customer_id"),
            price = message.price.toBigDecimal(),
            createdAt = message.createdAt.toInstant(),
            paymentStatus = when (message.paymentStatus) {
                PaymentStatusProto.PAYMENT_STATUS_COMPLETED -> PaymentStatus.COMPLETED
                PaymentStatusProto.PAYMENT_STATUS_CANCELLED -> PaymentStatus.CANCELLED
                PaymentStatusProto.PAYMENT_STATUS_FAILED -> PaymentStatus.FAILED
                else -> throw InvalidMessageException("Unknown payment status ${message.paymentStatusValue}")
            },
            failureMessages = message.failureMessagesList,
        )

    fun toRestaurantApprovalResponse(message: RestaurantApprovalResponseProto): RestaurantApprovalResponse =
        RestaurantApprovalResponse(
            sagaId = message.sagaId.toUuid("saga_id"),
            orderId = message.orderId.toUuid("order_id"),
            restaurantId = message.restaurantId.toUuid("restaurant_id"),
            createdAt = message.createdAt.toInstant(),
            orderApprovalStatus = when (message.orderApprovalStatus) {
                OrderApprovalStatusProto.ORDER_APPROVAL_STATUS_APPROVED -> OrderApprovalStatus.APPROVED
                OrderApprovalStatusProto.ORDER_APPROVAL_STATUS_REJECTED -> OrderApprovalStatus.REJECTED
                else -> throw InvalidMessageException("Unknown order approval status ${message.orderApprovalStatusValue}")
            },
            failureMessages = message.failureMessagesList,
        )

    private fun OrderApprovalEventNotes.toProto() = orderNotes {
        text = this@toProto.text
        allergies += this@toProto.allergies
        dietaryPreferences += this@toProto.dietaryPreferences.map { it.toProto() }
        itemInstructions += this@toProto.itemInstructions.map {
            itemInstruction {
                item = it.item
                instruction = it.instruction
            }
        }
        spiceLevel = this@toProto.spiceLevel.toProto()
        requiresStaffReview = this@toProto.requiresStaffReview
        reviewReasons += this@toProto.reviewReasons
    }

    private fun DietaryPreference.toProto(): DietaryPreferenceProto = when (this) {
        DietaryPreference.VEGETARIAN -> DietaryPreferenceProto.DIETARY_PREFERENCE_VEGETARIAN
        DietaryPreference.VEGAN -> DietaryPreferenceProto.DIETARY_PREFERENCE_VEGAN
        DietaryPreference.HALAL -> DietaryPreferenceProto.DIETARY_PREFERENCE_HALAL
        DietaryPreference.KOSHER -> DietaryPreferenceProto.DIETARY_PREFERENCE_KOSHER
        DietaryPreference.GLUTEN_FREE -> DietaryPreferenceProto.DIETARY_PREFERENCE_GLUTEN_FREE
        DietaryPreference.LACTOSE_FREE -> DietaryPreferenceProto.DIETARY_PREFERENCE_LACTOSE_FREE
    }

    // NOT_SPECIFIED is the proto's zero value.
    private fun SpiceLevel.toProto(): SpiceLevelProto = when (this) {
        SpiceLevel.NOT_SPECIFIED -> SpiceLevelProto.SPICE_LEVEL_UNSPECIFIED
        SpiceLevel.NONE -> SpiceLevelProto.SPICE_LEVEL_NONE
        SpiceLevel.MILD -> SpiceLevelProto.SPICE_LEVEL_MILD
        SpiceLevel.MEDIUM -> SpiceLevelProto.SPICE_LEVEL_MEDIUM
        SpiceLevel.HOT -> SpiceLevelProto.SPICE_LEVEL_HOT
    }

    // Domain amounts have scale 2, so they convert to cents exactly.
    private fun BigDecimal.toMoney(): Money = money {
        amountMinor = movePointRight(MINOR_UNIT_DIGITS).longValueExact()
        currencyCode = this@OrderMessagingDataMapper.currencyCode
    }

    private fun Money.toBigDecimal(): BigDecimal {
        if (currencyCode != this@OrderMessagingDataMapper.currencyCode) {
            throw InvalidMessageException("Unexpected currency '$currencyCode'")
        }
        return BigDecimal.valueOf(amountMinor, MINOR_UNIT_DIGITS)
    }

    private fun Instant.toTimestamp(): Timestamp = timestamp {
        seconds = epochSecond
        nanos = nano
    }

    private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())

    private fun String.toUuid(field: String): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw InvalidMessageException("$field is not a UUID: '$this'", e)
        }

    private companion object {
        private const val MINOR_UNIT_DIGITS = 2
    }
}
