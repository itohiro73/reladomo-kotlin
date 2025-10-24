package io.github.reladomokotlin.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.reladomokotlin.generator.model.AttributeDefinition
import io.github.reladomokotlin.generator.model.MithraObjectDefinition
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

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
            .addImport(definition.packageName, "${definition.className}Finder")
            .addImport("com.gs.fw.common.mithra.finder", "Operation")
            .addImport("com.gs.fw.common.mithra.attribute", "TimestampAttribute")
            .addImport("java.time", "Instant")
            .addImport("java.sql", "Timestamp")
            .addImport("com.gs.fw.common.mithra", "MithraManagerProvider")
            .addImport("org.springframework.transaction.annotation", "Transactional")
            .addImport("io.github.reladomokotlin.core", "BiTemporalRepository")
            .addImport("io.github.reladomokotlin.core", "BaseRepository")
            .addImport("io.github.reladomokotlin.core", "BiTemporalEntity")
            .addImport("io.github.reladomokotlin.core", "ReladomoObject")
            .addImport("io.github.reladomokotlin.core", "ReladomoFinder")
            .addImport("io.github.reladomokotlin.core.exceptions", "EntityNotFoundException")
            .addImport("${definition.packageName}.kotlin", entityName)
            .addImport("io.github.reladomokotlin.query", "QueryContext")
            .addImport("io.github.reladomokotlin.query", "query")
            .addImport("${definition.packageName}.kotlin.query", "${definition.className}QueryDsl")
            .addImport("org.springframework.beans.factory.annotation", "Autowired")
            .addImport("io.github.reladomokotlin.sequence", "SequenceGenerator")
            .apply {
                // Add imports for Date/Time types if needed
                if (definition.attributes.any { it.javaType == "Date" }) {
                    addImport("java.util", "Date")
                    addImport("java.time", "LocalDate")
                }
                if (definition.attributes.any { it.javaType == "Time" }) {
                    addImport("java.sql", "Time")
                    addImport("java.time", "LocalTime")
                }
            }
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
        
        val repositoryInterface = if (definition.isBitemporal) {
            ClassName("io.github.reladomokotlin.core", "BiTemporalRepository")
                .parameterizedBy(entityType, primaryKeyType)
        } else {
            ClassName("io.github.reladomokotlin.core", "BaseRepository")
                .parameterizedBy(entityType, primaryKeyType)
        }
        
        return TypeSpec.classBuilder(repositoryName)
            .addAnnotation(ClassName("org.springframework.stereotype", "Repository"))
            .addAnnotation(ClassName("org.springframework.transaction.annotation", "Transactional"))
            .addSuperinterface(repositoryInterface)
            .addProperty(
                PropertySpec.builder("sequenceGenerator", ClassName("io.github.reladomokotlin.sequence", "SequenceGenerator").copy(nullable = true))
                    .addModifiers(KModifier.PRIVATE)
                    .mutable()
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("org.springframework.beans.factory.annotation", "Autowired"))
                            .addMember("required = %L", false)
                            .build()
                    )
                    .initializer("null")
                    .build()
            )
            .addFunction(generateSaveMethod(definition, entityType, reladomoType))
            .addFunction(generateFindByIdMethod(definition, entityType, finderType, primaryKeyType))
            .addFunction(generateUpdateMethod(definition, entityType, finderType, primaryKeyType))
            .addFunction(generateDeleteByIdMethod(definition, finderType, primaryKeyType))
            .addFunction(generateFindAllMethod(definition, entityType, finderType))
            .addFunction(generateFindByMethod(definition, entityType, finderType))
            .addFunction(generateCountByMethod(definition, finderType))
            .addFunction(generateDeleteMethod(definition, entityType, primaryKeyType))
            .addFunction(generateDeleteAllMethod(definition, entityType, finderType))
            .addFunction(generateCountMethod(definition, entityType, finderType))
            .apply {
                // Add domain-specific methods if applicable
                if (definition.className == "Order") {
                    addFunction(generateFindByCustomerIdMethod(definition, entityType, finderType))
                }
                // Add bitemporal-specific methods only for bitemporal entities
                if (definition.isBitemporal) {
                    addFunction(generateFindByIdAsOfMethod(definition, entityType, finderType, primaryKeyType))
                    addFunction(generateUpdateNoDateMethod(definition, entityType, finderType, primaryKeyType))
                    addFunction(generateFindAllAsOfMethod(definition, entityType, finderType))
                    addFunction(generateGetHistoryMethod(definition, entityType, finderType, primaryKeyType))
                    addFunction(generateDeleteByIdAsOfMethod(definition, finderType, primaryKeyType))
                }
            }
            // Keep DSL methods
            .addFunction(generateFindWithDslMethod(definition, entityType, finderType))
            .addFunction(generateFindOneWithDslMethod(definition, entityType, finderType))
            .addFunction(generateCountWithDslMethod(definition, finderType))
            .addFunction(generateExistsWithDslMethod(definition, finderType))
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
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("entity", entityType)
            .returns(entityType)
            .apply {
                if (definition.isBitemporal) {
                    // For bitemporal entities, create with business date only constructor
                    // This allows Reladomo to set processing date to infinity automatically
                    addStatement("val obj = %T(Timestamp.from(entity.businessDate))", reladomoType)
                    // Set primary key if present and not null
                    val primaryKeys = definition.primaryKeyAttributes
                    if (primaryKeys.size == 1 && primaryKeys.first().javaType == "long") {
                        // Only use sequence generator for single long primary key
                        val primaryKey = primaryKeys.first()
                        addStatement("val ${primaryKey.name} = entity.${primaryKey.name}?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId(%S) ?: throw IllegalStateException(%S)", 
                            definition.className, 
                            "No ID provided and sequence generator not available")
                        addStatement("obj.${primaryKey.name} = ${primaryKey.name}")
                    } else {
                        // For composite keys or non-long keys, just set the values
                        primaryKeys.forEach { pk ->
                            addStatement("obj.${pk.name} = entity.${pk.name}")
                        }
                    }
                    // Set other attributes
                    definition.attributes.filter { !it.isPrimaryKey }.forEach { attr ->
                        when (attr.javaType) {
                            "Timestamp" -> {
                                if (attr.nullable) {
                                    addStatement("entity.${attr.name}?.let { obj.${attr.name} = Timestamp.from(it) }")
                                } else {
                                    addStatement("obj.${attr.name} = Timestamp.from(entity.${attr.name})")
                                }
                            }
                            else -> {
                                if (attr.nullable) {
                                    addStatement("entity.${attr.name}?.let { obj.${attr.name} = it }")
                                } else {
                                    addStatement("obj.${attr.name} = entity.${attr.name}")
                                }
                            }
                        }
                    }
                    addStatement("obj.insert()")
                    addStatement("return %T.fromReladomo(obj)", entityType)
                } else {
                    // For non-bitemporal entities
                    addStatement("val obj = %T()", reladomoType)
                    // Handle primary key
                    val primaryKeys = definition.primaryKeyAttributes
                    if (primaryKeys.size == 1 && primaryKeys.first().javaType == "long") {
                        // Only use sequence generator for single long primary key
                        val primaryKey = primaryKeys.first()
                        addStatement("val ${primaryKey.name} = entity.${primaryKey.name}?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId(%S) ?: throw IllegalStateException(%S)", 
                            definition.className, 
                            "No ID provided and sequence generator not available")
                        addStatement("obj.${primaryKey.name} = ${primaryKey.name}")
                    } else {
                        // For composite keys or non-long keys, just set the values
                        primaryKeys.forEach { pk ->
                            addStatement("obj.${pk.name} = entity.${pk.name}")
                        }
                    }
                    // Set other attributes
                    definition.attributes.filter { !it.isPrimaryKey }.forEach { attr ->
                        when (attr.javaType) {
                            "Timestamp" -> {
                                if (attr.nullable) {
                                    addStatement("entity.${attr.name}?.let { obj.${attr.name} = Timestamp.from(it) }")
                                } else {
                                    addStatement("obj.${attr.name} = Timestamp.from(entity.${attr.name})")
                                }
                            }
                            else -> {
                                if (attr.nullable) {
                                    addStatement("entity.${attr.name}?.let { obj.${attr.name} = it }")
                                } else {
                                    addStatement("obj.${attr.name} = entity.${attr.name}")
                                }
                            }
                        }
                    }
                    addStatement("obj.insert()")
                    addStatement("return %T.fromReladomo(obj)", entityType)
                }
            }
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
            // For bitemporal objects, use equalsEdgePoint to find the current active record
            FunSpec.builder("findById")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("id", primaryKeyType)
                .returns(entityType.copy(nullable = true))
                .addComment("For bitemporal objects, find the current active record using equalsEdgePoint")
                .addStatement("val operation = %T.${primaryKey.name}().eq(id)", finderType)
                .addStatement("    .and(%T.businessDate().equalsEdgePoint())", finderType)
                .addStatement("    .and(%T.processingDate().equalsEdgePoint())", finderType)
                .addStatement("val entity = %T.findOne(operation)", finderType)
                .addStatement("return entity?.let { %T.fromReladomo(it) }", entityType)
                .build()
        } else {
            // For non-temporal objects, use simple find
            FunSpec.builder("findById")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("id", primaryKeyType)
                .returns(entityType.copy(nullable = true))
                .addStatement("val entity = %T.findByPrimaryKey(id)", finderType)
                .addStatement("return entity?.let { %T.fromReladomo(it) }", entityType)
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
                .addModifiers(KModifier.OVERRIDE)
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
                .addModifiers(KModifier.OVERRIDE)
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
            // For bitemporal objects, find record with infinity processing date for updates
            FunSpec.builder("update")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("entity", entityType)
                .addParameter("businessDate", Instant::class)
                .returns(entityType)
                .addComment("For bitemporal objects, find record with infinity processing date at specified business date")
                .addStatement("val operation = %T.${primaryKey.name}().eq(entity.${primaryKey.name}!!)", finderType)
                .addStatement("    .and(%T.businessDate().eq(Timestamp.from(businessDate)))", finderType)
                .addStatement("    .and(%T.processingDate().equalsInfinity())", finderType)
                .addStatement("val existingEntity = %T.findOne(operation)", finderType)
                .addStatement("    ?: throw EntityNotFoundException(\"${definition.className} not found with id: \${entity.${primaryKey.name}}\")")
                .addStatement("")
                .addComment("Update fields - Reladomo handles bitemporal chaining")
                .apply {
                    // Generate setters for all non-temporal attributes
                    definition.attributes.filter { !it.isPrimaryKey && it.name !in listOf("businessDate", "processingDate") }.forEach { attr ->
                        val setterName = "set${attr.name.capitalize()}"
                        when {
                            attr.nullable -> {
                                when (attr.javaType) {
                                    "Timestamp" -> addStatement("entity.${attr.name}?.let { existingEntity.$setterName(Timestamp.from(it)) }")
                                    "Date" -> addStatement("entity.${attr.name}?.let { existingEntity.$setterName(java.util.Date.from(it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())) }")
                                    "Time" -> addStatement("entity.${attr.name}?.let { existingEntity.$setterName(java.sql.Time.valueOf(it)) }")
                                    else -> addStatement("entity.${attr.name}?.let { existingEntity.$setterName(it) }")
                                }
                            }
                            attr.javaType == "Timestamp" -> {
                                addStatement("existingEntity.$setterName(Timestamp.from(entity.${attr.name}))")
                            }
                            attr.javaType == "Date" -> {
                                addStatement("existingEntity.$setterName(java.util.Date.from(entity.${attr.name}.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))")
                            }
                            attr.javaType == "Time" -> {
                                addStatement("existingEntity.$setterName(java.sql.Time.valueOf(entity.${attr.name}))")
                            }
                            else -> {
                                addStatement("existingEntity.$setterName(entity.${attr.name})")
                            }
                        }
                    }
                }
                .addStatement("")
                .addStatement("return %T.fromReladomo(existingEntity)", entityType)
                .build()
        } else {
            // For non-temporal objects, simple update
            FunSpec.builder("update")
                .addModifiers(KModifier.OVERRIDE)
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
                                when (attr.javaType) {
                                    "Timestamp" -> addStatement("entity.${attr.name}?.let { existingOrder.$setterName(Timestamp.from(it)) }")
                                    "Date" -> addStatement("entity.${attr.name}?.let { existingOrder.$setterName(java.util.Date.from(it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())) }")
                                    "Time" -> addStatement("entity.${attr.name}?.let { existingOrder.$setterName(java.sql.Time.valueOf(it)) }")
                                    else -> addStatement("entity.${attr.name}?.let { existingOrder.$setterName(it) }")
                                }
                            }
                            attr.javaType == "Timestamp" -> {
                                addStatement("existingOrder.$setterName(Timestamp.from(entity.${attr.name}))")
                            }
                            attr.javaType == "Date" -> {
                                addStatement("existingOrder.$setterName(java.util.Date.from(entity.${attr.name}.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))")
                            }
                            attr.javaType == "Time" -> {
                                addStatement("existingOrder.$setterName(java.sql.Time.valueOf(entity.${attr.name}))")
                            }
                            else -> {
                                addStatement("existingOrder.$setterName(entity.${attr.name})")
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
            // For bitemporal objects, use current time for business date
            FunSpec.builder("deleteById")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("id", primaryKeyType)
                .addStatement("deleteByIdAsOf(id, Instant.now())")
                .build()
        } else {
            // For non-temporal objects, simple delete
            FunSpec.builder("deleteById")
                .addModifiers(KModifier.OVERRIDE)
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
        return if (definition.isBitemporal) {
            FunSpec.builder("findAll")
                .addModifiers(KModifier.OVERRIDE)
                .returns(LIST.parameterizedBy(entityType))
                .addComment("For bitemporal queries, use equalsEdgePoint to get active records")
                .addStatement("val operation = %T.businessDate().equalsEdgePoint()", finderType)
                .addStatement("    .and(%T.processingDate().equalsEdgePoint())", finderType)
                .addStatement("")
                .addStatement("val orders = %T.findMany(operation)", finderType)
                .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
                .build()
        } else {
            FunSpec.builder("findAll")
                .addModifiers(KModifier.OVERRIDE)
                .returns(LIST.parameterizedBy(entityType))
                .addStatement("val orders = %T.findMany(%T.all())", finderType, finderType)
                .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
                .build()
        }
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
            "Date" -> LocalDate::class.asTypeName()
            "Time" -> LocalTime::class.asTypeName()
            "Timestamp" -> Instant::class.asTypeName()
            "BigDecimal" -> BigDecimal::class.asTypeName()
            "byte[]" -> ByteArray::class.asTypeName()
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
        
        return if (definition.isBitemporal) {
            FunSpec.builder("findByCustomerId")
                .addParameter("customerId", LONG)
                .returns(LIST.parameterizedBy(entityType))
                .addStatement("val operation = %T.customerId().eq(customerId)", finderType)
                .addStatement("    .and(%T.businessDate().equalsEdgePoint())", finderType)
                .addStatement("    .and(%T.processingDate().equalsEdgePoint())", finderType)
                .addStatement("")
                .addStatement("val orders = %T.findMany(operation)", finderType)
                .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
                .build()
        } else {
            FunSpec.builder("findByCustomerId")
                .addParameter("customerId", LONG)
                .returns(LIST.parameterizedBy(entityType))
                .addStatement("val operation = %T.customerId().eq(customerId)", finderType)
                .addStatement("val orders = %T.findMany(operation)", finderType)
                .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
                .build()
        }
    }
    
    private fun generateFindWithDslMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("find")
            .addParameter(
                ParameterSpec.builder("query", LambdaTypeName.get(
                    receiver = ClassName("io.github.reladomokotlin.query", "QueryContext"),
                    returnType = UNIT
                )).build()
            )
            .returns(LIST.parameterizedBy(entityType))
            .addComment("Find entities using Query DSL")
            .addComment("Use ${definition.className}QueryDsl extensions to access attribute properties")
            .addStatement("val operation = io.github.reladomokotlin.query.query(query)")
            .addStatement("val results = %T.findMany(operation)", finderType)
            .addStatement("return results.map { %T.fromReladomo(it) }", entityType)
            .build()
    }
    
    private fun generateFindOneWithDslMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("findOne")
            .addParameter(
                ParameterSpec.builder("query", LambdaTypeName.get(
                    receiver = ClassName("io.github.reladomokotlin.query", "QueryContext"),
                    returnType = UNIT
                )).build()
            )
            .returns(entityType.copy(nullable = true))
            .addComment("Find a single entity using Query DSL")
            .addStatement("val operation = io.github.reladomokotlin.query.query(query)")
            .addStatement("val result = %T.findOne(operation)", finderType)
            .addStatement("return result?.let { %T.fromReladomo(it) }", entityType)
            .build()
    }
    
    private fun generateCountWithDslMethod(
        definition: MithraObjectDefinition,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("count")
            .addParameter(
                ParameterSpec.builder("query", LambdaTypeName.get(
                    receiver = ClassName("io.github.reladomokotlin.query", "QueryContext"),
                    returnType = UNIT
                )).build()
            )
            .returns(INT)
            .addComment("Count entities matching Query DSL criteria")
            .addStatement("val operation = io.github.reladomokotlin.query.query(query)")
            .addStatement("return %T.findMany(operation).size", finderType)
            .build()
    }
    
    private fun generateExistsWithDslMethod(
        definition: MithraObjectDefinition,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("exists")
            .addParameter(
                ParameterSpec.builder("query", LambdaTypeName.get(
                    receiver = ClassName("io.github.reladomokotlin.query", "QueryContext"),
                    returnType = UNIT
                )).build()
            )
            .returns(BOOLEAN)
            .addComment("Check if any entity exists matching Query DSL criteria")
            .addStatement("val operation = io.github.reladomokotlin.query.query(query)")
            .addStatement("return %T.findOne(operation) != null", finderType)
            .build()
    }
    
    private fun generateFindByMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("findBy")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("operation", ClassName("com.gs.fw.common.mithra.finder", "Operation"))
            .returns(LIST.parameterizedBy(entityType))
            .addStatement("val orders = %T.findMany(operation)", finderType)
            .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
            .build()
    }
    
    private fun generateCountByMethod(
        definition: MithraObjectDefinition,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("countBy")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("operation", ClassName("com.gs.fw.common.mithra.finder", "Operation"))
            .returns(LONG)
            .addStatement("return %T.findMany(operation).size.toLong()", finderType)
            .build()
    }
    
    private fun generateUpdateNoDateMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        return FunSpec.builder("update")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("entity", entityType)
            .returns(entityType)
            .addStatement("return update(entity, Instant.now())")
            .build()
    }
    
    private fun generateDeleteMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        val primaryKey = definition.primaryKeyAttributes.firstOrNull()
            ?: throw IllegalArgumentException("No primary key found")
            
        return FunSpec.builder("delete")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("entity", entityType)
            .addStatement("entity.${primaryKey.name}?.let { deleteById(it) }")
            .build()
    }
    
    private fun generateDeleteAllMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("deleteAll")
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("val allOrders = findAll()")
            .addStatement("allOrders.forEach { delete(it) }")
            .build()
    }
    
    private fun generateCountMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("count")
            .addModifiers(KModifier.OVERRIDE)
            .returns(LONG)
            .addStatement("return findAll().size.toLong()")
            .build()
    }
    
    private fun generateFindAllAsOfMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName
    ): FunSpec {
        return FunSpec.builder("findAllAsOf")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("businessDate", Instant::class)
            .addParameter("processingDate", Instant::class)
            .returns(LIST.parameterizedBy(entityType))
            .addStatement("val operation = %T.businessDate().eq(Timestamp.from(businessDate))", finderType)
            .addStatement("    .and(%T.processingDate().eq(Timestamp.from(processingDate)))", finderType)
            .addStatement("")
            .addStatement("val orders = %T.findMany(operation)", finderType)
            .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
            .build()
    }
    
    private fun generateGetHistoryMethod(
        definition: MithraObjectDefinition,
        entityType: ClassName,
        finderType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        val primaryKey = definition.primaryKeyAttributes.firstOrNull()
            ?: throw IllegalArgumentException("No primary key found")
            
        return FunSpec.builder("getHistory")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("id", primaryKeyType)
            .returns(LIST.parameterizedBy(entityType))
            .addComment("Get all versions of the entity across time")
            .addStatement("val operation = %T.${primaryKey.name}().eq(id)", finderType)
            .addStatement("val orders = %T.findMany(operation)", finderType)
            .addStatement("return orders.map { %T.fromReladomo(it) }", entityType)
            .build()
    }
    
    private fun generateDeleteByIdAsOfMethod(
        definition: MithraObjectDefinition,
        finderType: ClassName,
        primaryKeyType: TypeName
    ): FunSpec {
        val primaryKey = definition.primaryKeyAttributes.firstOrNull()
            ?: throw IllegalArgumentException("No primary key found")

        return FunSpec.builder("deleteByIdAsOf")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("id", primaryKeyType)
            .addParameter("businessDate", Instant::class)
            .addComment("For bitemporal objects, find record with infinity processing date at specified business date for termination")
            .addStatement("val operation = %T.${primaryKey.name}().eq(id)", finderType)
            .addStatement("    .and(%T.businessDate().eq(Timestamp.from(businessDate)))", finderType)
            .addStatement("    .and(%T.processingDate().equalsInfinity())", finderType)
            .addStatement("val entity = %T.findOne(operation)", finderType)
            .addStatement("    ?: throw EntityNotFoundException(\"${definition.className} not found with id: \$id\")")
            .addStatement("entity.terminate()")
            .build()
    }
}