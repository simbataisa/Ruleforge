plugins {
    id("ruleforge.java-conventions")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

description = "RuleForge Engine All-in-One Shadow JAR"

dependencies {
    implementation(project(":ruleforge-core"))
    implementation(project(":ruleforge-table"))
    implementation(project(":ruleforge-tree"))
    implementation(project(":ruleforge-scorecard"))
    implementation(project(":ruleforge-ensemble"))
    // ruleforge-dmn, ruleforge-hierarchy added when ready
}

tasks.shadowJar {
    archiveBaseName.set("ruleforge-engine-all")
    archiveClassifier.set("")
    mergeServiceFiles()
}
