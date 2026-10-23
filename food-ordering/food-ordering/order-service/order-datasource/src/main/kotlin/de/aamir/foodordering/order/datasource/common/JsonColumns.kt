package de.aamir.foodordering.order.datasource.common

import io.micronaut.core.type.Argument
import io.micronaut.json.JsonMapper

// readValue returns null for a JSON null, which no column written by this adapter holds.
fun <T : Any> JsonMapper.readColumn(json: String, type: Argument<T>): T =
    checkNotNull(readValue(json, type)) { "JSON column holds null, expected ${type.typeName}" }

fun <T : Any> JsonMapper.readColumn(json: String, type: Class<T>): T = readColumn(json, Argument.of(type))
