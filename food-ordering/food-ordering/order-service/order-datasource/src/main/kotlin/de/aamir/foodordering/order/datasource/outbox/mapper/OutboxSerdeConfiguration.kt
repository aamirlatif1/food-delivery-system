package de.aamir.foodordering.order.datasource.outbox.mapper

import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventNotes
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventPayload
import de.aamir.foodordering.order.application.outbox.model.approval.OrderApprovalEventProduct
import de.aamir.foodordering.order.application.outbox.model.payment.OrderPaymentEventPayload
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import io.micronaut.serde.annotation.SerdeImport

// Outbox payloads are stored as JSON in their application form, imported here to keep JSON
// annotations out of the application layer.
@SerdeImport(OrderPaymentEventPayload::class)
@SerdeImport(OrderApprovalEventPayload::class)
@SerdeImport(OrderApprovalEventProduct::class)
@SerdeImport(OrderApprovalEventNotes::class)
@SerdeImport(ItemInstruction::class)
class OutboxSerdeConfiguration
