package de.aamir.foodordering.order.ai.notes

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.aamir.foodordering.order.domain.valueobject.DietaryPreference
import de.aamir.foodordering.order.domain.valueobject.ItemInstruction
import de.aamir.foodordering.order.application.dto.notes.OrderNotesInterpretation
import de.aamir.foodordering.order.domain.valueobject.SpiceLevel
import de.aamir.foodordering.order.application.exception.OrderNotesInterpretationException

// Reads the model's JSON field by field and checks it again, even though strict mode should
// already guarantee the shape: anything unexpected is rejected rather than guessed.
internal object OrderNotesResponseParser {

    private val mapper = ObjectMapper()

    fun parse(json: String): OrderNotesInterpretation {
        val root = try {
            mapper.readTree(json)
        } catch (e: JacksonException) {
            throw OrderNotesInterpretationException("Model output is not valid JSON", e)
        }
        if (root == null || !root.isObject) {
            throw OrderNotesInterpretationException("Model output is not a JSON object")
        }
        val unknown = root.fieldNames().asSequence().toSet() - OrderNotesSchema.properties.keys
        if (unknown.isNotEmpty()) {
            throw OrderNotesInterpretationException("Model output has unknown fields $unknown")
        }
        return OrderNotesInterpretation(
            allergies = root.strings("allergies"),
            dietaryPreferences = root.strings("dietaryPreferences").map { enumValue<DietaryPreference>(it) }.toSet(),
            itemInstructions = root.array("itemInstructions").map {
                ItemInstruction(item = it.text("item"), instruction = it.text("instruction"))
            },
            spiceLevel = enumValue<SpiceLevel>(root.text("spiceLevel")),
            deliveryInstructions = root.field("deliveryInstructions").let { if (it.isNull) null else root.text("deliveryInstructions") },
            contactlessDelivery = root.boolean("contactlessDelivery"),
            requiresStaffReview = root.boolean("requiresStaffReview"),
            reviewReasons = root.strings("reviewReasons"),
        )
    }

    private fun JsonNode.field(name: String): JsonNode =
        get(name) ?: throw OrderNotesInterpretationException("Model output is missing '$name'")

    private fun JsonNode.text(name: String): String =
        field(name).takeIf { it.isTextual }?.asText()
            ?: throw OrderNotesInterpretationException("Model output '$name' is not a string")

    private fun JsonNode.boolean(name: String): Boolean =
        field(name).takeIf { it.isBoolean }?.asBoolean()
            ?: throw OrderNotesInterpretationException("Model output '$name' is not a boolean")

    private fun JsonNode.array(name: String): List<JsonNode> =
        field(name).takeIf { it.isArray }?.toList()
            ?: throw OrderNotesInterpretationException("Model output '$name' is not an array")

    private fun JsonNode.strings(name: String): List<String> =
        array(name).map {
            if (!it.isTextual) throw OrderNotesInterpretationException("Model output '$name' has a non-string entry")
            it.asText()
        }

    private inline fun <reified E : Enum<E>> enumValue(value: String): E =
        enumValues<E>().firstOrNull { it.name == value }
            ?: throw OrderNotesInterpretationException("Model output has unknown ${E::class.simpleName} '$value'")
}
