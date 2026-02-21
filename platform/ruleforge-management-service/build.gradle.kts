plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":ruleforge-common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:${property("flywayVersion")}")
    implementation("org.flywaydb:flyway-database-postgresql:${property("flywayVersion")}")

    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // Engine dependencies (composite build from engine project)
    implementation("io.ruleforge:ruleforge-core:${property("version")}")
    implementation("io.ruleforge:ruleforge-table:${property("version")}")
    implementation("io.ruleforge:ruleforge-tree:${property("version")}")
    implementation("io.ruleforge:ruleforge-scorecard:${property("version")}")
    implementation("io.ruleforge:ruleforge-ensemble:${property("version")}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
