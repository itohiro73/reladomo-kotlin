package io.github.kotlinreladomo.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

/**
 * Gradle plugin for generating Kotlin wrappers from Reladomo XML files.
 */
class KotlinReladomoPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create("kotlinReladomo", KotlinReladomoExtension::class.java)
        
        // Configure defaults
        extension.xmlDirectory.convention(project.layout.projectDirectory.dir("src/main/resources/reladomo"))
        extension.outputDirectory.convention(project.layout.buildDirectory.dir("generated/kotlin"))
        extension.packageName.convention("${project.group}.kotlin")
        extension.generateRepositories.convention(true)
        extension.generateBiTemporalSupport.convention(true)
        
        // Register the code generation task
        val generateTask = project.tasks.register("generateKotlinWrappers", GenerateKotlinWrappersTask::class.java) { task ->
            task.description = "Generates Kotlin wrapper classes from Reladomo XML files"
            task.group = "reladomo"
            
            task.xmlDirectory.set(extension.xmlDirectory)
            task.outputDirectory.set(extension.outputDirectory)
            task.packageName.set(extension.packageName)
            task.generateRepositories.set(extension.generateRepositories)
            task.generateBiTemporalSupport.set(extension.generateBiTemporalSupport)
        }
        
        // Add generated sources to source sets
        project.afterEvaluate {
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
            sourceSets?.getByName("main")?.java?.srcDir(generateTask.flatMap { it.outputDirectory })
            
            // Make compilation depend on code generation
            project.tasks.findByName("compileKotlin")?.dependsOn(generateTask)
            project.tasks.findByName("compileJava")?.dependsOn(generateTask)
        }
        
        // Add dependencies
        project.dependencies.add("implementation", "io.github.kotlin-reladomo:kotlin-reladomo-core:${project.version}")
        project.dependencies.add("implementation", "com.goldmansachs.reladomo:reladomo:18.0.0")
    }
}