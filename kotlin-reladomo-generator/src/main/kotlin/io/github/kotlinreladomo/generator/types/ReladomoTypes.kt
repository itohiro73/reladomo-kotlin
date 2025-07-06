package io.github.kotlinreladomo.generator.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Algebraic Data Type representing all possible Reladomo types
 */
sealed class ReladomoType {
    abstract fun toKotlinType(nullable: Boolean = false): TypeName
    abstract val xmlName: String
    
    data class Primitive(val type: PrimitiveType) : ReladomoType() {
        override fun toKotlinType(nullable: Boolean): TypeName = type.toKotlinType(nullable)
        override val xmlName: String = type.xmlName
    }
    
    data class Object(val className: String, val packageName: String) : ReladomoType() {
        override fun toKotlinType(nullable: Boolean): TypeName {
            val baseType = ClassName(packageName, className)
            return if (nullable) baseType.copy(nullable = true) else baseType
        }
        override val xmlName: String = "$packageName.$className"
    }
    
    data class List(val elementType: ReladomoType) : ReladomoType() {
        override fun toKotlinType(nullable: Boolean): TypeName {
            val listType = kotlin.collections.List::class.asClassName()
                .parameterizedBy(elementType.toKotlinType(false))
            return if (nullable) listType.copy(nullable = true) else listType
        }
        override val xmlName: String = "List<${elementType.xmlName}>"
    }
}

/**
 * Sealed class representing all primitive types supported by Reladomo
 */
sealed class PrimitiveType {
    abstract fun toKotlinType(nullable: Boolean = false): TypeName
    abstract val xmlName: kotlin.String
    abstract val defaultValue: kotlin.String
    
    object Long : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.Long::class.asTypeName().copy(nullable = true) 
            else kotlin.Long::class.asTypeName()
        override val xmlName = "long"
        override val defaultValue = "0L"
    }
    
    object Int : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.Int::class.asTypeName().copy(nullable = true)
            else kotlin.Int::class.asTypeName()
        override val xmlName = "int"
        override val defaultValue = "0"
    }
    
    object Double : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.Double::class.asTypeName().copy(nullable = true)
            else kotlin.Double::class.asTypeName()
        override val xmlName = "double"
        override val defaultValue = "0.0"
    }
    
    object Float : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.Float::class.asTypeName().copy(nullable = true)
            else kotlin.Float::class.asTypeName()
        override val xmlName = "float"
        override val defaultValue = "0.0f"
    }
    
    object BigDecimal : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) java.math.BigDecimal::class.asTypeName().copy(nullable = true)
            else java.math.BigDecimal::class.asTypeName()
        override val xmlName = "BigDecimal"
        override val defaultValue = "BigDecimal.ZERO"
    }
    
    object String : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.String::class.asTypeName().copy(nullable = true)
            else kotlin.String::class.asTypeName()
        override val xmlName = "String"
        override val defaultValue = "\"\""
    }
    
    object Timestamp : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) Instant::class.asTypeName().copy(nullable = true)
            else Instant::class.asTypeName()
        override val xmlName = "Timestamp"
        override val defaultValue = "Instant.now()"
    }
    
    object Date : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) LocalDate::class.asTypeName().copy(nullable = true)
            else LocalDate::class.asTypeName()
        override val xmlName = "Date"
        override val defaultValue = "LocalDate.now()"
    }
    
    object Time : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) LocalTime::class.asTypeName().copy(nullable = true)
            else LocalTime::class.asTypeName()
        override val xmlName = "Time"
        override val defaultValue = "LocalTime.now()"
    }
    
    object Boolean : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.Boolean::class.asTypeName().copy(nullable = true)
            else kotlin.Boolean::class.asTypeName()
        override val xmlName = "boolean"
        override val defaultValue = "false"
    }
    
    object ByteArray : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.ByteArray::class.asTypeName().copy(nullable = true)
            else kotlin.ByteArray::class.asTypeName()
        override val xmlName = "byte[]"
        override val defaultValue = "byteArrayOf()"
    }
    
    object Char : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.Char::class.asTypeName().copy(nullable = true)
            else kotlin.Char::class.asTypeName()
        override val xmlName = "char"
        override val defaultValue = "'\\u0000'"
    }
    
    object Short : PrimitiveType() {
        override fun toKotlinType(nullable: Boolean) = 
            if (nullable) kotlin.Short::class.asTypeName().copy(nullable = true)
            else kotlin.Short::class.asTypeName()
        override val xmlName = "short"
        override val defaultValue = "0"
    }
}

/**
 * Type mapper that safely converts XML type strings to ReladomoType
 */
object TypeMapper {
    fun fromXmlType(xmlType: String): ReladomoType = when (xmlType.lowercase()) {
        "int", "integer" -> ReladomoType.Primitive(PrimitiveType.Int)
        "long" -> ReladomoType.Primitive(PrimitiveType.Long)
        "double" -> ReladomoType.Primitive(PrimitiveType.Double)
        "float" -> ReladomoType.Primitive(PrimitiveType.Float)
        "bigdecimal", "decimal" -> ReladomoType.Primitive(PrimitiveType.BigDecimal)
        "string", "varchar", "char" -> ReladomoType.Primitive(PrimitiveType.String)
        "timestamp", "datetime" -> ReladomoType.Primitive(PrimitiveType.Timestamp)
        "date" -> ReladomoType.Primitive(PrimitiveType.Date)
        "time" -> ReladomoType.Primitive(PrimitiveType.Time)
        "boolean", "bool" -> ReladomoType.Primitive(PrimitiveType.Boolean)
        "byte[]", "blob" -> ReladomoType.Primitive(PrimitiveType.ByteArray)
        "char" -> ReladomoType.Primitive(PrimitiveType.Char)
        "short" -> ReladomoType.Primitive(PrimitiveType.Short)
        else -> {
            // Check if it's a fully qualified class name
            if (xmlType.contains(".")) {
                val lastDot = xmlType.lastIndexOf('.')
                val packageName = xmlType.substring(0, lastDot)
                val className = xmlType.substring(lastDot + 1)
                ReladomoType.Object(className, packageName)
            } else {
                throw IllegalArgumentException("Unsupported XML type: $xmlType")
            }
        }
    }
    
    fun toXmlType(reladomoType: ReladomoType): String = reladomoType.xmlName
}