package io.github.kotlinreladomo.core.query

import java.time.Instant

/**
 * Type-safe query representation
 */
data class Query(
    val predicates: List<Predicate>
) {
    fun isEmpty(): Boolean = predicates.isEmpty()
    
    fun toReladomoOperation(): Any? {
        // This will be implemented to convert to Reladomo's Operation
        return null
    }
}

/**
 * Sealed class representing different types of predicates
 */
sealed class Predicate {
    abstract val attributeName: String
    
    data class Equals(
        override val attributeName: String,
        val value: Any?
    ) : Predicate()
    
    data class NotEquals(
        override val attributeName: String,
        val value: Any?
    ) : Predicate()
    
    data class In(
        override val attributeName: String,
        val values: List<Any?>
    ) : Predicate()
    
    data class GreaterThan(
        override val attributeName: String,
        val value: Any
    ) : Predicate()
    
    data class GreaterThanOrEquals(
        override val attributeName: String,
        val value: Any
    ) : Predicate()
    
    data class LessThan(
        override val attributeName: String,
        val value: Any
    ) : Predicate()
    
    data class LessThanOrEquals(
        override val attributeName: String,
        val value: Any
    ) : Predicate()
    
    data class Between(
        override val attributeName: String,
        val from: Any,
        val to: Any
    ) : Predicate()
    
    data class Contains(
        override val attributeName: String,
        val value: String
    ) : Predicate()
    
    data class StartsWith(
        override val attributeName: String,
        val value: String
    ) : Predicate()
    
    data class EndsWith(
        override val attributeName: String,
        val value: String
    ) : Predicate()
    
    data class IsNull(
        override val attributeName: String
    ) : Predicate()
    
    data class IsNotNull(
        override val attributeName: String
    ) : Predicate()
    
    data class And(
        val predicates: List<Predicate>
    ) : Predicate() {
        override val attributeName: String = ""
    }
    
    data class Or(
        val predicates: List<Predicate>
    ) : Predicate() {
        override val attributeName: String = ""
    }
    
    data class AsOf(
        val businessDate: Instant,
        val processingDate: Instant? = null
    ) : Predicate() {
        override val attributeName: String = ""
    }
}

/**
 * Extension functions for predicate combinations
 */
infix fun Predicate.and(other: Predicate): Predicate = 
    Predicate.And(listOf(this, other))

infix fun Predicate.or(other: Predicate): Predicate = 
    Predicate.Or(listOf(this, other))

/**
 * DSL marker for query building
 */
@DslMarker
annotation class QueryDsl