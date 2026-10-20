package de.aamir.foodordering.kafka.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord

// Implemented by the @KafkaListener beans in each service's messaging module, e.g.
//
//   @KafkaListener(
//       groupId = "...",
//       offsetStrategy = OffsetStrategy.SYNC_PER_RECORD,
//       errorStrategy = ErrorStrategy(value = ErrorStrategyValue.RETRY_TOPIC_ON_ERROR, ...),
//   )
//   class PaymentResponseKafkaListener : KafkaConsumer<String, ByteArray> {
//       @Topic("payment-response-topic")
//       override fun receive(record: ConsumerRecord<String, ByteArray>) { ... }
//   }
//
// One record at a time: Micronaut's retry topics do not support batch listeners.
// OffsetStrategy.SYNC_PER_RECORD commits each record's offset only after receive returns, i.e.
// after the handler's transaction commits.
interface KafkaConsumer<K : Any, V : Any> {
    fun receive(record: ConsumerRecord<K, V>)
}
