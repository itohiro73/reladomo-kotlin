package io.github.kotlinreladomo.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kotlinreladomo.core.BiTemporalEntity
import io.github.kotlinreladomo.generator.model.AttributeDefinition
import io.github.kotlinreladomo.generator.model.MithraObjectDefinition
import java.io.File
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

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
                if (definition.isBitemporal) {
                    addImport("io.github.kotlinreladomo.core", "BiTemporalEntity")
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
            val property = PropertySpec.builder(attr.name, propertyType)
                .initializer(attr.name)
                .build()
            
            builder.addProperty(property)
            constructorBuilder.addParameter(attr.name, propertyType)
        }
        
        // Add bitemporal properties if needed
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
            .addStatement("val obj = %T()", reladomoClassName)
            .apply {
                // Set regular attributes
                definition.attributes.forEach { attr ->
                    when (attr.javaType) {
                        "Timestamp" -> {
                            if (attr.nullable) {
                                addStatement("obj.${attr.name} = this.${attr.name}?.let { Timestamp.from(it) }")
                            } else {
                                addStatement("obj.${attr.name} = Timestamp.from(this.${attr.name})")
                            }
                        }
                        else -> addStatement("obj.${attr.name} = this.${attr.name}")
                    }
                }
                
                // Set bitemporal attributes
                if (definition.isBitemporal) {
                    addStatement("obj.businessDate = Timestamp.from(this.businessDate)")
                    addStatement("obj.processingDate = Timestamp.from(this.processingDate)")
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
            .addStatement("return %T(", wrapperClassName)
            .apply {
                // Map regular attributes
                definition.attributes.forEach { attr ->
                    val conversion = when (attr.javaType) {
                        "Timestamp" -> {
                            if (attr.nullable) {
                                "obj.${attr.name}?.toInstant()"
                            } else {
                                "obj.${attr.name}.toInstant()"
                            }
                        }
                        else -> "obj.${attr.name}"
                    }
                    addStatement("    ${attr.name} = $conversion,")
                }
                
                // Map bitemporal attributes
                if (definition.isBitemporal) {
                    addStatement("    businessDate = obj.businessDate.toInstant(),")
                    addStatement("    processingDate = obj.processingDate.toInstant()")
                } else {
                    // Remove trailing comma from last attribute
                    val lastStatement = statements.last()
                    statements[statements.lastIndex] = lastStatement.toString().trimEnd(',')
                }
            }
            .addStatement(")")
            .build()
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
            "Date", "Timestamp" -> Instant::class.asTypeName()
            "BigDecimal" -> BigDecimal::class.asTypeName()
            else -> ClassName("", attribute.javaType)
        }
        
        return if (attribute.nullable) baseType.copy(nullable = true) else baseType
    }
}