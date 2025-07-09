package io.github.reladomokotlin.generator.query

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.reladomokotlin.generator.model.ParsedMithraObject
import io.github.reladomokotlin.generator.types.*
import java.math.BigDecimal
import java.time.Instant

/**
 * Generator for type-safe query DSL
 */
class QueryDslGenerator {
    
    fun generateQueryBuilder(
        parsedObject: ParsedMithraObject,
        outputPackage: String
    ): TypeSpec {
        val builderName = "${parsedObject.className}KtQueryBuilder"
        
        return TypeSpec.classBuilder(builderName)
            .addKdoc("Type-safe query builder for ${parsedObject.className}")
            .addProperty(
                PropertySpec.builder("predicates", 
                    MUTABLE_LIST.parameterizedBy(predicateType))
                    .initializer("mutableListOf()")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addFunctions(generateAttributePredicates(parsedObject))
            .addFunction(generateBuildFunction())
            .addFunction(generateAndFunction())
            .addFunction(generateOrFunction())
            .addTypes(generateNestedBuilders(parsedObject))
            .build()
    }
    
    private fun generateAttributePredicates(parsedObject: ParsedMithraObject): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        
        parsedObject.simpleAttributes.forEach { attr ->
            functions.addAll(generatePredicatesForAttribute(attr))
        }
        
        // Add temporal predicates if applicable
        if (parsedObject.objectType.isTemporal) {
            functions.addAll(generateTemporalPredicates(parsedObject))
        }
        
        return functions
    }
    
    private fun generatePredicatesForAttribute(attr: AttributeType.Simple): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        val attrName = attr.name
        
        when (val type = attr.type) {
            is ReladomoType.Primitive -> {
                val kotlinType = type.toKotlinType(attr.nullable)
                
                // equals
                functions.add(
                    FunSpec.builder("${attrName}Equals")
                        .addParameter("value", kotlinType)
                        .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                        .addStatement("predicates.add(%T.Equals(%S, value))", predicateType, attrName)
                        .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                        .build()
                )
                
                // notEquals
                functions.add(
                    FunSpec.builder("${attrName}NotEquals")
                        .addParameter("value", kotlinType)
                        .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                        .addStatement("predicates.add(%T.NotEquals(%S, value))", predicateType, attrName)
                        .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                        .build()
                )
                
                // in
                functions.add(
                    FunSpec.builder("${attrName}In")
                        .addParameter("values", LIST.parameterizedBy(kotlinType))
                        .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                        .addStatement("predicates.add(%T.In(%S, values))", predicateType, attrName)
                        .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                        .build()
                )
                
                // Type-specific predicates
                when (type.type) {
                    is PrimitiveType.String -> {
                        functions.addAll(generateStringPredicates(attr))
                    }
                    is PrimitiveType.Int, is PrimitiveType.Long, 
                    is PrimitiveType.Double, is PrimitiveType.Float,
                    is PrimitiveType.BigDecimal -> {
                        functions.addAll(generateNumericPredicates(attr))
                    }
                    is PrimitiveType.Timestamp, is PrimitiveType.Date -> {
                        functions.addAll(generateTemporalAttributePredicates(attr))
                    }
                    is PrimitiveType.Boolean -> {
                        functions.add(generateBooleanPredicate(attr))
                    }
                    else -> {}
                }
                
                // isNull / isNotNull for nullable attributes
                if (attr.nullable) {
                    functions.add(
                        FunSpec.builder("${attrName}IsNull")
                            .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                            .addStatement("predicates.add(%T.IsNull(%S))", predicateType, attrName)
                            .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                            .build()
                    )
                    
                    functions.add(
                        FunSpec.builder("${attrName}IsNotNull")
                            .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                            .addStatement("predicates.add(%T.IsNotNull(%S))", predicateType, attrName)
                            .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                            .build()
                    )
                }
            }
            else -> {}
        }
        
        return functions
    }
    
    private fun generateStringPredicates(attr: AttributeType.Simple): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        val attrName = attr.name
        
        // contains
        functions.add(
            FunSpec.builder("${attrName}Contains")
                .addParameter("value", String::class)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.Contains(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // startsWith
        functions.add(
            FunSpec.builder("${attrName}StartsWith")
                .addParameter("value", String::class)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.StartsWith(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // endsWith
        functions.add(
            FunSpec.builder("${attrName}EndsWith")
                .addParameter("value", String::class)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.EndsWith(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        return functions
    }
    
    private fun generateNumericPredicates(attr: AttributeType.Simple): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        val attrName = attr.name
        val kotlinType = attr.type.toKotlinType(attr.nullable)
        
        // greaterThan
        functions.add(
            FunSpec.builder("${attrName}GreaterThan")
                .addParameter("value", kotlinType)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.GreaterThan(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // greaterThanOrEquals
        functions.add(
            FunSpec.builder("${attrName}GreaterThanOrEquals")
                .addParameter("value", kotlinType)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.GreaterThanOrEquals(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // lessThan
        functions.add(
            FunSpec.builder("${attrName}LessThan")
                .addParameter("value", kotlinType)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.LessThan(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // lessThanOrEquals
        functions.add(
            FunSpec.builder("${attrName}LessThanOrEquals")
                .addParameter("value", kotlinType)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.LessThanOrEquals(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // between
        functions.add(
            FunSpec.builder("${attrName}Between")
                .addParameter("from", kotlinType)
                .addParameter("to", kotlinType)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.Between(%S, from, to))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        return functions
    }
    
    private fun generateTemporalAttributePredicates(attr: AttributeType.Simple): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        val attrName = attr.name
        
        // before
        functions.add(
            FunSpec.builder("${attrName}Before")
                .addParameter("value", Instant::class)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.LessThan(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // after
        functions.add(
            FunSpec.builder("${attrName}After")
                .addParameter("value", Instant::class)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.GreaterThan(%S, value))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        // between
        functions.add(
            FunSpec.builder("${attrName}Between")
                .addParameter("from", Instant::class)
                .addParameter("to", Instant::class)
                .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
                .addStatement("predicates.add(%T.Between(%S, from, to))", predicateType, attrName)
                .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
                .build()
        )
        
        return functions
    }
    
    private fun generateBooleanPredicate(attr: AttributeType.Simple): FunSpec {
        return FunSpec.builder("${attr.name}Is")
            .addParameter("value", Boolean::class)
            .returns(ClassName("", "${attr.name.capitalize()}QueryBuilder"))
            .addStatement("predicates.add(%T.Equals(%S, value))", predicateType, attr.name)
            .addStatement("return ${attr.name.capitalize()}QueryBuilder(this)")
            .build()
    }
    
    private fun generateTemporalPredicates(parsedObject: ParsedMithraObject): List<FunSpec> {
        val functions = mutableListOf<FunSpec>()
        
        // asOf predicate
        val asOfBuilder = FunSpec.builder("asOf")
            .addParameter("businessDate", Instant::class)
        
        if (parsedObject.objectType == ObjectType.BITEMPORAL) {
            asOfBuilder.addParameter("processingDate", Instant::class)
            asOfBuilder.addStatement("predicates.add(%T.AsOf(businessDate, processingDate))", predicateType)
        } else {
            asOfBuilder.addStatement("predicates.add(%T.AsOf(businessDate))", predicateType)
        }
        
        asOfBuilder.returns(ClassName("", "TemporalQueryBuilder"))
        asOfBuilder.addStatement("return TemporalQueryBuilder(this)")
        
        functions.add(asOfBuilder.build())
        
        return functions
    }
    
    private fun generateBuildFunction(): FunSpec {
        return FunSpec.builder("build")
            .returns(queryType)
            .addStatement("return %T(predicates.toList())", queryType)
            .build()
    }
    
    private fun generateAndFunction(): FunSpec {
        return FunSpec.builder("and")
            .addParameter("other", LambdaTypeName.get(
                receiver = ClassName("", "QueryBuilder"),
                returnType = Unit::class.asTypeName()
            ))
            .returns(ClassName("", "QueryBuilder"))
            .addCode(buildCodeBlock {
                addStatement("val subBuilder = QueryBuilder()")
                addStatement("other(subBuilder)")
                addStatement("predicates.add(%T.And(subBuilder.predicates))", predicateType)
                addStatement("return this")
            })
            .build()
    }
    
    private fun generateOrFunction(): FunSpec {
        return FunSpec.builder("or")
            .addParameter("other", LambdaTypeName.get(
                receiver = ClassName("", "QueryBuilder"),
                returnType = Unit::class.asTypeName()
            ))
            .returns(ClassName("", "QueryBuilder"))
            .addCode(buildCodeBlock {
                addStatement("val subBuilder = QueryBuilder()")
                addStatement("other(subBuilder)")
                addStatement("predicates.add(%T.Or(subBuilder.predicates))", predicateType)
                addStatement("return this")
            })
            .build()
    }
    
    private fun generateNestedBuilders(parsedObject: ParsedMithraObject): List<TypeSpec> {
        val builders = mutableListOf<TypeSpec>()
        
        // Generate attribute-specific builders for fluent API
        parsedObject.simpleAttributes.forEach { attr ->
            builders.add(generateAttributeBuilder(attr))
        }
        
        // Generate temporal query builder if applicable
        if (parsedObject.objectType.isTemporal) {
            builders.add(generateTemporalQueryBuilder())
        }
        
        return builders
    }
    
    private fun generateAttributeBuilder(attr: AttributeType.Simple): TypeSpec {
        return TypeSpec.classBuilder("${attr.name.capitalize()}QueryBuilder")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("parent", ClassName("", "QueryBuilder"))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("parent", ClassName("", "QueryBuilder"))
                    .initializer("parent")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addFunction(
                FunSpec.builder("and")
                    .returns(ClassName("", "QueryBuilder"))
                    .addStatement("return parent")
                    .build()
            )
            .build()
    }
    
    private fun generateTemporalQueryBuilder(): TypeSpec {
        return TypeSpec.classBuilder("TemporalQueryBuilder")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("parent", ClassName("", "QueryBuilder"))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("parent", ClassName("", "QueryBuilder"))
                    .initializer("parent")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addFunction(
                FunSpec.builder("and")
                    .returns(ClassName("", "QueryBuilder"))
                    .addStatement("return parent")
                    .build()
            )
            .build()
    }
    
    companion object {
        private val predicateType = ClassName("io.github.reladomokotlin.core.query", "Predicate")
        private val queryType = ClassName("io.github.reladomokotlin.core.query", "Query")
    }
}