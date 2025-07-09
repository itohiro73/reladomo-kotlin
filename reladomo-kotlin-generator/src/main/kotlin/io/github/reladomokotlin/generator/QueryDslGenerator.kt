package io.github.reladomokotlin.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.reladomokotlin.generator.model.AttributeDefinition
import io.github.reladomokotlin.generator.model.AsOfAttributeDefinition
import io.github.reladomokotlin.generator.model.MithraObjectDefinition
import java.io.File

/**
 * Generates Query DSL extensions for Mithra objects.
 */
class QueryDslGenerator {
    
    /**
     * Generate Query DSL extensions for a Mithra object definition.
     */
    fun generate(definition: MithraObjectDefinition): FileSpec {
        val dslFileName = "${definition.className}QueryDsl"
        val packageName = "${definition.packageName}.kotlin.query"
        
        return FileSpec.builder(packageName, dslFileName)
            .addFileComment("Generated Query DSL extensions for ${definition.className}")
            .addType(generateDslExtensions(definition))
            .addImport("io.github.reladomokotlin.query", "QueryContext")
            .addImport("io.github.reladomokotlin.query", "AttributeProperty")
            .addImport("io.github.reladomokotlin.query", "NumericAttributeProperty")
            .addImport("io.github.reladomokotlin.query", "StringAttributeProperty")
            .addImport("io.github.reladomokotlin.query", "TemporalAttributeProperty")
            .addImport("io.github.reladomokotlin.query", "AsOfAttributeProperty")
            .addImport("io.github.reladomokotlin.query", "attribute")
            .addImport("io.github.reladomokotlin.query", "intAttribute")
            .addImport("io.github.reladomokotlin.query", "longAttribute")
            .addImport("io.github.reladomokotlin.query", "doubleAttribute")
            .addImport("io.github.reladomokotlin.query", "floatAttribute")
            .addImport("io.github.reladomokotlin.query", "stringAttribute")
            .addImport("io.github.reladomokotlin.query", "timestampAttribute")
            .addImport("io.github.reladomokotlin.query", "dateAttribute")
            .addImport("io.github.reladomokotlin.query", "bigDecimalAttribute")
            .addImport("io.github.reladomokotlin.query", "asOfAttribute")
            .addImport(definition.packageName, "${definition.className}Finder")
            .apply {
                // Add imports for attribute types used
                val attributeTypes = mutableSetOf<String>()
                definition.attributes.forEach { attr ->
                    when (attr.javaType) {
                        "int", "Integer" -> attributeTypes.add("IntegerAttribute")
                        "long", "Long" -> attributeTypes.add("LongAttribute")
                        "float", "Float" -> attributeTypes.add("FloatAttribute")
                        "double", "Double" -> attributeTypes.add("DoubleAttribute")
                        "String" -> attributeTypes.add("StringAttribute")
                        "Timestamp" -> attributeTypes.add("TimestampAttribute")
                        "Date" -> attributeTypes.add("DateAttribute")
                        "BigDecimal" -> attributeTypes.add("BigDecimalAttribute")
                        "boolean", "Boolean" -> attributeTypes.add("BooleanAttribute")
                    }
                }
                attributeTypes.forEach { type ->
                    addImport("com.gs.fw.common.mithra.attribute", type)
                }
                
                // Add type imports
                if (definition.attributes.any { it.javaType == "Timestamp" }) {
                    addImport("java.sql", "Timestamp")
                }
                if (definition.attributes.any { it.javaType == "Date" }) {
                    addImport("java.util", "Date")
                }
                if (definition.attributes.any { it.javaType == "BigDecimal" }) {
                    addImport("java.math", "BigDecimal")
                }
                
                // Add Kotlin type imports for property types
                if (definition.attributes.any { it.javaType == "long" || it.javaType == "Long" }) {
                    addImport("kotlin", "Long")
                }
            }
            .build()
    }
    
    /**
     * Generate to file.
     */
    fun generateToFile(definition: MithraObjectDefinition, outputDir: File): File {
        val fileSpec = generate(definition)
        fileSpec.writeTo(outputDir)
        val packagePath = fileSpec.packageName.replace('.', '/')
        return File(outputDir, "$packagePath/${fileSpec.name}.kt")
    }
    
    private fun generateDslExtensions(definition: MithraObjectDefinition): TypeSpec {
        val extensionObjectName = "${definition.className}QueryDsl"
        
        return TypeSpec.objectBuilder(extensionObjectName)
            .addKdoc("Query DSL extensions for ${definition.className}")
            .apply {
                // Generate extension properties for each attribute
                definition.attributes.forEach { attribute ->
                    addProperty(generateAttributeProperty(attribute, definition))
                }
                
                // Add temporal attributes if bitemporal
                if (definition.isBitemporal) {
                    definition.asOfAttributes.forEach { asOfAttr ->
                        addProperty(generateAsOfAttributeProperty(asOfAttr, definition))
                    }
                }
            }
            .build()
    }
    
    private fun generateAttributeProperty(
        attribute: AttributeDefinition,
        definition: MithraObjectDefinition
    ): PropertySpec {
        val propertyName = toCamelCase(attribute.name)
        val finderClass = ClassName(definition.packageName, "${definition.className}Finder")
        
        // Determine the appropriate property type based on attribute type
        val (propertyType, helperFunction) = when (attribute.javaType) {
            "int", "Integer" -> {
                ClassName("io.github.reladomokotlin.query", "NumericAttributeProperty")
                    .parameterizedBy(INT, ClassName("com.gs.fw.common.mithra.attribute", "IntegerAttribute").parameterizedBy(STAR)) to "intAttribute"
            }
            "long", "Long" -> {
                ClassName("io.github.reladomokotlin.query", "NumericAttributeProperty")
                    .parameterizedBy(LONG, ClassName("com.gs.fw.common.mithra.attribute", "LongAttribute").parameterizedBy(STAR)) to "longAttribute"
            }
            "double", "Double" -> {
                ClassName("io.github.reladomokotlin.query", "NumericAttributeProperty")
                    .parameterizedBy(DOUBLE, ClassName("com.gs.fw.common.mithra.attribute", "DoubleAttribute").parameterizedBy(STAR)) to "doubleAttribute"
            }
            "float", "Float" -> {
                ClassName("io.github.reladomokotlin.query", "NumericAttributeProperty")
                    .parameterizedBy(FLOAT, ClassName("com.gs.fw.common.mithra.attribute", "FloatAttribute").parameterizedBy(STAR)) to "floatAttribute"
            }
            "String" -> {
                ClassName("io.github.reladomokotlin.query", "StringAttributeProperty") to "stringAttribute"
            }
            "Timestamp" -> {
                ClassName("io.github.reladomokotlin.query", "TemporalAttributeProperty")
                    .parameterizedBy(
                        ClassName("java.sql", "Timestamp"),
                        ClassName("com.gs.fw.common.mithra.attribute", "TimestampAttribute").parameterizedBy(STAR)
                    ) to "timestampAttribute"
            }
            "Date" -> {
                ClassName("io.github.reladomokotlin.query", "TemporalAttributeProperty")
                    .parameterizedBy(
                        ClassName("java.util", "Date"),
                        ClassName("com.gs.fw.common.mithra.attribute", "DateAttribute").parameterizedBy(STAR)
                    ) to "dateAttribute"
            }
            "BigDecimal" -> {
                ClassName("io.github.reladomokotlin.query", "NumericAttributeProperty")
                    .parameterizedBy(
                        ClassName("java.math", "BigDecimal"),
                        ClassName("com.gs.fw.common.mithra.attribute", "BigDecimalAttribute").parameterizedBy(STAR)
                    ) to "bigDecimalAttribute"
            }
            "boolean", "Boolean" -> {
                ClassName("io.github.reladomokotlin.query", "AttributeProperty")
                    .parameterizedBy(
                        BOOLEAN,
                        ClassName("com.gs.fw.common.mithra.attribute", "BooleanAttribute").parameterizedBy(STAR)
                    ) to "attribute"
            }
            else -> {
                ClassName("io.github.reladomokotlin.query", "AttributeProperty")
                    .parameterizedBy(
                        ClassName("", attribute.javaType),
                        ClassName("com.gs.fw.common.mithra.attribute", "Attribute")
                            .parameterizedBy(ClassName("", attribute.javaType))
                    ) to "attribute"
            }
        }
        
        return PropertySpec.builder(propertyName, propertyType)
            .receiver(ClassName("io.github.reladomokotlin.query", "QueryContext"))
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return %L(%T.%L())", helperFunction, finderClass, attribute.name)
                    .build()
            )
            .build()
    }
    
    private fun generateAsOfAttributeProperty(
        asOfAttr: AsOfAttributeDefinition,
        definition: MithraObjectDefinition
    ): PropertySpec {
        val propertyName = toCamelCase(asOfAttr.name)
        val finderClass = ClassName(definition.packageName, "${definition.className}Finder")
        val propertyType = ClassName("io.github.reladomokotlin.query", "AsOfAttributeProperty")
        
        return PropertySpec.builder(propertyName, propertyType)
            .receiver(ClassName("io.github.reladomokotlin.query", "QueryContext"))
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return asOfAttribute(%T.%L())", finderClass, asOfAttr.name)
                    .build()
            )
            .build()
    }
    
    private fun toCamelCase(name: String): String {
        if (name.contains('_')) {
            return name.split('_').mapIndexed { index, part ->
                if (index == 0) part.lowercase()
                else part.lowercase().replaceFirstChar { it.uppercase() }
            }.joinToString("")
        }
        return name
    }
}