plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.data:spring-data-commons")

    // Reladomo Kotlin dependencies - using local project for latest changes
    implementation(project(":reladomo-kotlin-core"))
    implementation(project(":reladomo-kotlin-spring-boot"))
    implementation(project(":reladomo-kotlin-generator"))

    // Database
    runtimeOnly("com.h2database:h2:2.2.224")

    // Reladomo
    implementation("com.goldmansachs.reladomo:reladomo:${property("reladomoVersion")}")
    implementation("com.goldmansachs.reladomo:reladomogen:${property("reladomoVersion")}")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.test {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("io.github.chronostaff.ChronoStaffApplicationKt")
}

// Add generated sources to source sets
sourceSets {
    main {
        java {
            srcDir("build/generated/reladomo/java")
        }
    }
}

// Configure Reladomo code generation task
val generateReladomoCode = tasks.register<JavaExec>("generateReladomoCode") {
    group = "code generation"
    description = "Generate Reladomo Java code and Kotlin wrappers for ChronoStaff HR demo"

    mainClass.set("io.github.reladomokotlin.generator.cli.GeneratorCli")
    classpath = configurations.runtimeClasspath.get()

    args = listOf(
        "src/main/resources/reladomo",
        "build/generated/reladomo/java",
        "src/main/kotlin"
    )

    workingDir = projectDir

    outputs.dir("build/generated/reladomo/java")
    outputs.dir("src/main/kotlin/io/github/chronostaff/domain/kotlin")
    outputs.dir("src/main/kotlin/io/github/chronostaff/domain/kotlin/repository")
    inputs.dir("src/main/resources/reladomo")
}

tasks.compileJava {
    dependsOn(generateReladomoCode)
}

tasks.compileKotlin {
    dependsOn(generateReladomoCode)
}

tasks.register("generateCode") {
    dependsOn(generateReladomoCode)
}

// Set JVM timezone to UTC for consistent timestamp handling
// CRITICAL: This ensures all TIMESTAMP columns are interpreted as UTC
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs = listOf("-Duser.timezone=UTC")
}
