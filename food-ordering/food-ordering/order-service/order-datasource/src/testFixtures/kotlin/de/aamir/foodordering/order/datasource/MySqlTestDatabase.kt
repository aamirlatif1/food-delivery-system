package de.aamir.foodordering.order.datasource

import io.micronaut.test.support.TestPropertyProvider
import org.testcontainers.mysql.MySQLContainer

// One MySQL container for all tests in this module; Flyway creates the schema when the context starts.
object MySqlTestDatabase {

    private val container: MySQLContainer by lazy {
        MySQLContainer("mysql:8.4")
            .withUrlParam("connectionTimeZone", "UTC")
            .withUrlParam("forceConnectionTimeZoneToSession", "true")
            .apply { start() }
    }

    fun properties(): Map<String, String> = mapOf(
        "datasources.default.url" to container.jdbcUrl,
        "datasources.default.username" to container.username,
        "datasources.default.password" to container.password,
        "datasources.default.driver-class-name" to "com.mysql.cj.jdbc.Driver",
        "datasources.default.dialect" to "MYSQL",
        "flyway.datasources.default.enabled" to "true",
    )
}

// Implemented by @MicronautTest classes, which then need @TestInstance(PER_CLASS).
interface MySqlTest : TestPropertyProvider {
    override fun getProperties(): Map<String, String> = MySqlTestDatabase.properties()
}
