import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22" apply false
    kotlin("plugin.spring") version "1.9.22" apply false
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    `maven-publish`
    `java-library`
}

group = "io.github.reladomo-kotlin"
version = "0.1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    
    group = rootProject.group
    version = rootProject.version
    
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                
                pom {
                    name.set("Kotlin Reladomo ${project.name}")
                    description.set("Kotlin wrapper for Reladomo ORM with Spring Boot integration")
                    url.set("https://github.com/kotlin-reladomo/kotlin-reladomo")
                    
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("kotlin-reladomo")
                            name.set("Kotlin Reladomo Team")
                        }
                    }
                    
                    scm {
                        url.set("https://github.com/kotlin-reladomo/kotlin-reladomo")
                        connection.set("scm:git:git://github.com/kotlin-reladomo/kotlin-reladomo.git")
                        developerConnection.set("scm:git:ssh://github.com/kotlin-reladomo/kotlin-reladomo.git")
                    }
                }
            }
        }
    }
}