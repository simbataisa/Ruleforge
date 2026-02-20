plugins {
    id("ruleforge.java-conventions")
}

description = "RuleForge Script - GraalVM polyglot script rule evaluation"

dependencies {
    api(project(":ruleforge-core"))

    // GraalVM SDK for polyglot script execution
    implementation("org.graalvm.sdk:graal-sdk:${property("graalvmVersion")}")
    // Jackson for JSON serialization of working memory facts
    implementation("com.fasterxml.jackson.core:jackson-databind:${property("jacksonVersion")}")
}
