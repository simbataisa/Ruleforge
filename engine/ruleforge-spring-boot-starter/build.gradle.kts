plugins {
    id("ruleforge.java-conventions")
}

description = "RuleForge Spring Boot Starter - Auto-configuration"

dependencies {
    api(project(":ruleforge-core"))
    api(project(":ruleforge-table"))
    api(project(":ruleforge-tree"))
    api(project(":ruleforge-scorecard"))
    api(project(":ruleforge-flow"))
    api(project(":ruleforge-script"))
    api(project(":ruleforge-ensemble"))
    api(project(":ruleforge-hierarchy"))

    implementation("org.springframework.boot:spring-boot-starter:${property("springBootVersion")}")
    implementation("org.springframework.boot:spring-boot-autoconfigure:${property("springBootVersion")}")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${property("springBootVersion")}")
}
