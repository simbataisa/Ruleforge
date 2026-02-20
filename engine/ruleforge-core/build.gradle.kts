plugins {
    id("ruleforge.java-conventions")
}

description = "RuleForge Engine Core - Abstractions, Working Memory, RFL Parser, SPI"

dependencies {
    // Zero external deps except SLF4J API
    api("org.slf4j:slf4j-api:${property("slf4jVersion")}")

    // Jackson for JSON parsing (optional, used for RuleDefinition deserialization)
    implementation("com.fasterxml.jackson.core:jackson-databind:${property("jacksonVersion")}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${property("jacksonVersion")}")
}
