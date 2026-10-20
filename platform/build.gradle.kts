plugins {
    java
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
        maven {
            url = uri("https://build.shibboleth.net/maven/releases/")
            content {
                includeGroupByRegex("org\\.opensaml.*")
                includeGroupByRegex("net\\.shibboleth.*")
            }
        }
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(property("javaVersion") as String))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        // Align the launcher with the JUnit version managed by Spring Boot (JUnit 6 in Boot 4)
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
