import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.signing

plugins {
    `maven-publish`
    signing
    `java-library`
}

// Only publish specific modules
val publishedModules = setOf(
    "reladomo-kotlin-core",
    "reladomo-kotlin-generator",
    "reladomo-kotlin-spring-boot",
    "reladomo-kotlin-gradle-plugin"
)

if (project.name in publishedModules) {
    java {
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = project.name
                from(components["java"])

                pom {
                    name.set(property("pomName").toString() + " - ${project.name}")
                    description.set(property("pomDescription").toString())
                    url.set(property("pomUrl").toString())

                    licenses {
                        license {
                            name.set(property("pomLicenseName").toString())
                            url.set(property("pomLicenseUrl").toString())
                        }
                    }

                    developers {
                        developer {
                            id.set("reladomo-kotlin")
                            name.set(property("pomDeveloperName").toString())
                            email.set(property("pomDeveloperEmail").toString())
                        }
                    }

                    scm {
                        url.set(property("pomScmUrl").toString())
                        connection.set(property("pomScmConnection").toString())
                        developerConnection.set(property("pomScmConnection").toString().replace("git://", "ssh://"))
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                
                credentials {
                    username = System.getenv("OSSRH_USERNAME") ?: property("ossrhUsername")?.toString()
                    password = System.getenv("OSSRH_PASSWORD") ?: property("ossrhPassword")?.toString()
                }
            }
        }
    }

    signing {
        val signingKeyId = System.getenv("SIGNING_KEY_ID") ?: property("signing.keyId")?.toString()
        val signingPassword = System.getenv("SIGNING_PASSWORD") ?: property("signing.password")?.toString()
        val signingKey = System.getenv("SIGNING_SECRET_KEY") ?: property("signing.secretKeyRingFile")?.toString()

        if (!signingKeyId.isNullOrEmpty()) {
            if (!signingKey.isNullOrEmpty() && signingKey.contains("BEGIN PGP")) {
                useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            } else {
                useGpgCmd()
            }
            sign(publishing.publications["maven"])
        }
    }

    tasks.withType<Sign> {
        onlyIf { !version.toString().endsWith("SNAPSHOT") }
    }
}