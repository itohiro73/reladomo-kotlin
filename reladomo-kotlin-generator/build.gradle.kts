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
    
    // Reladomo generator
    implementation("com.goldmansachs.reladomo:reladomogen:${property("reladomoVersion")}")
    implementation("com.goldmansachs.reladomo:reladomo:${property("reladomoVersion")}")
    implementation("org.apache.ant:ant:1.10.14")
    
    // Project dependencies
    implementation(project(":reladomo-kotlin-core"))
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${property("junitVersion")}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${property("kotlinVersion")}")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}