rootProject.name = "reladomo-kotlin"

include(
    "reladomo-kotlin-core",
    "reladomo-kotlin-generator",
    "reladomo-kotlin-spring-boot",
    "reladomo-kotlin-gradle-plugin",
    "reladomo-kotlin-sample"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}