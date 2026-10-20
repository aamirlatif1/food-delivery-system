package de.aamir.foodordering.order.ai.notes

import com.openai.core.JsonValue
import com.openai.models.ResponseFormatJsonSchema
import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel

// The JSON schema the model must answer in. strict = true makes OpenAI enforce it, so every field
// is required and no others are allowed; an optional value is nullable instead. The enums come
// from the application DTOs, so the schema cannot drift from them.
object OrderNotesSchema {

    const val NAME = "order_notes_interpretation"

    private val stringArray = mapOf("type" to "array", "items" to mapOf("type" to "string"))

    val properties: Map<String, Any> = mapOf(
        "allergies" to stringArray,
        "dietaryPreferences" to mapOf(
            "type" to "array",
            "items" to mapOf("type" to "string", "enum" to DietaryPreference.entries.map { it.name }),
        ),
        "itemInstructions" to mapOf(
            "type" to "array",
            "items" to strictObject(
                mapOf(
                    "item" to mapOf("type" to "string"),
                    "instruction" to mapOf("type" to "string"),
                ),
            ),
        ),
        "spiceLevel" to mapOf("type" to "string", "enum" to SpiceLevel.entries.map { it.name }),
        "deliveryInstructions" to mapOf("type" to listOf("string", "null")),
        "contactlessDelivery" to mapOf("type" to "boolean"),
        "requiresStaffReview" to mapOf("type" to "boolean"),
        "reviewReasons" to stringArray,
    )

    val responseFormat: ResponseFormatJsonSchema = ResponseFormatJsonSchema.builder()
        .jsonSchema(
            ResponseFormatJsonSchema.JsonSchema.builder()
                .name(NAME)
                .description("Structured interpretation of a customer's food order notes")
                .strict(true)
                .schema(
                    ResponseFormatJsonSchema.JsonSchema.Schema.builder()
                        .putAllAdditionalProperties(strictObject(properties).mapValues { JsonValue.from(it.value) })
                        .build(),
                )
                .build(),
        )
        .build()

    private fun strictObject(properties: Map<String, Any>): Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to properties,
        "required" to properties.keys.toList(),
        "additionalProperties" to false,
    )
}
