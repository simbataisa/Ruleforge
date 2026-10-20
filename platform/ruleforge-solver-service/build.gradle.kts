plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":ruleforge-common"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.github.ben-manes.caffeine:caffeine:${property("caffeineVersion")}")
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    // Engine dependencies (composite build from engine project)
    implementation("io.ruleforge:ruleforge-core:${property("version")}")
    implementation("io.ruleforge:ruleforge-table:${property("version")}")
    implementation("io.ruleforge:ruleforge-tree:${property("version")}")
    implementation("io.ruleforge:ruleforge-script:${property("version")}")
    implementation("io.ruleforge:ruleforge-flow:${property("version")}")
    implementation("io.ruleforge:ruleforge-scorecard:${property("version")}")
    implementation("io.ruleforge:ruleforge-ensemble:${property("version")}")
    implementation("io.ruleforge:ruleforge-dmn:${property("version")}")
    implementation("io.ruleforge:ruleforge-cep:${property("version")}")
    implementation("io.ruleforge:ruleforge-hierarchy:${property("version")}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
