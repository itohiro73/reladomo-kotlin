package io.github.kotlinreladomo.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kotlinreladomo.generator.model.AttributeDefinition
import io.github.kotlinreladomo.generator.model.MithraObjectDefinition
import java.io.File
import java.time.Instant

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
        val packageName = "${definition.packageName}.kotlin"
        
        return FileSpec.builder(packageName, repositoryName)
            .addType(generateRepositoryClass(definition, entityName, repositoryName))
            .addImport("org.springframework.stereotype", "Repository")
            .addImport(definition.packageName, definition.className)
            .addImport(definition.packageName, "${definition.className}Finder")
            .addImport("com.gs.fw.common.mithra.finder", "Operation")
            .addImport("com.gs.fw.common.mithra.attribute", "TimestampAttribute")
            .addImport("java.time", "Instant")
            .addImport("java.sql", "Timestamp")
            .addImport("com.gs.fw.common.mithra", "MithraManagerProvider")
            .addImport("org.springframework.transaction.annotation", "Transactional")
            .addImport("io.github.kotlinreladomo.core", "AbstractBiTemporalRepository")
            .addImport("io.github.kotlinreladomo.core", "ReladomoObject")
            .addImport("io.github.kotlinreladomo.core", "ReladomoFinder")
            .addImport("io.github.kotlinreladomo.core.exceptions", "EntityNotFoundException")
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
        val finderType = ClassName(definition.packageName, "${definition.className}Finder")
        
        val superclass = ClassName("io.github.kotlinreladomo.core", "AbstractBiTemporalRepository")
            .parameterizedBy(entityType, primaryKeyType, ClassName("io.github.kotlinreladomo.core", "ReladomoObject"))
        
        return TypeSpec.classBuilder(repositoryName)
            .addAnnotation(ClassName("org.springframework.stereotype", "Repository"))
            .addAnnotation(ClassName("org.springframework.transaction.annotation", "Transactional"))
            .addFunction(generateSaveMethod(definition, entityType, reladomoType))
            .addFunction(generateFindByIdMethod(definition, entityType, finderType, primaryKeyType))
            .addFunction(generateFindByIdAsOfMethod(definition, entityType, finderType, primaryKeyType))
            .addFunction(generateUpdateMethod(definition, entityType, finderType, primaryKeyType))
            .addFunction(generateDeleteByIdMethod(definition, finderType, primaryKeyType))
            .addFunction(generateFindAllMethod(definition, entityType, finderType))
            .addFunction(generateFindByCustomerIdMethod(definition, entityType, finderType))
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
    
    private fun generateSaveMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        reladomoType: ClassName
    ): FunSpec {
        return FunSpec.builder("save")
            .addParameter("entity", entityType)
            .returns(entityType)
            .addStatement("val reladomoObject = entity.toReladomo()")
            .addStatement("reladomoObject.insert()")
            .addStatement("return %T.fromReladomo(reladomoObject)", entityType)
            .build()
    }
    
    private fun generateFindByIdMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        val primaryKey = definition.primaryKeyAttributes.firstOrNull()
            ?: throw IllegalArgumentException("No primary key found")
            
        return if (definition.isBitemporal) {
            // For bitemporal objects, use edge point operations
            FunSpec.builder("findById")
                .addParameter("id", primaryKeyType)
                .returns(entityType.copy(nullable = true))
                .addComment("For bitemporal objects, find the current active record")
                .addComment("Use infinity processing date to get the latest version")
                .addStatement("val now = Timestamp.from(Instant.now())")
                .addStatement("val infinityTs = Timestamp.valueOf(\"9999-12-01 23:59:00.0\")")
                .addStatement("val order = %T.findByPrimaryKey(id, now, infinityTs)", finderType)
                .addStatement("return order?.let { %T.fromReladomo(it) }", entityType)
                .build()
        } else {
            // For non-temporal objects, use simple find
            FunSpec.builder("findById")
                .addParameter("id", primaryKeyType)
                .returns(entityType.copy(nullable = true))
                .addStatement("val order = %T.findByPrimaryKey(id)", finderType)
                .addStatement("return order?.let { %T.fromReladomo(it) }", entityType)
                .build()
        }
    }
    
    private fun generateFindByIdAsOfMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        return if (definition.isBitemporal) {
            FunSpec.builder("findByIdAsOf")
                .addParameter("id", primaryKeyType)
                .addParameter("businessDate", Instant::class)
                .addParameter("processingDate", Instant::class)
                .returns(entityType.copy(nullable = true))
                .addComment("Find by primary key as of specific business and processing dates")
                .addStatement("val order = %T.findByPrimaryKey(id, Timestamp.from(businessDate), Timestamp.from(processingDate))", finderType)
                .addStatement("return order?.let { %T.fromReladomo(it) }", entityType)
                .build()
        } else {
            // For non-temporal objects, ignore the dates
            FunSpec.builder("findByIdAsOf")
                .addParameter("id", primaryKeyType)
                .addParameter("businessDate", Instant::class)
                .addParameter("processingDate", Instant::class)
                .returns(entityType.copy(nullable = true))
                .addComment("For non-temporal objects, dates are ignored")
                .addStatement("val order = %T.findByPrimaryKey(id)", finderType)
                .addStatement("return order?.let { %T.fromReladomo(it) }", entityType)
                .build()
        }
    }
    
    private fun generateUpdateMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        val primaryKey = definition.primaryKeyAttributes.firstOrNull()
            ?: throw IllegalArgumentException("No primary key found")
            
        return if (definition.isBitemporal) {
            // For bitemporal objects, fetch as of specific business date and update
            FunSpec.builder("update")
                .addParameter("entity", entityType)
                .addParameter(ParameterSpec.builder("businessDate", Instant::class)
                    .defaultValue("Instant.now()")
                    .build())
                .returns(entityType)
                .addComment("For bitemporal objects, fetch as of the business date and update")
                .addComment("Processing date should be infinity to get the current active record")
                .addStatement("val businessDateTs = Timestamp.from(businessDate)")
                .addStatement("val infinityTs = Timestamp.valueOf(\"9999-12-01 23:59:00.0\")")
                .addStatement("")
                .addStatement("val existingOrder = %T.findByPrimaryKey(entity.${primaryKey.name}!!, businessDateTs, infinityTs)", finderType)
                .addStatement("    ?: throw EntityNotFoundException(\"Order not found with id: \${entity.${primaryKey.name}}\")")
                .addStatement("")
                .addComment("Update fields - Reladomo handles bitemporal chaining")
                .apply {
                    // Generate setters for all non-temporal attributes
                    definition.attributes.filter { !it.isPrimaryKey && it.name !in listOf("businessDate", "processingDate") }.forEach { attr ->
                        val setterName = "set${attr.name.capitalize()}"
                        when {
                            attr.nullable -> {
                                addStatement("entity.${attr.name}?.let { existingOrder.$setterName(it) }")
                            }
                            attr.javaType == "Timestamp" -> {
                                addStatement("existingOrder.$setterName(Timestamp.from(entity.${attr.name}!!))") 
                            }
                            else -> {
                                addStatement("existingOrder.$setterName(entity.${attr.name}!!)")
                            }
                        }
                    }
                }
                .addStatement("")
                .addStatement("return %T.fromReladomo(existingOrder)", entityType)
                .build()
        } else {
            // For non-temporal objects, simple update
            FunSpec.builder("update")
                .addParameter("entity", entityType)
                .returns(entityType)
                .addStatement("val existingOrder = %T.findByPrimaryKey(entity.${primaryKey.name}!!)", finderType)
                .addStatement("    ?: throw EntityNotFoundException(\"Order not found with id: \${entity.${primaryKey.name}}\")")
                .addStatement("")
                .addComment("Update attributes")
                .apply {
                    definition.attributes.filter { !it.isPrimaryKey }.forEach { attr ->
                        val setterName = "set${attr.name.capitalize()}"
                        when {
                            attr.nullable -> {
                                addStatement("entity.${attr.name}?.let { existingOrder.$setterName(it) }")
                            }
                            else -> {
                                addStatement("existingOrder.$setterName(entity.${attr.name}!!)")
                            }
                        }
                    }
                }
                .addStatement("")
                .addStatement("return %T.fromReladomo(existingOrder)", entityType)
                .build()
        }
    }
    
    private fun generateDeleteByIdMethod(
        definition: MithraObjectDefinition,
        finderType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        return if (definition.isBitemporal) {
            // For bitemporal objects, fetch as of specific business date and terminate
            FunSpec.builder("deleteById")
                .addParameter("id", primaryKeyType)
                .addParameter(ParameterSpec.builder("businessDate", Instant::class)
                    .defaultValue("Instant.now()")
                    .build())
                .addComment("For bitemporal objects, use findByPrimaryKey with specific business date")
                .addComment("Processing date should be infinity to get the current active record")
                .addStatement("val businessDateTs = Timestamp.from(businessDate)")
                .addStatement("val infinityTs = Timestamp.valueOf(\"9999-12-01 23:59:00.0\")")
                .addStatement("")
                .addStatement("val order = %T.findByPrimaryKey(id, businessDateTs, infinityTs)", finderType)
                .addStatement("    ?: throw EntityNotFoundException(\"Order not found with id: \$id\")")
                .addStatement("order.terminate()")
                .build()
        } else {
            // For non-temporal objects, simple delete
            FunSpec.builder("deleteById")
                .addParameter("id", primaryKeyType)
                .addStatement("val order = %T.findByPrimaryKey(id)", finderType)
                .addStatement("    ?: throw EntityNotFoundException(\"Order not found with id: \$id\")")
                .addStatement("order.delete()")
                .build()
        }
    }
    
    private fun generateFindAllMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("findAll")
            .returns(LIST.parameterizedBy(entityType))
            .addComment("For bitemporal queries, use the current time to get active records")
            .addStatement("val currentTime = Timestamp.from(Instant.now())")
            .addStatement("val operation = %T.businessDate().equalsEdgePoint()", finderType)
            .addStatement("    .and(%T.processingDate().equalsEdgePoint())", finderType)
            .addStatement("")
            .addStatement("val orders = %T.findMany(operation)", finderType)
            .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
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
            .addStatement("    .and(%T.businessDate().equalsEdgePoint())", finderType)
            .addStatement("    .and(%T.processingDate().equalsEdgePoint())", finderType)
            .addStatement("")
            .addStatement("val orders = %T.findMany(operation)", finderType)
            .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
            .build()
    }
}