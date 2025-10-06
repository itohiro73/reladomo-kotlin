plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `java-library`
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter:${property("springBootVersion")}")
    implementation("org.springframework.boot:spring-boot-starter-jdbc:${property("springBootVersion")}")
    implementation("org.springframework.data:spring-data-commons:3.2.0")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${property("springBootVersion")}")
    
    // Project dependencies
    api(project(":reladomo-kotlin-core"))
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:${property("springBootVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}