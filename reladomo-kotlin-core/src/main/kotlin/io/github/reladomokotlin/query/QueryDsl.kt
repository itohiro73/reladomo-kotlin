package io.github.reladomokotlin.query

import com.gs.fw.common.mithra.attribute.*
import com.gs.fw.common.mithra.finder.Operation
import org.eclipse.collections.impl.set.mutable.primitive.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * DSL marker annotation to restrict scope of DSL functions
 */
@DslMarker
annotation class QueryDslMarker

/**
 * Base interface for query builders
 */
@QueryDslMarker
interface QueryBuilder {
    fun build(): Operation
}

/**
 * Main query context for building operations
 */
@QueryDslMarker
class QueryContext : QueryBuilder {
    private val operations = mutableListOf<Operation>()
    private val extensions = mutableMapOf<String, () -> Attribute<*, *>>()
    
    fun addOperation(operation: Operation) {
        operations.add(operation)
    }
    
    fun addExtension(name: String, attributeProvider: () -> Attribute<*, *>) {
        extensions[name] = attributeProvider
    }
    
    override fun build(): Operation {
        return when (operations.size) {
            0 -> throw IllegalStateException("No query conditions specified")
            1 -> operations.first()
            else -> operations.reduce { acc, operation -> acc.and(operation) }
        }
    }
}

/**
 * Property delegate for attributes in the DSL
 */
class AttributeProperty<T, A : Attribute<*, T>>(
    private val context: QueryContext,
    private val attribute: A
) {
    infix fun eq(value: T): AttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).eq(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).eq(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).eq(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).eq(value as Float)
            is BooleanAttribute<*> -> (attribute as BooleanAttribute<*>).eq(value as Boolean)
            is StringAttribute<*> -> (attribute as StringAttribute<*>).eq(value as String)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).eq(value as BigDecimal)
            is TimestampAttribute<*> -> (attribute as TimestampAttribute<*>).eq(value as java.sql.Timestamp)
            is DateAttribute<*> -> (attribute as DateAttribute<*>).eq(value as java.util.Date)
            is TimeAttribute<*> -> (attribute as TimeAttribute<*>).eq(value as com.gs.fw.common.mithra.util.Time)
            else -> throw UnsupportedOperationException("Unsupported attribute type: ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun notEq(value: T): AttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).notEq(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).notEq(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).notEq(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).notEq(value as Float)
            is BooleanAttribute<*> -> (attribute as BooleanAttribute<*>).notEq(value as Boolean)
            is StringAttribute<*> -> (attribute as StringAttribute<*>).notEq(value as String)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).notEq(value as BigDecimal)
            is TimestampAttribute<*> -> (attribute as TimestampAttribute<*>).notEq(value as java.sql.Timestamp)
            is DateAttribute<*> -> (attribute as DateAttribute<*>).notEq(value as java.util.Date)
            is TimeAttribute<*> -> (attribute as TimeAttribute<*>).notEq(value as com.gs.fw.common.mithra.util.Time)
            else -> throw UnsupportedOperationException("Unsupported attribute type: ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun <R> `in`(values: Collection<R>): AttributeProperty<T, A> where R : T {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> {
                val intSet = IntHashSet()
                values.forEach { intSet.add(it as Int) }
                (attribute as IntegerAttribute<*>).`in`(intSet)
            }
            is LongAttribute<*> -> {
                val longSet = LongHashSet()
                values.forEach { longSet.add(it as Long) }
                (attribute as LongAttribute<*>).`in`(longSet)
            }
            is DoubleAttribute<*> -> {
                val doubleSet = DoubleHashSet()
                values.forEach { doubleSet.add(it as Double) }
                (attribute as DoubleAttribute<*>).`in`(doubleSet)
            }
            is FloatAttribute<*> -> {
                val floatSet = FloatHashSet()
                values.forEach { floatSet.add(it as Float) }
                (attribute as FloatAttribute<*>).`in`(floatSet)
            }
            is StringAttribute<*> -> (attribute as StringAttribute<*>).`in`(values.map { it as String }.toSet())
            else -> throw UnsupportedOperationException("IN operation not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    @Suppress("UNCHECKED_CAST")
    infix fun notIn(values: Collection<T>): AttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> {
                val intSet = IntHashSet()
                values.forEach { intSet.add(it as Int) }
                (attribute as IntegerAttribute<*>).notIn(intSet)
            }
            is LongAttribute<*> -> {
                val longSet = LongHashSet()
                values.forEach { longSet.add(it as Long) }
                (attribute as LongAttribute<*>).notIn(longSet)
            }
            is DoubleAttribute<*> -> {
                val doubleSet = DoubleHashSet()
                values.forEach { doubleSet.add(it as Double) }
                (attribute as DoubleAttribute<*>).notIn(doubleSet)
            }
            is FloatAttribute<*> -> {
                val floatSet = FloatHashSet()
                values.forEach { floatSet.add(it as Float) }
                (attribute as FloatAttribute<*>).notIn(floatSet)
            }
            is StringAttribute<*> -> (attribute as StringAttribute<*>).notIn(values.map { it as String }.toSet())
            else -> throw UnsupportedOperationException("NOT IN operation not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    fun isNull(): AttributeProperty<T, A> {
        context.addOperation(attribute.isNull())
        return this
    }
    
    fun isNotNull(): AttributeProperty<T, A> {
        context.addOperation(attribute.isNotNull())
        return this
    }
}

/**
 * Numeric attribute property with additional operators
 */
class NumericAttributeProperty<T : Number, A : NumericAttribute<*, T>>(
    private val context: QueryContext,
    private val attribute: A
) {
    
    infix fun eq(value: T): NumericAttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).eq(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).eq(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).eq(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).eq(value as Float)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).eq(value as BigDecimal)
            else -> throw UnsupportedOperationException("Unsupported attribute type: ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun notEq(value: T): NumericAttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).notEq(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).notEq(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).notEq(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).notEq(value as Float)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).notEq(value as BigDecimal)
            else -> throw UnsupportedOperationException("Unsupported attribute type: ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun <R> `in`(values: Collection<R>): NumericAttributeProperty<T, A> where R : T {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> {
                val intSet = IntHashSet()
                values.forEach { intSet.add(it as Int) }
                (attribute as IntegerAttribute<*>).`in`(intSet)
            }
            is LongAttribute<*> -> {
                val longSet = LongHashSet()
                values.forEach { longSet.add(it as Long) }
                (attribute as LongAttribute<*>).`in`(longSet)
            }
            is DoubleAttribute<*> -> {
                val doubleSet = DoubleHashSet()
                values.forEach { doubleSet.add(it as Double) }
                (attribute as DoubleAttribute<*>).`in`(doubleSet)
            }
            is FloatAttribute<*> -> {
                val floatSet = FloatHashSet()
                values.forEach { floatSet.add(it as Float) }
                (attribute as FloatAttribute<*>).`in`(floatSet)
            }
            else -> throw UnsupportedOperationException("IN operation not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    @Suppress("UNCHECKED_CAST")
    infix fun notIn(values: Collection<T>): NumericAttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> {
                val intSet = IntHashSet()
                values.forEach { intSet.add(it as Int) }
                (attribute as IntegerAttribute<*>).notIn(intSet)
            }
            is LongAttribute<*> -> {
                val longSet = LongHashSet()
                values.forEach { longSet.add(it as Long) }
                (attribute as LongAttribute<*>).notIn(longSet)
            }
            is DoubleAttribute<*> -> {
                val doubleSet = DoubleHashSet()
                values.forEach { doubleSet.add(it as Double) }
                (attribute as DoubleAttribute<*>).notIn(doubleSet)
            }
            is FloatAttribute<*> -> {
                val floatSet = FloatHashSet()
                values.forEach { floatSet.add(it as Float) }
                (attribute as FloatAttribute<*>).notIn(floatSet)
            }
            else -> throw UnsupportedOperationException("NOT IN operation not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    fun isNull(): NumericAttributeProperty<T, A> {
        context.addOperation((attribute as Attribute<*, *>).isNull())
        return this
    }
    
    fun isNotNull(): NumericAttributeProperty<T, A> {
        context.addOperation((attribute as Attribute<*, *>).isNotNull())
        return this
    }
    
    infix fun greaterThan(value: T): NumericAttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).greaterThan(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).greaterThan(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).greaterThan(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).greaterThan(value as Float)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).greaterThan(value as BigDecimal)
            else -> throw UnsupportedOperationException("Greater than not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun greaterThanEquals(value: T): NumericAttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).greaterThanEquals(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).greaterThanEquals(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).greaterThanEquals(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).greaterThanEquals(value as Float)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).greaterThanEquals(value as BigDecimal)
            else -> throw UnsupportedOperationException("Greater than equals not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun lessThan(value: T): NumericAttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).lessThan(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).lessThan(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).lessThan(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).lessThan(value as Float)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).lessThan(value as BigDecimal)
            else -> throw UnsupportedOperationException("Less than not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun lessThanEquals(value: T): NumericAttributeProperty<T, A> {
        val operation = when (attribute) {
            is IntegerAttribute<*> -> (attribute as IntegerAttribute<*>).lessThanEquals(value as Int)
            is LongAttribute<*> -> (attribute as LongAttribute<*>).lessThanEquals(value as Long)
            is DoubleAttribute<*> -> (attribute as DoubleAttribute<*>).lessThanEquals(value as Double)
            is FloatAttribute<*> -> (attribute as FloatAttribute<*>).lessThanEquals(value as Float)
            is BigDecimalAttribute<*> -> (attribute as BigDecimalAttribute<*>).lessThanEquals(value as BigDecimal)
            else -> throw UnsupportedOperationException("Less than equals not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun between(range: Pair<T, T>): NumericAttributeProperty<T, A> {
        val (start, end) = range
        val operation = when (attribute) {
            is IntegerAttribute<*> -> {
                val attr = attribute as IntegerAttribute<*>
                attr.greaterThanEquals(start as Int).and(attr.lessThanEquals(end as Int))
            }
            is LongAttribute<*> -> {
                val attr = attribute as LongAttribute<*>
                attr.greaterThanEquals(start as Long).and(attr.lessThanEquals(end as Long))
            }
            is DoubleAttribute<*> -> {
                val attr = attribute as DoubleAttribute<*>
                attr.greaterThanEquals(start as Double).and(attr.lessThanEquals(end as Double))
            }
            is FloatAttribute<*> -> {
                val attr = attribute as FloatAttribute<*>
                attr.greaterThanEquals(start as Float).and(attr.lessThanEquals(end as Float))
            }
            is BigDecimalAttribute<*> -> {
                val attr = attribute as BigDecimalAttribute<*>
                attr.greaterThanEquals(start as BigDecimal).and(attr.lessThanEquals(end as BigDecimal))
            }
            else -> throw UnsupportedOperationException("Between not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
}

/**
 * String attribute property with additional operators
 */
class StringAttributeProperty(
    private val context: QueryContext,
    private val attribute: StringAttribute<*>
) {
    
    infix fun eq(value: String): StringAttributeProperty {
        context.addOperation(attribute.eq(value))
        return this
    }
    
    infix fun notEq(value: String): StringAttributeProperty {
        context.addOperation(attribute.notEq(value))
        return this
    }
    
    infix fun `in`(values: Collection<String>): StringAttributeProperty {
        context.addOperation(attribute.`in`(values.toSet()))
        return this
    }
    
    infix fun notIn(values: Collection<String>): StringAttributeProperty {
        context.addOperation(attribute.notIn(values.toSet()))
        return this
    }
    
    fun isNull(): StringAttributeProperty {
        context.addOperation(attribute.isNull())
        return this
    }
    
    fun isNotNull(): StringAttributeProperty {
        context.addOperation(attribute.isNotNull())
        return this
    }
    
    infix fun startsWith(value: String): StringAttributeProperty {
        context.addOperation(attribute.startsWith(value))
        return this
    }
    
    infix fun endsWith(value: String): StringAttributeProperty {
        context.addOperation(attribute.endsWith(value))
        return this
    }
    
    infix fun contains(value: String): StringAttributeProperty {
        context.addOperation(attribute.contains(value))
        return this
    }
    
    infix fun wildcard(pattern: String): StringAttributeProperty {
        context.addOperation(attribute.wildCardEq(pattern))
        return this
    }
}

/**
 * Temporal attribute property for date/time operations
 */
class TemporalAttributeProperty<T, A : Attribute<*, T>>(
    private val context: QueryContext,
    private val attribute: A
) {
    
    infix fun eq(value: T): TemporalAttributeProperty<T, A> {
        val operation = when (attribute) {
            is TimestampAttribute<*> -> (attribute as TimestampAttribute<*>).eq(value as java.sql.Timestamp)
            is DateAttribute<*> -> (attribute as DateAttribute<*>).eq(value as java.util.Date)
            is TimeAttribute<*> -> (attribute as TimeAttribute<*>).eq(value as com.gs.fw.common.mithra.util.Time)
            else -> throw UnsupportedOperationException("Unsupported attribute type: ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun notEq(value: T): TemporalAttributeProperty<T, A> {
        val operation = when (attribute) {
            is TimestampAttribute<*> -> (attribute as TimestampAttribute<*>).notEq(value as java.sql.Timestamp)
            is DateAttribute<*> -> (attribute as DateAttribute<*>).notEq(value as java.util.Date)
            is TimeAttribute<*> -> (attribute as TimeAttribute<*>).notEq(value as com.gs.fw.common.mithra.util.Time)
            else -> throw UnsupportedOperationException("Unsupported attribute type: ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    fun isNull(): TemporalAttributeProperty<T, A> {
        context.addOperation(attribute.isNull())
        return this
    }
    
    fun isNotNull(): TemporalAttributeProperty<T, A> {
        context.addOperation(attribute.isNotNull())
        return this
    }
    
    infix fun after(value: T): TemporalAttributeProperty<T, A> {
        val operation = when (attribute) {
            is TimestampAttribute<*> -> (attribute as TimestampAttribute<*>).greaterThan(value as java.sql.Timestamp)
            is DateAttribute<*> -> (attribute as DateAttribute<*>).greaterThan(value as java.util.Date)
            is TimeAttribute<*> -> (attribute as TimeAttribute<*>).greaterThan(value as com.gs.fw.common.mithra.util.Time)
            else -> throw UnsupportedOperationException("After not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun before(value: T): TemporalAttributeProperty<T, A> {
        val operation = when (attribute) {
            is TimestampAttribute<*> -> (attribute as TimestampAttribute<*>).lessThan(value as java.sql.Timestamp)
            is DateAttribute<*> -> (attribute as DateAttribute<*>).lessThan(value as java.util.Date)
            is TimeAttribute<*> -> (attribute as TimeAttribute<*>).lessThan(value as com.gs.fw.common.mithra.util.Time)
            else -> throw UnsupportedOperationException("Before not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
    
    infix fun between(range: Pair<T, T>): TemporalAttributeProperty<T, A> {
        val (start, end) = range
        val operation = when (attribute) {
            is TimestampAttribute<*> -> {
                val attr = attribute as TimestampAttribute<*>
                attr.greaterThanEquals(start as java.sql.Timestamp)
                    .and(attr.lessThanEquals(end as java.sql.Timestamp))
            }
            is DateAttribute<*> -> {
                val attr = attribute as DateAttribute<*>
                attr.greaterThanEquals(start as java.util.Date)
                    .and(attr.lessThanEquals(end as java.util.Date))
            }
            is TimeAttribute<*> -> {
                val attr = attribute as TimeAttribute<*>
                attr.greaterThanEquals(start as com.gs.fw.common.mithra.util.Time)
                    .and(attr.lessThanEquals(end as com.gs.fw.common.mithra.util.Time))
            }
            else -> throw UnsupportedOperationException("Between not supported for ${attribute::class}")
        }
        context.addOperation(operation)
        return this
    }
}

/**
 * AsOf attribute property for bitemporal queries
 */
class AsOfAttributeProperty(
    private val context: QueryContext,
    private val attribute: AsOfAttribute<*>
) {
    fun equalsEdgePoint(): AsOfAttributeProperty {
        context.addOperation(attribute.equalsEdgePoint())
        return this
    }
    
    infix fun eq(timestamp: java.sql.Timestamp): AsOfAttributeProperty {
        context.addOperation(attribute.eq(timestamp))
        return this
    }
    
    infix fun eq(instant: Instant): AsOfAttributeProperty {
        context.addOperation(attribute.eq(java.sql.Timestamp.from(instant)))
        return this
    }
}

/**
 * Entry point for creating queries
 */
inline fun query(init: QueryContext.() -> Unit): Operation {
    val context = QueryContext()
    context.init()
    return context.build()
}

/**
 * Convenience function for creating attribute properties
 */
fun <T> QueryContext.attribute(attr: Attribute<*, T>): AttributeProperty<T, Attribute<*, T>> {
    return AttributeProperty(this, attr)
}

fun QueryContext.intAttribute(attr: IntegerAttribute<*>): NumericAttributeProperty<Int, IntegerAttribute<*>> {
    return NumericAttributeProperty(this, attr)
}

fun QueryContext.longAttribute(attr: LongAttribute<*>): NumericAttributeProperty<Long, LongAttribute<*>> {
    return NumericAttributeProperty(this, attr)
}

fun QueryContext.doubleAttribute(attr: DoubleAttribute<*>): NumericAttributeProperty<Double, DoubleAttribute<*>> {
    return NumericAttributeProperty(this, attr)
}

fun QueryContext.stringAttribute(attr: StringAttribute<*>): StringAttributeProperty {
    return StringAttributeProperty(this, attr)
}

fun QueryContext.timestampAttribute(attr: TimestampAttribute<*>): TemporalAttributeProperty<java.sql.Timestamp, TimestampAttribute<*>> {
    return TemporalAttributeProperty(this, attr)
}

fun QueryContext.floatAttribute(attr: FloatAttribute<*>): NumericAttributeProperty<Float, FloatAttribute<*>> {
    return NumericAttributeProperty(this, attr)
}

fun QueryContext.dateAttribute(attr: DateAttribute<*>): TemporalAttributeProperty<java.util.Date, DateAttribute<*>> {
    return TemporalAttributeProperty(this, attr)
}

fun QueryContext.bigDecimalAttribute(attr: BigDecimalAttribute<*>): NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>> {
    return NumericAttributeProperty(this, attr)
}

fun QueryContext.asOfAttribute(attr: AsOfAttribute<*>): AsOfAttributeProperty {
    return AsOfAttributeProperty(this, attr)
}