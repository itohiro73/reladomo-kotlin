import java.io.File

// Standalone script to test the code generator
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.goldmansachs.reladomo:reladomo:18.0.0")
        classpath("com.goldmansachs.reladomo:reladomogen:18.0.0")
        classpath("com.squareup:kotlinpoet:1.14.2")
        classpath("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
        classpath(files("reladomo-kotlin-generator/build/libs/reladomo-kotlin-generator.jar"))
    }
}

tasks.register("testGenerator") {
    doLast {
        // Add all required classes to classpath
        val generator = Class.forName("io.github.reladomokotlin.generator.KotlinWrapperGenerator")
            .getDeclaredConstructor().newInstance()
        
        val parser = Class.forName("io.github.reladomokotlin.generator.parser.ReladomoXmlParser")
            .getDeclaredConstructor().newInstance()
        
        val xmlFile = File("reladomo-kotlin-sample/src/main/resources/reladomo/Order.xml")
        val outputDir = File("build/test-generated")
        
        println("Parsing XML file: ${xmlFile.absolutePath}")
        val parseMethod = parser.javaClass.getMethod("parse", File::class.java)
        val definition = parseMethod.invoke(parser, xmlFile)
        
        println("Generating Kotlin code...")
        val generateMethod = generator.javaClass.getMethod(
            "generateToFile", 
            Class.forName("io.github.reladomokotlin.generator.model.MithraObjectDefinition"),
            File::class.java
        )
        
        outputDir.mkdirs()
        val generatedFile = generateMethod.invoke(generator, definition, outputDir)
        println("Generated file: $generatedFile")
        
        // Also generate repository
        val repoGenerator = Class.forName("io.github.reladomokotlin.generator.KotlinRepositoryGenerator")
            .getDeclaredConstructor().newInstance()
        val repoGenerateMethod = repoGenerator.javaClass.getMethod(
            "generateToFile",
            Class.forName("io.github.reladomokotlin.generator.model.MithraObjectDefinition"),
            File::class.java
        )
        val repoFile = repoGenerateMethod.invoke(repoGenerator, definition, outputDir)
        println("Generated repository: $repoFile")
    }
}