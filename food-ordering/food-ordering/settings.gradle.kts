plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "food-ordering"

include(
    "common:common-domain",
    "order-service:order-domain:core-domain",
    "order-service:order-domain:order-application-service",
    "order-service:order-application",
    "order-service:order-messaging",
    "order-service:order-datasource",
    "order-service:order-container",
)
