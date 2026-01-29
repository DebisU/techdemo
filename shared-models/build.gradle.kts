plugins {
    kotlin("jvm")
}

description = "Shared models and events"

dependencies {
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
}
