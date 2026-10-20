// Message payloads generated from the Protobuf contracts in contracts/proto (REQUIREMENTS.md §5,
// ADR-008). Pure Kotlin/Java: no Micronaut.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
}

sourceSets {
    main {
        proto {
            srcDir(rootDir.resolve("../../contracts/proto"))
        }
    }
}

dependencies {
    // api: the generated classes extend protobuf-java types.
    api(libs.protobuf.kotlin)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                // Kotlin DSL builders (paymentRequest { ... }) next to the Java classes.
                create("kotlin")
            }
        }
    }
}
