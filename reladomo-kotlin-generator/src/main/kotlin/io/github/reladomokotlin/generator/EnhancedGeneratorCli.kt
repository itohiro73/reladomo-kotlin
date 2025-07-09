package io.github.reladomokotlin.generator.cli

import io.github.reladomokotlin.generator.EnhancedKotlinCodeGenerator
import io.github.reladomokotlin.generator.parser.EnhancedXmlParser
import io.github.reladomokotlin.generator.result.*
import com.gs.fw.common.mithra.generator.MithraGenerator
import java.io.File

/**
 * Enhanced command-line interface for code generation with type safety and error handling
 */
object EnhancedGeneratorCli {
    @JvmStatic
    fun main(args: Array<String>) {
        val result = parseArguments(args).flatMap { config ->
            generate(config)
        }
        
        result.onFailure { error ->
            System.err.println("Generation failed: $error")
            System.exit(1)
        }.onSuccess {
            println("Code generation completed successfully!")
        }
    }
    
    private fun parseArguments(args: Array<String>): Result<GeneratorConfig> {
        return if (args.size < 3) {
            Result.failure(GeneratorError.ConfigurationError(
                "Invalid arguments",
                "args"
            ))
        } else {
            Result.success(GeneratorConfig(
                xmlDir = File(args[0]),
                javaOutputDir = File(args[1]),
                kotlinOutputDir = File(args[2])
            ))
        }
    }
    
    fun generate(config: GeneratorConfig): Result<GenerationSummary> {
        return validateDirectories(config).flatMap { validConfig ->
            generateJavaCode(validConfig).flatMap { javaResult ->
                generateKotlinCode(validConfig).map { kotlinResult ->
                    GenerationSummary(
                        javaFiles = javaResult,
                        kotlinFiles = kotlinResult,
                        config = validConfig
                    )
                }
            }
        }
    }
    
    private fun validateDirectories(config: GeneratorConfig): Result<GeneratorConfig> {
        return when {
            !config.xmlDir.exists() -> Result.failure(
                GeneratorError.FileSystemError(
                    "XML directory does not exist",
                    config.xmlDir.absolutePath
                )
            )
            !config.xmlDir.isDirectory -> Result.failure(
                GeneratorError.FileSystemError(
                    "XML path is not a directory",
                    config.xmlDir.absolutePath
                )
            )
            else -> {
                // Create output directories if they don't exist
                config.javaOutputDir.mkdirs()
                config.kotlinOutputDir.mkdirs()
                Result.success(config)
            }
        }
    }
    
    private fun generateJavaCode(config: GeneratorConfig): Result<Int> {
        println("\n=== Step 1: Running Reladomo Java code generator ===")
        
        val classListFile = File(config.xmlDir, "MithraClassList.xml")
        return if (!classListFile.exists()) {
            Result.failure(GeneratorError.FileSystemError(
                "MithraClassList.xml not found",
                classListFile.absolutePath
            ))
        } else {
            Result.runCatching {
                val mithraGenerator = MithraGenerator()
                mithraGenerator.setXml(classListFile.absolutePath)
                mithraGenerator.setGeneratedDir(config.javaOutputDir.absolutePath)
                mithraGenerator.setNonGeneratedDir(config.javaOutputDir.absolutePath)
                mithraGenerator.execute()
                
                // Count generated files
                val generatedFiles = config.javaOutputDir.walkTopDown()
                    .filter { it.isFile && it.extension == "java" }
                    .count()
                    
                println("  ✓ Generated $generatedFiles Java classes")
                generatedFiles
            }
        }
    }
    
    private fun generateKotlinCode(config: GeneratorConfig): Result<List<GeneratedFileInfo>> {
        println("\n=== Step 2: Generating Kotlin wrappers with enhanced type safety ===")
        
        val parser = EnhancedXmlParser()
        val generator = EnhancedKotlinCodeGenerator()
        val generatedFiles = mutableListOf<GeneratedFileInfo>()
        val errors = mutableListOf<GeneratorError>()
        
        // Process each XML file (except MithraClassList.xml)
        val xmlFiles = config.xmlDir.listFiles { file -> 
            file.extension == "xml" && file.name != "MithraClassList.xml" 
        } ?: emptyArray()
        
        xmlFiles.forEach { xmlFile ->
            println("\nProcessing: ${xmlFile.name}")
            
            val result = Result.runCatching {
                parser.parseXmlFile(xmlFile)
            }.flatMap { parsedObject ->
                println("  ✓ Parsed: ${parsedObject.className} (${parsedObject.objectType})")
                println("    - ${parsedObject.simpleAttributes.size} simple attributes")
                println("    - ${parsedObject.asOfAttributes.size} temporal attributes")
                println("    - ${parsedObject.relationships.size} relationships")
                
                generator.generateKotlinWrapper(parsedObject, config.kotlinOutputDir)
            }
            
            result.onSuccess { files ->
                println("  ✓ Generated wrapper: ${files.wrapper.name}")
                println("  ✓ Generated repository: ${files.repository.name}")
                files.builder?.let { println("  ✓ Generated builder: ${it.name}") }
                
                generatedFiles.add(GeneratedFileInfo(
                    xmlFile = xmlFile,
                    wrapperFile = files.wrapper,
                    repositoryFile = files.repository,
                    builderFile = files.builder
                ))
            }.onFailure { error ->
                println("  ✗ Error: $error")
                errors.add(error)
            }
        }
        
        return if (errors.isNotEmpty()) {
            Result.failure(GeneratorError.CodeGenerationError(
                "Failed to generate ${errors.size} files",
                null,
                GeneratorException(errors.first())
            ))
        } else {
            println("\n✓ Successfully generated ${generatedFiles.size} Kotlin wrappers")
            Result.success(generatedFiles)
        }
    }
    
    data class GeneratorConfig(
        val xmlDir: File,
        val javaOutputDir: File,
        val kotlinOutputDir: File
    )
    
    data class GeneratedFileInfo(
        val xmlFile: File,
        val wrapperFile: File,
        val repositoryFile: File,
        val builderFile: File?
    )
    
    data class GenerationSummary(
        val javaFiles: Int,
        val kotlinFiles: List<GeneratedFileInfo>,
        val config: GeneratorConfig
    ) {
        fun printSummary() {
            println("\n=== Generation Summary ===")
            println("Java files generated: $javaFiles")
            println("Kotlin wrappers generated: ${kotlinFiles.size}")
            println("Output directories:")
            println("  - Java: ${config.javaOutputDir.absolutePath}")
            println("  - Kotlin: ${config.kotlinOutputDir.absolutePath}")
            
            if (kotlinFiles.isNotEmpty()) {
                println("\nGenerated Kotlin files:")
                kotlinFiles.forEach { info ->
                    println("  ${info.xmlFile.name} →")
                    println("    - Wrapper: ${info.wrapperFile.relativeTo(config.kotlinOutputDir)}")
                    println("    - Repository: ${info.repositoryFile.relativeTo(config.kotlinOutputDir)}")
                    info.builderFile?.let {
                        println("    - Builder: ${it.relativeTo(config.kotlinOutputDir)}")
                    }
                }
            }
        }
    }
}