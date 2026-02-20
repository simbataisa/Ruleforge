plugins {
    id("ruleforge.java-conventions")
}

description = "RuleForge Ensemble - Multi-strategy rule aggregation and voting"

dependencies {
    api(project(":ruleforge-core"))
}
