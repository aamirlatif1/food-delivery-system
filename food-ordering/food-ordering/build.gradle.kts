import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.micronaut.library) apply false
    alias(libs.plugins.micronaut.application) apply false
    alias(libs.plugins.micronaut.aot) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "de.aamir.foodordering"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(25)
        }

        dependencies {
            "testImplementation"(kotlin("test"))
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            // Gradle 9 fails a test task that discovers no tests; empty modules are expected for now.
            failOnNoDiscoveredTests = false
        }
    }
}
