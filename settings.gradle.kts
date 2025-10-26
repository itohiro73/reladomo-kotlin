rootProject.name = "reladomo-kotlin"

include(
    "reladomo-kotlin-core",
    "reladomo-kotlin-generator",
    "reladomo-kotlin-spring-boot",
    "reladomo-kotlin-gradle-plugin",
    "reladomo-kotlin-sample",
    "reladomo-kotlin-demo:backend"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version "1.9.22"
        kotlin("plugin.spring") version "1.9.22"
        id("org.springframework.boot") version "3.2.0"
        id("io.spring.dependency-management") version "1.1.4"
        id("com.vanniktech.maven.publish") version "0.30.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}