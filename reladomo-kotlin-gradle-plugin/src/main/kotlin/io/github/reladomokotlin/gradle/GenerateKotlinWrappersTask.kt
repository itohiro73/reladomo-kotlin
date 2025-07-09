package io.github.reladomokotlin.gradle

import io.github.reladomokotlin.generator.KotlinRepositoryGenerator
import io.github.reladomokotlin.generator.KotlinWrapperGenerator
import io.github.reladomokotlin.generator.parser.ReladomoXmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.InputChanges
import java.io.File

/**
 * Gradle task for generating Kotlin wrapper classes from Reladomo XML files.
 */
@CacheableTask
abstract class GenerateKotlinWrappersTask : DefaultTask() {
    
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val xmlDirectory: DirectoryProperty
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    @get:Input
    abstract val packageName: Property<String>
    
    @get:Input
    abstract val generateRepositories: Property<Boolean>
    
    @get:Input
    abstract val generateBiTemporalSupport: Property<Boolean>
    
    @TaskAction
    fun generate(inputChanges: InputChanges) {
        val parser = ReladomoXmlParser()
        val wrapperGenerator = KotlinWrapperGenerator()
        val repositoryGenerator = KotlinRepositoryGenerator()
        
        val xmlDir = xmlDirectory.asFile.get()
        val outputDir = outputDirectory.asFile.get()
        
        // Clean output directory if doing a full rebuild
        if (!inputChanges.isIncremental) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()
        
        // Find all XML files
        val xmlFiles = xmlDir.walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .toList()
        
        if (xmlFiles.isEmpty()) {
            logger.warn("No XML files found in ${xmlDir.absolutePath}")
            return
        }
        
        logger.lifecycle("Found ${xmlFiles.size} Reladomo XML files to process")
        
        // Process each XML file
        xmlFiles.forEach { xmlFile ->
            try {
                logger.lifecycle("Processing: ${xmlFile.name}")
                
                // Parse XML
                val definition = parser.parse(xmlFile)
                
                // Override package name if specified
                val processedDefinition = if (packageName.isPresent && packageName.get().isNotBlank()) {
                    definition.copy(packageName = packageName.get().substringBeforeLast(".kotlin"))
                } else {
                    definition
                }
                
                // Generate wrapper class
                val wrapperFile = wrapperGenerator.generateToFile(processedDefinition, outputDir)
                logger.info("Generated wrapper: ${wrapperFile.relativeTo(outputDir)}")
                
                // Generate repository if enabled
                if (generateRepositories.get() && processedDefinition.isBitemporal) {
                    val repositoryFile = repositoryGenerator.generateToFile(processedDefinition, outputDir)
                    logger.info("Generated repository: ${repositoryFile.relativeTo(outputDir)}")
                }
                
            } catch (e: Exception) {
                logger.error("Failed to process ${xmlFile.name}: ${e.message}")
                throw e
            }
        }
        
        logger.lifecycle("Code generation completed successfully")
    }
}