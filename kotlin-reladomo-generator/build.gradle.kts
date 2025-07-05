plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Code generation
    implementation("com.squareup:kotlinpoet:${property("kotlinPoetVersion")}")
    
    // XML parsing
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:4.0.4")
    implementation("com.sun.xml.bind:jaxb-impl:4.0.4")
    
    // Project dependencies
    implementation(project(":kotlin-reladomo-core"))
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${property("junitVersion")}")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}