plugins {
    id("ruleforge.java-conventions")
}

description = "RuleForge CEP - Complex Event Processing with Kafka Streams"

dependencies {
    api(project(":ruleforge-core"))

    // Kafka Streams for complex event processing
    implementation("org.apache.kafka:kafka-streams:${property("kafkaVersion")}")
}
