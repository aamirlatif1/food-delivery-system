package de.aamir.foodordering.order.ai.prompt

// The system prompt sets the persona and the guardrails; the user template frames the
// customer's notes as data inside <customer_notes> tags.
object OrderNotesPrompts {

    // Logged with every call, so a change in behaviour can be traced to a prompt change.
    const val VERSION = "order-notes-v1"

    private val system = PromptTemplate.fromClasspath("prompts/order-notes/system.txt")
    private val user = PromptTemplate.fromClasspath("prompts/order-notes/user.txt")

    fun systemMessage(): String = system.render(emptyMap())

    fun userMessage(notes: String): String = user.render(mapOf("notes" to neutralize(notes)))

    // Removes the delimiter tags from the notes, so a customer cannot close the data block
    // and write text that reads like instructions.
    private fun neutralize(notes: String): String = DELIMITER.replace(notes, "").trim()

    private val DELIMITER = Regex("""</?\s*customer_notes\s*>""", RegexOption.IGNORE_CASE)
}
