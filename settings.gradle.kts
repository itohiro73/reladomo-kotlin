rootProject.name = "kotlin-reladomo"

include(
    "kotlin-reladomo-core",
    "kotlin-reladomo-generator",
    "kotlin-reladomo-spring-boot",
    "kotlin-reladomo-gradle-plugin",
    "kotlin-reladomo-sample"
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