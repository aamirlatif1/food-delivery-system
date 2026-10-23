package de.aamir.foodordering.order.datasource.common

import io.micronaut.core.convert.ConversionContext
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import io.micronaut.data.model.runtime.convert.AttributeConverter
import jakarta.inject.Singleton
import java.nio.ByteBuffer
import java.util.UUID

// Stores a UUID as BINARY(16), most significant bits first (REQUIREMENTS §6.4).
@Singleton
class UuidBinaryConverter : AttributeConverter<UUID, ByteArray> {

    override fun convertToPersistedValue(entityValue: UUID?, context: ConversionContext): ByteArray? =
        entityValue?.let {
            ByteBuffer.allocate(16).putLong(it.mostSignificantBits).putLong(it.leastSignificantBits).array()
        }

    override fun convertToEntityValue(persistedValue: ByteArray?, context: ConversionContext): UUID? =
        persistedValue?.let {
            val buffer = ByteBuffer.wrap(it)
            UUID(buffer.long, buffer.long)
        }
}

// Marks a UUID property stored in a BINARY(16) column.
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@TypeDef(type = DataType.BYTE_ARRAY, converter = UuidBinaryConverter::class)
annotation class BinaryUuid
