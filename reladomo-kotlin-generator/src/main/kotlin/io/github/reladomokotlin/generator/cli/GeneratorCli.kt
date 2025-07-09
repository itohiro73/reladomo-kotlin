package io.github.reladomokotlin.generator.cli

import io.github.reladomokotlin.generator.KotlinWrapperGenerator
import io.github.reladomokotlin.generator.KotlinRepositoryGenerator
import io.github.reladomokotlin.generator.QueryDslGenerator
import io.github.reladomokotlin.generator.parser.ReladomoXmlParser
import com.gs.fw.common.mithra.generator.MithraGenerator
import java.io.File

/**
 * Command-line interface for code generation.
 * Usage: GeneratorCli <xmlDir> <javaOutputDir> <kotlinOutputDir>
 */
object GeneratorCli {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 3) {
            println("Usage: GeneratorCli <xmlDir> <javaOutputDir> <kotlinOutputDir>")
            System.exit(1)
        }
        
        val xmlDir = File(args[0])
        val javaOutputDir = File(args[1])
        val kotlinOutputDir = File(args[2])
        
        generate(xmlDir, javaOutputDir, kotlinOutputDir)
    }
    
    fun generate(xmlDir: File, javaOutputDir: File, kotlinOutputDir: File) {
        println("Generating code from XML files in: ${xmlDir.absolutePath}")
        
        // Step 1: Run Reladomo's Java code generator
        println("\n=== Step 1: Running Reladomo Java code generator ===")
        javaOutputDir.mkdirs()
        
        val classListFile = File(xmlDir, "MithraClassList.xml")
        if (classListFile.exists()) {
            println("Using MithraClassList.xml for Java code generation")
            try {
                val mithraGenerator = MithraGenerator()
                mithraGenerator.setXml(classListFile.absolutePath)
                mithraGenerator.setGeneratedDir(javaOutputDir.absolutePath)
                mithraGenerator.setNonGeneratedDir(javaOutputDir.absolutePath)
                mithraGenerator.execute()
                println("  ✓ Generated Java classes")
            } catch (e: Exception) {
                println("  ✗ Error generating Java code: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("  ✗ MithraClassList.xml not found in $xmlDir")
        }
        
        // Step 2: Generate Kotlin wrappers
        println("\n=== Step 2: Generating Kotlin wrappers ===")
        
        // Ensure output directories exist
        kotlinOutputDir.mkdirs()
        
        val parser = ReladomoXmlParser()
        val generator = KotlinWrapperGenerator()
        val repoGenerator = KotlinRepositoryGenerator()
        val dslGenerator = QueryDslGenerator()
        
        // Generate code for each XML file (except MithraClassList.xml)
        xmlDir.listFiles { file -> 
            file.extension == "xml" && file.name != "MithraClassList.xml" 
        }?.forEach { xmlFile ->
            println("Processing: ${xmlFile.name}")
            
            try {
                // Parse XML
                val definition = parser.parse(xmlFile)
                println("  Parsed: ${definition.className} in package ${definition.packageName}")
                
                // Generate wrapper
                val wrapperFile = generator.generateToFile(definition, kotlinOutputDir)
                println("  Generated wrapper: ${wrapperFile.absolutePath}")
                
                // Generate repository
                val repoFile = repoGenerator.generateToFile(definition, kotlinOutputDir)
                println("  Generated repository: ${repoFile.absolutePath}")
                
                // Generate Query DSL extensions
                val dslFile = dslGenerator.generateToFile(definition, kotlinOutputDir)
                println("  Generated Query DSL: ${dslFile.absolutePath}")
                
            } catch (e: Exception) {
                println("  Error processing ${xmlFile.name}: ${e.message}")
                e.printStackTrace()
            }
        }
        
        println("\nCode generation completed!")
        println("Java output: ${javaOutputDir.absolutePath}")
        println("Kotlin output: ${kotlinOutputDir.absolutePath}")
    }
}