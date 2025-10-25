package io.github.reladomokotlin.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.generator.model.AttributeDefinition
import io.github.reladomokotlin.generator.model.MithraObjectDefinition
import java.io.File
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date

/**
 * Generates Kotlin wrapper classes from Mithra object definitions.
 */
class KotlinWrapperGenerator {
    
    /**
     * Generate a Kotlin wrapper class for a Mithra object definition.
     */
    fun generate(definition: MithraObjectDefinition): FileSpec {
        val className = "${definition.className}Kt"
        val packageName = "${definition.packageName}.kotlin"
        
        return FileSpec.builder(packageName, className)
            .addType(generateDataClass(definition, className))
            .addImport("java.time", "Instant")
            .addImport("java.sql", "Timestamp")
            .apply {
                if (definition.attributes.any { it.javaType == "BigDecimal" }) {
                    addImport("java.math", "BigDecimal")
                }
                if (definition.attributes.any { it.javaType == "Date" }) {
                    addImport("java.util", "Date")
                    addImport("java.time", "LocalDate")
                }
                if (definition.attributes.any { it.javaType == "Time" }) {
                    addImport("java.sql", "Time")
                    addImport("java.time", "LocalTime")
                }
                if (definition.isBitemporal) {
                    addImport("io.github.reladomokotlin.core", "BiTemporalEntity")
                }
            }
            .build()
    }
    
    /**
     * Generate and write the Kotlin file to a directory.
     */
    fun generateToFile(definition: MithraObjectDefinition, outputDirectory: File): File {
        val fileSpec = generate(definition)
        fileSpec.writeTo(outputDirectory)
        
        val packagePath = fileSpec.packageName.replace('.', '/')
        return File(outputDirectory, "$packagePath/${fileSpec.name}.kt")
    }
    
    private fun generateDataClass(
        definition: MithraObjectDefinition,
        className: String
    ): TypeSpec {
        val builder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
        
        // Add BiTemporalEntity interface if applicable
        if (definition.isBitemporal) {
            builder.addSuperinterface(BiTemporalEntity::class)
        }
        
        // Create constructor
        val constructorBuilder = FunSpec.constructorBuilder()
        
        // Add regular attributes
        definition.attributes.forEach { attr ->
            val propertyType = mapToKotlinType(attr)
            val fieldName = toCamelCase(attr.name)
            val property = PropertySpec.builder(fieldName, propertyType)
                .initializer(fieldName)
                .build()
            
            builder.addProperty(property)
            constructorBuilder.addParameter(fieldName, propertyType)
        }
        
        // Add temporal properties if needed
        if (definition.isBitemporal) {
            val businessDateProp = PropertySpec.builder("businessDate", Instant::class)
                .initializer("businessDate")
                .addModifiers(KModifier.OVERRIDE)
                .build()

            val processingDateProp = PropertySpec.builder("processingDate", Instant::class)
                .initializer("processingDate")
                .addModifiers(KModifier.OVERRIDE)
                .build()

            builder.addProperty(businessDateProp)
            builder.addProperty(processingDateProp)

            constructorBuilder.addParameter("businessDate", Instant::class)
            constructorBuilder.addParameter("processingDate", Instant::class)
        } else if (definition.isUnitemporal) {
            val businessDateProp = PropertySpec.builder("businessDate", Instant::class)
                .initializer("businessDate")
                .build()

            builder.addProperty(businessDateProp)
            constructorBuilder.addParameter("businessDate", Instant::class)
        }
        
        builder.primaryConstructor(constructorBuilder.build())
        
        // Add companion object with conversion methods
        builder.addType(generateCompanionObject(definition, className))
        
        // Add conversion method to Reladomo object
        builder.addFunction(generateToReladomoMethod(definition))
        
        return builder.build()
    }
    
    private fun generateCompanionObject(
        definition: MithraObjectDefinition,
        className: String
    ): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunction(generateFromReladomoMethod(definition, className))
            .build()
    }
    
    private fun generateToReladomoMethod(definition: MithraObjectDefinition): FunSpec {
        val reladomoClassName = ClassName(definition.packageName, definition.className)
        
        return FunSpec.builder("toReladomo")
            .returns(reladomoClassName)
            .apply {
                // For temporal objects, use constructor with dates
                if (definition.isBitemporal) {
                    addStatement("val obj = %T(Timestamp.from(this.businessDate), Timestamp.from(this.processingDate))", reladomoClassName)
                } else if (definition.isUnitemporal) {
                    addStatement("val obj = %T(Timestamp.from(this.businessDate))", reladomoClassName)
                } else {
                    addStatement("val obj = %T()", reladomoClassName)
                }
                
                // Set regular attributes
                definition.attributes.forEach { attr ->
                    val fieldName = toCamelCase(attr.name)
                    when {
                        attr.javaType == "Timestamp" -> {
                            if (attr.nullable) {
                                addStatement("obj.${attr.name} = this.${fieldName}?.let { Timestamp.from(it) }")
                            } else {
                                addStatement("obj.${attr.name} = Timestamp.from(this.${fieldName})")
                            }
                        }
                        attr.javaType == "Date" -> {
                            if (attr.nullable) {
                                addStatement("obj.${attr.name} = this.${fieldName}?.let { Date.from(it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()) }")
                            } else {
                                addStatement("obj.${attr.name} = Date.from(this.${fieldName}.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())")
                            }
                        }
                        attr.javaType == "Time" -> {
                            if (attr.nullable) {
                                addStatement("obj.${attr.name} = this.${fieldName}?.let { java.sql.Time.valueOf(it) }")
                            } else {
                                addStatement("obj.${attr.name} = java.sql.Time.valueOf(this.${fieldName})")
                            }
                        }
                        attr.nullable || (attr.isPrimaryKey && attr.javaType == "long") -> {
                            // Handle nullable attributes and potentially nullable primary keys
                            addStatement("this.${fieldName}?.let { obj.${attr.name} = it }")
                        }
                        else -> addStatement("obj.${attr.name} = this.${fieldName}")
                    }
                }
            }
            .addStatement("return obj")
            .build()
    }
    
    private fun generateFromReladomoMethod(
        definition: MithraObjectDefinition,
        className: String
    ): FunSpec {
        val reladomoClassName = ClassName(definition.packageName, definition.className)
        val wrapperClassName = ClassName("", className)
        
        return FunSpec.builder("fromReladomo")
            .addParameter("obj", reladomoClassName)
            .returns(wrapperClassName)
            .addCode(buildCodeBlock {
                add("return %T(\n", wrapperClassName)
                indent()
                
                // Map regular attributes
                definition.attributes.forEachIndexed { index, attr ->
                    val conversion = when (attr.javaType) {
                        "Timestamp" -> {
                            if (attr.nullable) {
                                "obj.${attr.name}?.toInstant()"
                            } else {
                                "obj.${attr.name}.toInstant()"
                            }
                        }
                        "Date" -> {
                            if (attr.nullable) {
                                "obj.${attr.name}?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()"
                            } else {
                                "obj.${attr.name}.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()"
                            }
                        }
                        "Time" -> {
                            if (attr.nullable) {
                                "obj.${attr.name}?.toLocalTime()"
                            } else {
                                "obj.${attr.name}.toLocalTime()"
                            }
                        }
                        else -> "obj.${attr.name}"
                    }
                    
                    val fieldName = toCamelCase(attr.name)
                    add("${fieldName} = $conversion")

                    // Add comma if not last attribute or if temporal
                    if (index < definition.attributes.size - 1 || definition.isBitemporal || definition.isUnitemporal) {
                        add(",")
                    }
                    add("\n")
                }
                
                // Map temporal attributes
                if (definition.isBitemporal) {
                    add("businessDate = obj.businessDate.toInstant(),\n")
                    add("processingDate = obj.processingDate.toInstant()\n")
                } else if (definition.isUnitemporal) {
                    add("businessDate = obj.businessDate.toInstant()\n")
                }
                
                unindent()
                add(")")
            })
            .build()
    }
    
    private fun toCamelCase(name: String): String {
        // If the name contains underscores, convert from snake_case
        if (name.contains('_')) {
            return name.split('_').mapIndexed { index, part ->
                if (index == 0) part.lowercase()
                else part.lowercase().replaceFirstChar { it.uppercase() }
            }.joinToString("")
        }
        // Otherwise, assume it's already in camelCase and return as-is
        return name
    }
    
    private fun mapToKotlinType(attribute: AttributeDefinition): TypeName {
        val baseType = when (attribute.javaType) {
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
        
        return if (attribute.nullable || attribute.isPrimaryKey) baseType.copy(nullable = true) else baseType
    }
}