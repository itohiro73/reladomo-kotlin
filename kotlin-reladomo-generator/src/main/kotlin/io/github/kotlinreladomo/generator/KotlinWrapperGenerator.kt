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
            .apply {
                // For bitemporal objects, use constructor with dates
                if (definition.isBitemporal) {
                    addStatement("val obj = %T(Timestamp.from(this.businessDate), Timestamp.from(this.processingDate))", reladomoClassName)
                } else {
                    addStatement("val obj = %T()", reladomoClassName)
                }
                
                // Set regular attributes
                definition.attributes.forEach { attr ->
                    when {
                        attr.javaType == "Timestamp" -> {
                            if (attr.nullable) {
                                addStatement("obj.${attr.name} = this.${attr.name}?.let { Timestamp.from(it) }")
                            } else {
                                addStatement("obj.${attr.name} = Timestamp.from(this.${attr.name})")
                            }
                        }
                        attr.nullable || (attr.isPrimaryKey && attr.javaType == "long") -> {
                            // Handle nullable attributes and potentially nullable primary keys
                            addStatement("this.${attr.name}?.let { obj.${attr.name} = it }")
                        }
                        else -> addStatement("obj.${attr.name} = this.${attr.name}")
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
                        else -> "obj.${attr.name}"
                    }
                    
                    add("${attr.name} = $conversion")
                    
                    // Add comma if not last attribute or if bitemporal
                    if (index < definition.attributes.size - 1 || definition.isBitemporal) {
                        add(",")
                    }
                    add("\n")
                }
                
                // Map bitemporal attributes
                if (definition.isBitemporal) {
                    add("businessDate = obj.businessDate.toInstant(),\n")
                    add("processingDate = obj.processingDate.toInstant()\n")
                }
                
                unindent()
                add(")")
            })
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