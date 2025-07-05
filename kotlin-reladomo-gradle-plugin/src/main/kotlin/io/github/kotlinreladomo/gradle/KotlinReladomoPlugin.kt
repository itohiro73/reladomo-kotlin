package io.github.kotlinreladomo.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.*

/**
 * Gradle plugin for generating Kotlin wrappers from Reladomo XML files.
 */
class KotlinReladomoPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create<KotlinReladomoExtension>("kotlinReladomo")
        
        // Configure defaults
        extension.xmlDirectory.convention(project.layout.projectDirectory.dir("src/main/resources/reladomo"))
        extension.outputDirectory.convention(project.layout.buildDirectory.dir("generated/kotlin"))
        extension.packageName.convention("${project.group}.kotlin")
        extension.generateRepositories.convention(true)
        extension.generateBiTemporalSupport.convention(true)
        
        // Register the code generation task
        val generateTask = project.tasks.register<GenerateKotlinWrappersTask>("generateKotlinWrappers") {
            description = "Generates Kotlin wrapper classes from Reladomo XML files"
            group = "reladomo"
            
            xmlDirectory.set(extension.xmlDirectory)
            outputDirectory.set(extension.outputDirectory)
            packageName.set(extension.packageName)
            generateRepositories.set(extension.generateRepositories)
            generateBiTemporalSupport.set(extension.generateBiTemporalSupport)
        }
        
        // Add generated sources to source sets
        project.afterEvaluate {
            val sourceSets = project.extensions.findByType<SourceSetContainer>()
            sourceSets?.named("main") {
                java.srcDir(generateTask.flatMap { it.outputDirectory })
            }
            
            // Make compilation depend on code generation
            project.tasks.named("compileKotlin") {
                dependsOn(generateTask)
            }
            
            project.tasks.named("compileJava") {
                dependsOn(generateTask)
            }
        }
        
        // Add dependencies
        project.dependencies {
            add("implementation", "io.github.kotlin-reladomo:kotlin-reladomo-core:${project.version}")
            add("implementation", "com.goldmansachs.reladomo:reladomo:18.0.0")
        }
    }
}