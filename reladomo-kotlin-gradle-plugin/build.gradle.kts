plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    
    // Gradle API
    implementation(gradleApi())
    
    // Project dependencies
    implementation(project(":reladomo-kotlin-generator"))
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("kotlinReladomo") {
            id = "io.github.reladomo-kotlin"
            implementationClass = "io.github.reladomokotlin.gradle.KotlinReladomoPlugin"
            displayName = "Kotlin Reladomo Plugin"
            description = "Gradle plugin for generating Kotlin wrappers from Reladomo XML files"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}