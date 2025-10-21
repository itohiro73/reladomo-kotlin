import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

// Only publish specific modules
val publishedModules = setOf(
    "reladomo-kotlin-core",
    "reladomo-kotlin-generator",
    "reladomo-kotlin-spring-boot",
    "reladomo-kotlin-gradle-plugin"
)

if (project.name in publishedModules) {
    mavenPublishing {
        // Publish to Central Portal (2025+ requirement)
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

        // Sign all publications (required by Maven Central)
        // Only sign if signing credentials are available
        val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
        if (!signingKey.isNullOrEmpty()) {
            signAllPublications()
        }

        // Configure Maven coordinates
        coordinates(
            groupId = findProperty("group")?.toString() ?: "io.github.itohiro73",
            artifactId = project.name,
            version = project.version.toString()
        )

        // Configure POM metadata
        pom {
            name.set(project.name)
            description.set(findProperty("pomDescription")?.toString() ?: "Kotlin wrapper for Reladomo ORM")
            inceptionYear.set("2025")
            url.set(findProperty("pomUrl")?.toString() ?: "https://github.com/itohiro73/reladomo-kotlin")

            licenses {
                license {
                    name.set(findProperty("pomLicenseName")?.toString() ?: "The Apache License, Version 2.0")
                    url.set(findProperty("pomLicenseUrl")?.toString() ?: "http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("itohiro73")
                    name.set(findProperty("pomDeveloperName")?.toString() ?: "Hiroshi Ito")
                    email.set(findProperty("pomDeveloperEmail")?.toString() ?: "")
                }
            }

            scm {
                url.set(findProperty("pomScmUrl")?.toString() ?: "https://github.com/itohiro73/reladomo-kotlin")
                connection.set(findProperty("pomScmConnection")?.toString() ?: "scm:git:git://github.com/itohiro73/reladomo-kotlin.git")
                developerConnection.set("scm:git:ssh://git@github.com/itohiro73/reladomo-kotlin.git")
            }
        }
    }
}
