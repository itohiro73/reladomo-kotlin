plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("io.github.kotlin-reladomo") version "0.1.0-SNAPSHOT"
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    
    // Project dependencies
    implementation(project(":kotlin-reladomo-core"))
    implementation(project(":kotlin-reladomo-spring-boot"))
    
    // Database
    runtimeOnly("com.h2database:h2:2.2.224")
    runtimeOnly("org.postgresql:postgresql:42.7.1")
    
    // Reladomo
    implementation("com.goldmansachs.reladomo:reladomo:${property("reladomoVersion")}")
    implementation("com.goldmansachs.reladomo:reladomogen:${property("reladomoVersion")}")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

kotlinReladomo {
    xmlDirectory = file("src/main/resources/reladomo")
    outputDirectory = file("build/generated/kotlin")
    packageName = "io.github.kotlinreladomo.sample"
    generateRepositories = true
    generateBiTemporalSupport = true
}

tasks.test {
    useJUnitPlatform()
}