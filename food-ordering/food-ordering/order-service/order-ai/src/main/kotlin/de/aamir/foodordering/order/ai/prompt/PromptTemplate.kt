package de.aamir.foodordering.order.ai.prompt

// A prompt kept as a classpath resource with {{name}} placeholders. Rendering fails when a
// placeholder has no value or a value has no placeholder, so a prompt never goes out half filled.
class PromptTemplate(val name: String, private val text: String) {

    val placeholders: Set<String> = PLACEHOLDER.findAll(text).map { it.groupValues[1] }.toSet()

    fun render(values: Map<String, String>): String {
        val missing = placeholders - values.keys
        val unknown = values.keys - placeholders
        require(missing.isEmpty() && unknown.isEmpty()) {
            "Prompt '$name': missing values $missing, unknown values $unknown"
        }
        return PLACEHOLDER.replace(text) { values.getValue(it.groupValues[1]) }
    }

    companion object {
        private val PLACEHOLDER = Regex("""\{\{(\w+)}}""")

        fun fromClasspath(path: String): PromptTemplate {
            val resource = checkNotNull(PromptTemplate::class.java.classLoader.getResource(path)) {
                "Prompt resource $path not found"
            }
            return PromptTemplate(path, resource.readText().trim())
        }
    }
}
