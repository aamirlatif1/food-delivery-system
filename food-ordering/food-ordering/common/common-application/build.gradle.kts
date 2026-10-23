// Micronaut HTTP building blocks shared by the services' web adapters.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.micronaut.library)
}

dependencies {
    // api: handlers in the services extend ProblemExceptionHandler, an ExceptionHandler.
    api("io.micronaut:micronaut-http-server")
    // Errors as RFC 9457 Problem Details (application/problem+json).
    implementation("io.micronaut.problem:micronaut-problem-json")
    implementation("org.zalando:problem")
}
