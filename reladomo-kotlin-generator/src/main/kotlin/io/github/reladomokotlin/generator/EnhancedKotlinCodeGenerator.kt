package io.github.reladomokotlin.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.reladomokotlin.generator.model.ParsedMithraObject
import io.github.reladomokotlin.generator.result.*
import io.github.reladomokotlin.generator.types.ObjectType
import io.github.reladomokotlin.generator.types.AttributeType
import io.github.reladomokotlin.generator.types.ReladomoType
import io.github.reladomokotlin.generator.types.PrimitiveType
import java.io.File
import java.time.Instant

/**
 * Enhanced Kotlin code generator with type safety and validation
 */
class EnhancedKotlinCodeGenerator {
    
    fun generateKotlinWrapper(
        parsedObject: ParsedMithraObject,
        outputDir: File
    ): Result<GeneratedFiles> {
        return Result.runCatching {
            val wrapperFile = generateWrapperClass(parsedObject, outputDir)
            val repositoryFile = generateRepository(parsedObject, outputDir)
            val builderFile = if (parsedObject.objectType.isTransactional) {
                generateBuilder(parsedObject, outputDir)
            } else null
            
            GeneratedFiles(
                wrapper = wrapperFile.getOrThrow(),
                repository = repositoryFile.getOrThrow(),
                builder = builderFile?.getOrThrow()
            )
        }
    }
    
    private fun generateWrapperClass(
        parsedObject: ParsedMithraObject,
        outputDir: File
    ): Result<File> {
        return Result.runCatching {
            val className = "${parsedObject.className}Kt"
            val fileSpec = FileSpec.builder(parsedObject.packageName + ".kotlin", className)
                .addType(buildWrapperClass(parsedObject, className))
                .addFunction(buildToReladomoFunction(parsedObject))
                .build()
                
            val outputFile = File(outputDir, fileSpec.relativePath)
            outputFile.parentFile.mkdirs()
            fileSpec.writeTo(outputDir)
            outputFile
        }
    }
    
    private fun buildWrapperClass(parsedObject: ParsedMithraObject, className: String): TypeSpec {
        val builder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addKdoc("Kotlin wrapper for ${parsedObject.className} Reladomo object")
        
        // Add constructor parameters for all simple attributes
        val constructorBuilder = FunSpec.constructorBuilder()
        
        parsedObject.simpleAttributes.forEach { attr ->
            val paramSpec = ParameterSpec.builder(
                attr.name, 
                attr.type.toKotlinType(attr.nullable)
            ).build()
            
            constructorBuilder.addParameter(paramSpec)
            builder.addProperty(
                PropertySpec.builder(attr.name, attr.type.toKotlinType(attr.nullable))
                    .initializer(attr.name)
                    .build()
            )
        }
        
        // Add temporal properties if needed
        if (parsedObject.objectType.isTemporal) {
            val instantType = Instant::class.asTypeName()
            
            parsedObject.businessDateAttribute?.let {
                constructorBuilder.addParameter("businessDate", instantType)
                builder.addProperty(
                    PropertySpec.builder("businessDate", instantType)
                        .initializer("businessDate")
                        .build()
                )
            }
            
            parsedObject.processingDateAttribute?.let {
                constructorBuilder.addParameter("processingDate", instantType)
                builder.addProperty(
                    PropertySpec.builder("processingDate", instantType)
                        .initializer("processingDate")
                        .build()
                )
            }
        }
        
        builder.primaryConstructor(constructorBuilder.build())
        
        // Add interface implementations
        when (parsedObject.objectType) {
            ObjectType.BITEMPORAL -> {
                builder.addSuperinterface(
                    ClassName("io.github.reladomokotlin.core", "BiTemporalEntity")
                )
            }
            ObjectType.DATED_TRANSACTIONAL -> {
                builder.addSuperinterface(
                    ClassName("io.github.reladomokotlin.core", "DatedEntity")
                )
            }
            else -> {}
        }
        
        // Add validation in init block
        builder.addInitializerBlock(
            buildValidationBlock(parsedObject)
        )
        
        return builder.build()
    }
    
    private fun buildValidationBlock(parsedObject: ParsedMithraObject): CodeBlock {
        return buildCodeBlock {
            parsedObject.primaryKeyAttributes.forEach { attr ->
                if (!attr.nullable) {
                    when (val type = attr.type) {
                        is ReladomoType.Primitive -> when (type.type) {
                            is PrimitiveType.String -> {
                                addStatement("require(${attr.name}.isNotBlank()) { %S }", 
                                    "Primary key ${attr.name} cannot be blank")
                            }
                            is PrimitiveType.Long, is PrimitiveType.Int -> {
                                addStatement("require(${attr.name} > 0) { %S }", 
                                    "Primary key ${attr.name} must be positive")
                            }
                            else -> {}
                        }
                        else -> {}
                    }
                }
            }
            
            // Validate temporal constraints
            if (parsedObject.objectType == ObjectType.BITEMPORAL) {
                addStatement("require(businessDate <= processingDate) { %S }", 
                    "Business date cannot be after processing date")
            }
        }
    }
    
    private fun buildToReladomoFunction(parsedObject: ParsedMithraObject): FunSpec {
        val reladomoClassName = ClassName(parsedObject.packageName, parsedObject.className)
        
        return FunSpec.builder("toReladomo")
            .receiver(ClassName(parsedObject.packageName + ".kotlin", "${parsedObject.className}Kt"))
            .returns(reladomoClassName)
            .addCode(buildToReladomoBody(parsedObject))
            .build()
    }
    
    private fun buildToReladomoBody(parsedObject: ParsedMithraObject): CodeBlock {
        return buildCodeBlock {
            val reladomoClassName = ClassName(parsedObject.packageName, parsedObject.className)
            
            when (parsedObject.objectType) {
                ObjectType.BITEMPORAL -> {
                    addStatement("val reladomoObject = %T(businessDate, processingDate)", reladomoClassName)
                }
                ObjectType.DATED_TRANSACTIONAL -> {
                    addStatement("val reladomoObject = %T(businessDate)", reladomoClassName)
                }
                else -> {
                    addStatement("val reladomoObject = %T()", reladomoClassName)
                }
            }
            
            // Set all non-primary key attributes
            parsedObject.nonPrimaryKeyAttributes.forEach { attr ->
                val setterName = "set${attr.name.capitalize()}"
                addStatement("reladomoObject.$setterName($${attr.name})")
            }
            
            // Set primary key attributes if not identity
            parsedObject.primaryKeyAttributes.filter { !it.identity }.forEach { attr ->
                val setterName = "set${attr.name.capitalize()}"
                addStatement("reladomoObject.$setterName($${attr.name})")
            }
            
            addStatement("return reladomoObject")
        }
    }
    
    private fun generateRepository(
        parsedObject: ParsedMithraObject,
        outputDir: File
    ): Result<File> {
        return Result.runCatching {
            val repositoryName = "${parsedObject.className}KtRepository"
            val packageName = parsedObject.packageName + ".kotlin.repository"
            
            val fileSpec = FileSpec.builder(packageName, repositoryName)
                .addType(buildRepositoryInterface(parsedObject, repositoryName))
                .addType(buildRepositoryImplementation(parsedObject, repositoryName))
                .build()
                
            val outputFile = File(outputDir, fileSpec.relativePath)
            outputFile.parentFile.mkdirs()
            fileSpec.writeTo(outputDir)
            outputFile
        }
    }
    
    private fun buildRepositoryInterface(
        parsedObject: ParsedMithraObject,
        repositoryName: String
    ): TypeSpec {
        val wrapperType = ClassName(parsedObject.packageName + ".kotlin", "${parsedObject.className}Kt")
        val queryBuilderType = ClassName(parsedObject.packageName + ".kotlin.query", "${parsedObject.className}KtQueryBuilder")
        
        val builder = TypeSpec.interfaceBuilder(repositoryName)
            .addKdoc("Repository interface for ${parsedObject.className}")
        
        // Basic CRUD operations
        builder.addFunction(
            FunSpec.builder("findById")
                .addModifiers(KModifier.ABSTRACT)
                .addParameter("id", Long::class)
                .returns(wrapperType.copy(nullable = true))
                .build()
        )
        
        builder.addFunction(
            FunSpec.builder("findAll")
                .addModifiers(KModifier.ABSTRACT)
                .returns(List::class.asClassName().parameterizedBy(wrapperType))
                .build()
        )
        
        builder.addFunction(
            FunSpec.builder("save")
                .addModifiers(KModifier.ABSTRACT)
                .addParameter("entity", wrapperType)
                .returns(wrapperType)
                .build()
        )
        
        builder.addFunction(
            FunSpec.builder("deleteById")
                .addModifiers(KModifier.ABSTRACT)
                .addParameter("id", Long::class)
                .build()
        )
        
        // Query DSL operations
        builder.addFunction(
            FunSpec.builder("findOne")
                .addModifiers(KModifier.ABSTRACT)
                .addParameter("query", LambdaTypeName.get(
                    receiver = queryBuilderType,
                    returnType = Unit::class.asTypeName()
                ))
                .returns(wrapperType.copy(nullable = true))
                .build()
        )
        
        builder.addFunction(
            FunSpec.builder("findAll")
                .addModifiers(KModifier.ABSTRACT)
                .addParameter("query", LambdaTypeName.get(
                    receiver = queryBuilderType,
                    returnType = Unit::class.asTypeName()
                ))
                .returns(List::class.asClassName().parameterizedBy(wrapperType))
                .build()
        )
        
        // Temporal operations if applicable
        if (parsedObject.objectType.isTemporal) {
            builder.addFunction(
                FunSpec.builder("findByIdAsOf")
                    .addModifiers(KModifier.ABSTRACT)
                    .addParameter("id", Long::class)
                    .addParameter("businessDate", Instant::class)
                    .apply {
                        if (parsedObject.objectType == ObjectType.BITEMPORAL) {
                            addParameter("processingDate", Instant::class)
                        }
                    }
                    .returns(wrapperType.copy(nullable = true))
                    .build()
            )
        }
        
        return builder.build()
    }
    
    private fun buildRepositoryImplementation(
        parsedObject: ParsedMithraObject,
        repositoryName: String
    ): TypeSpec {
        val implName = "${repositoryName}Impl"
        
        return TypeSpec.classBuilder(implName)
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(ClassName(parsedObject.packageName + ".kotlin.repository", repositoryName))
            .addAnnotation(
                AnnotationSpec.builder(ClassName("org.springframework.stereotype", "Repository"))
                    .build()
            )
            .addKdoc("Default implementation of $repositoryName")
            .build()
    }
    
    private fun generateBuilder(
        parsedObject: ParsedMithraObject,
        outputDir: File
    ): Result<File> {
        return Result.runCatching {
            val builderName = "${parsedObject.className}KtBuilder"
            val packageName = parsedObject.packageName + ".kotlin.builder"
            
            val fileSpec = FileSpec.builder(packageName, builderName)
                .addType(buildBuilderClass(parsedObject, builderName))
                .build()
                
            val outputFile = File(outputDir, fileSpec.relativePath)
            outputFile.parentFile.mkdirs()
            fileSpec.writeTo(outputDir)
            outputFile
        }
    }
    
    private fun buildBuilderClass(
        parsedObject: ParsedMithraObject,
        builderName: String
    ): TypeSpec {
        // Implementation of type-safe builder pattern
        // This would be extensive, so showing a simplified version
        return TypeSpec.classBuilder(builderName)
            .addKdoc("Type-safe builder for ${parsedObject.className}")
            .build()
    }
    
    data class GeneratedFiles(
        val wrapper: File,
        val repository: File,
        val builder: File?
    )
}