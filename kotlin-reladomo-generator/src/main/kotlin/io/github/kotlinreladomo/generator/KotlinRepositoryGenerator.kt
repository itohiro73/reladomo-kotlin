package io.github.kotlinreladomo.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kotlinreladomo.generator.model.AttributeDefinition
import io.github.kotlinreladomo.generator.model.MithraObjectDefinition
import java.io.File

/**
 * Generates Kotlin repository classes for Mithra objects.
 */
class KotlinRepositoryGenerator {
    
    /**
     * Generate a repository class for a Mithra object definition.
     */
    fun generate(definition: MithraObjectDefinition): FileSpec {
        val entityName = "${definition.className}Kt"
        val repositoryName = "${entityName}Repository"
        val packageName = "${definition.packageName}.kotlin.repository"
        
        return FileSpec.builder(packageName, repositoryName)
            .addType(generateRepositoryClass(definition, entityName, repositoryName))
            .addImport("org.springframework.stereotype", "Repository")
            .addImport(definition.packageName, definition.className)
            .addImport("${definition.packageName}.finder", "${definition.className}Finder")
            .addImport("com.gs.fw.finder", "Operation")
            .addImport("java.time", "Instant")
            .addImport("java.sql", "Timestamp")
            .addImport("io.github.kotlinreladomo.core", "AbstractBiTemporalRepository")
            .addImport("${definition.packageName}.kotlin", entityName)
            .build()
    }
    
    /**
     * Generate and write the repository file to a directory.
     */
    fun generateToFile(definition: MithraObjectDefinition, outputDirectory: File): File {
        val fileSpec = generate(definition)
        fileSpec.writeTo(outputDirectory)
        
        val packagePath = fileSpec.packageName.replace('.', '/')
        return File(outputDirectory, "$packagePath/${fileSpec.name}.kt")
    }
    
    private fun generateRepositoryClass(
        definition: MithraObjectDefinition,
        entityName: String,
        repositoryName: String
    ): TypeSpec {
        val primaryKeyType = findPrimaryKeyType(definition)
        val reladomoType = ClassName(definition.packageName, definition.className)
        val entityType = ClassName("${definition.packageName}.kotlin", entityName)
        val finderType = ClassName("${definition.packageName}.finder", "${definition.className}Finder")
        
        val superclass = ClassName("io.github.kotlinreladomo.core", "AbstractBiTemporalRepository")
            .parameterizedBy(entityType, primaryKeyType, reladomoType)
        
        return TypeSpec.classBuilder(repositoryName)
            .addAnnotation(ClassName("org.springframework.stereotype", "Repository"))
            .superclass(superclass)
            .addFunction(generateGetFinderMethod(finderType))
            .addFunction(generateToEntityMethod(definition, entityType))
            .addFunction(generateFromEntityMethod(definition, entityType))
            .addFunction(generateGetPrimaryKeyMethod(definition, primaryKeyType))
            .addFunction(generateCreatePrimaryKeyOperationMethod(definition, primaryKeyType, finderType))
            .apply {
                if (definition.isBitemporal) {
                    addFunction(generateGetBusinessDateAttributeMethod(finderType))
                    addFunction(generateGetProcessingDateAttributeMethod(finderType))
                }
                
                // Add custom finder methods
                addFunction(generateFindByCustomerIdMethod(definition, entityType, finderType))
            }
            .build()
    }
    
    private fun findPrimaryKeyType(definition: MithraObjectDefinition): TypeName {
        val primaryKeys = definition.primaryKeyAttributes
        
        return when {
            primaryKeys.isEmpty() -> throw IllegalArgumentException("No primary key found for ${definition.className}")
            primaryKeys.size == 1 -> mapToKotlinType(primaryKeys.first())
            else -> {
                // For composite keys, we'll use a String representation for now
                // In a full implementation, we'd generate a composite key class
                String::class.asTypeName()
            }
        }
    }
    
    private fun generateGetFinderMethod(finderType: ClassName): FunSpec {
        return FunSpec.builder("getFinder")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .returns(finderType)
            .addStatement("return %T", finderType)
            .build()
    }
    
    private fun generateToEntityMethod(definition: MithraObjectDefinition, entityType: ClassName): FunSpec {
        val reladomoType = ClassName(definition.packageName, definition.className)
        return FunSpec.builder("toEntity")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .addParameter("reladomoObject", reladomoType)
            .returns(entityType)
            .addStatement("return %T.fromReladomo(reladomoObject)", entityType)
            .build()
    }
    
    private fun generateFromEntityMethod(definition: MithraObjectDefinition, entityType: ClassName): FunSpec {
        val reladomoType = ClassName(definition.packageName, definition.className)
        return FunSpec.builder("fromEntity")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .addParameter("entity", entityType)
            .returns(reladomoType)
            .addStatement("return entity.toReladomo()")
            .build()
    }
    
    private fun generateGetPrimaryKeyMethod(
        definition: MithraObjectDefinition,
        primaryKeyType: TypeName
    ): FunSpec {
        val primaryKey = definition.primaryKeyAttributes.firstOrNull()
            ?: throw IllegalArgumentException("No primary key found")
        
        return FunSpec.builder("getPrimaryKey")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .addParameter("entity", ClassName("", "${definition.className}Kt"))
            .returns(primaryKeyType)
            .addStatement("return entity.${primaryKey.name}")
            .build()
    }
    
    private fun generateCreatePrimaryKeyOperationMethod(
        definition: MithraObjectDefinition,
        primaryKeyType: TypeName,
        finderType: ClassName
    ): FunSpec {
        val primaryKey = definition.primaryKeyAttributes.firstOrNull()
            ?: throw IllegalArgumentException("No primary key found")
        
        return FunSpec.builder("createPrimaryKeyOperation")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .addParameter("id", primaryKeyType)
            .returns(ClassName("com.gs.fw.finder", "Operation"))
            .addStatement("return %T.${primaryKey.name}().eq(id)", finderType)
            .build()
    }
    
    private fun generateGetBusinessDateAttributeMethod(finderType: ClassName): FunSpec {
        return FunSpec.builder("getBusinessDateAttribute")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .returns(ClassName("com.gs.fw.finder", "TimestampAttribute").parameterizedBy(STAR))
            .addStatement("return %T.businessDate()", finderType)
            .build()
    }
    
    private fun generateGetProcessingDateAttributeMethod(finderType: ClassName): FunSpec {
        return FunSpec.builder("getProcessingDateAttribute")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .returns(ClassName("com.gs.fw.finder", "TimestampAttribute").parameterizedBy(STAR))
            .addStatement("return %T.processingDate()", finderType)
            .build()
    }
    
    private fun mapToKotlinType(attribute: AttributeDefinition): TypeName {
        return when (attribute.javaType) {
            "boolean" -> BOOLEAN
            "byte" -> BYTE
            "short" -> SHORT
            "int" -> INT
            "long" -> LONG
            "float" -> FLOAT
            "double" -> DOUBLE
            "String" -> STRING
            else -> ClassName("", attribute.javaType)
        }
    }
    
    private fun generateFindByCustomerIdMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        // Only generate if there's a customerId attribute
        val hasCustomerId = definition.attributes.any { it.name == "customerId" }
        if (!hasCustomerId) {
            return FunSpec.builder("findByCustomerId")
                .addParameter("customerId", LONG)
                .returns(LIST.parameterizedBy(entityType))
                .addStatement("return emptyList()")
                .build()
        }
        
        return FunSpec.builder("findByCustomerId")
            .addParameter("customerId", LONG)
            .returns(LIST.parameterizedBy(entityType))
            .addStatement("val operation = %T.customerId().eq(customerId)", finderType)
            .addStatement("return findMany(operation)")
            .build()
    }
}