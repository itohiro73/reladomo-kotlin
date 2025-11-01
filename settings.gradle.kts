rootProject.name = "reladomo-kotlin"

include(
    "reladomo-kotlin-core",
    "reladomo-kotlin-generator",
    "reladomo-kotlin-spring-boot",
    "reladomo-kotlin-gradle-plugin",
    "reladomo-kotlin-sample",
    "reladomo-kotlin-demo:backend",
    "chronostaff:backend"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "2.2.21"
        kotlin("plugin.spring") version "2.2.21"
        id("org.springframework.boot") version "3.2.0"
        id("io.spring.dependency-management") version "1.1.7"
        id("com.vanniktech.maven.publish") version "0.34.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}