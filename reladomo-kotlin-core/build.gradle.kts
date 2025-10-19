plugins {
    kotlin("jvm")
    `java-library`
    id("publishing-conventions")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Reladomo
    api("com.goldmansachs.reladomo:reladomo:${property("reladomoVersion")}")
    
    // Java time backport for better compatibility
    implementation("org.threeten:threetenbp:1.6.8")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${property("junitVersion")}")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}