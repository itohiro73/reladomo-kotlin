plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    // id("io.github.reladomo-kotlin") version "0.1.0-SNAPSHOT" // Not published yet
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.data:spring-data-commons")
    
    // Project dependencies
    implementation(project(":reladomo-kotlin-core"))
    implementation(project(":reladomo-kotlin-spring-boot"))
    implementation(project(":reladomo-kotlin-generator"))
    
    // Database
    runtimeOnly("com.h2database:h2:2.2.224")
    runtimeOnly("org.postgresql:postgresql:42.7.7")
    
    // Reladomo
    implementation("com.goldmansachs.reladomo:reladomo:${property("reladomoVersion")}")
    implementation("com.goldmansachs.reladomo:reladomogen:${property("reladomoVersion")}")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

// kotlinReladomo {
//     xmlDirectory = file("src/main/resources/reladomo")
//     outputDirectory = file("build/generated/kotlin")
//     packageName = "io.github.reladomokotlin.sample"
//     generateRepositories = true
//     generateBiTemporalSupport = true
// }

tasks.test {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("io.github.reladomokotlin.sample.SampleApplicationKt")
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
    description = "Generate Reladomo Java code and Kotlin wrappers"
    
    // We need the generator classes to be compiled first
    dependsOn(":reladomo-kotlin-generator:classes")
    
    mainClass.set("io.github.reladomokotlin.generator.cli.GeneratorCli")
    
    // Build classpath for running the generator
    classpath = files(
        project(":reladomo-kotlin-generator").sourceSets["main"].output,
        project(":reladomo-kotlin-core").sourceSets["main"].output,
        configurations.runtimeClasspath
    )
    
    args = listOf(
        "src/main/resources/reladomo",
        "build/generated/reladomo/java",
        "src/main/kotlin"
    )
    
    workingDir = projectDir
    
    // Mark outputs for proper up-to-date checking
    outputs.dir("build/generated/reladomo/java")
    outputs.dir("src/main/kotlin/io/github/kotlinreladomo/sample/domain/kotlin")
    outputs.dir("src/main/kotlin/io/github/kotlinreladomo/sample/domain/kotlin/repository")
    
    // Inputs are the XML files
    inputs.dir("src/main/resources/reladomo")
}

// Make compile tasks depend on code generation
tasks.compileJava {
    dependsOn(generateReladomoCode)
}

tasks.compileKotlin {
    dependsOn(generateReladomoCode)
}

// For manual generation
tasks.register("generateCode") {
    dependsOn(generateReladomoCode)
}