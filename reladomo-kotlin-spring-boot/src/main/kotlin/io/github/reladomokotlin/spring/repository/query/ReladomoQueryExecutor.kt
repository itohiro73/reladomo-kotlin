package io.github.reladomokotlin.spring.repository.query

import com.gs.fw.common.mithra.finder.Operation
import io.github.reladomokotlin.core.BaseRepository
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Executes parsed queries against Reladomo repositories.
 */
class ReladomoQueryExecutor<T : Any, ID : Any>(
    private val repository: BaseRepository<T, ID>,
    private val entityType: KClass<T>
) {
    
    private val logger = LoggerFactory.getLogger(ReladomoQueryExecutor::class.java)
    
    fun execute(parsedQuery: ParsedQuery, parameters: Array<Any?>): Any? {
        logger.debug("Executing query: ${parsedQuery.queryType} with ${parsedQuery.conditions.size} conditions")
        
        // Validate parameter count
        val expectedParamCount = calculateExpectedParameterCount(parsedQuery)
        if (parameters.size != expectedParamCount) {
            throw IllegalArgumentException("Expected $expectedParamCount parameters but got ${parameters.size}")
        }
        
        return when (parsedQuery.queryType) {
            QueryType.FIND -> executeFindQuery(parsedQuery, parameters)
            QueryType.COUNT -> executeCountQuery(parsedQuery, parameters)
            QueryType.EXISTS -> executeExistsQuery(parsedQuery, parameters)
            QueryType.DELETE -> executeDeleteQuery(parsedQuery, parameters)
        }
    }
    
    private fun executeFindQuery(parsedQuery: ParsedQuery, parameters: Array<Any?>): Any? {
        // For simplified implementation, we'll use findAll() and filter in memory
        // In a real implementation, this would build proper Reladomo operations
        val results = repository.findAll()
        
        // Filter results based on conditions
        val filtered = if (parsedQuery.conditions.isNotEmpty()) {
            filterResults(results, parsedQuery, parameters)
        } else {
            results
        }
        
        // Apply sorting if needed
        val sorted = if (parsedQuery.orderBy.isNotEmpty()) {
            sortResults(filtered, parsedQuery.orderBy)
        } else {
            filtered
        }
        
        // Apply limit
        val limited = if (parsedQuery.limit != null) {
            sorted.take(parsedQuery.limit)
        } else {
            sorted
        }
        
        // Apply distinct if needed
        return if (parsedQuery.distinct) {
            limited.distinct()
        } else {
            limited
        }
    }
    
    private fun executeCountQuery(parsedQuery: ParsedQuery, parameters: Array<Any?>): Long {
        val results = executeFindQuery(parsedQuery, parameters)
        return (results as? List<*>)?.size?.toLong() ?: 0L
    }
    
    private fun executeExistsQuery(parsedQuery: ParsedQuery, parameters: Array<Any?>): Boolean {
        val results = executeFindQuery(parsedQuery, parameters)
        return (results as? List<*>)?.isNotEmpty() ?: false
    }
    
    private fun executeDeleteQuery(parsedQuery: ParsedQuery, parameters: Array<Any?>): Unit {
        val results = executeFindQuery(parsedQuery, parameters) as? List<T> ?: return
        results.forEach { repository.delete(it) }
    }
    
    private fun filterResults(
        results: List<T>,
        parsedQuery: ParsedQuery,
        parameters: Array<Any?>
    ): List<T> {
        if (parsedQuery.conditions.isEmpty()) return results
        
        var paramIndex = 0
        // Skip the AsOf date parameter if present (it's always the last parameter)
        val effectiveParams = if (parsedQuery.asOf && parameters.isNotEmpty()) {
            parameters.sliceArray(0 until parameters.size - 1)
        } else {
            parameters
        }
        
        return results.filter { entity ->
            var result = false
            var currentGroupResult = true
            paramIndex = 0
            
            for ((index, condition) in parsedQuery.conditions.withIndex()) {
                val propertyValue = getPropertyValue(entity, condition.propertyName)
                val paramCount = getParameterCount(condition.operator)
                
                val conditionMatches = when (condition.operator) {
                    Operator.EQUALS -> {
                        if (paramIndex < effectiveParams.size) {
                            propertyValue == effectiveParams[paramIndex]
                        } else false
                    }
                    Operator.NOT_EQUALS -> {
                        if (paramIndex < effectiveParams.size) {
                            propertyValue != effectiveParams[paramIndex]
                        } else false
                    }
                    Operator.LESS_THAN -> {
                        if (paramIndex < effectiveParams.size) {
                            compareValues(propertyValue, effectiveParams[paramIndex]) < 0
                        } else false
                    }
                    Operator.LESS_THAN_EQUAL -> {
                        if (paramIndex < effectiveParams.size) {
                            compareValues(propertyValue, effectiveParams[paramIndex]) <= 0
                        } else false
                    }
                    Operator.GREATER_THAN -> {
                        if (paramIndex < effectiveParams.size) {
                            compareValues(propertyValue, effectiveParams[paramIndex]) > 0
                        } else false
                    }
                    Operator.GREATER_THAN_EQUAL -> {
                        if (paramIndex < effectiveParams.size) {
                            compareValues(propertyValue, effectiveParams[paramIndex]) >= 0
                        } else false
                    }
                    Operator.BETWEEN -> {
                        if (paramIndex + 1 < effectiveParams.size) {
                            val min = effectiveParams[paramIndex]
                            val max = effectiveParams[paramIndex + 1]
                            compareValues(propertyValue, min) >= 0 && compareValues(propertyValue, max) <= 0
                        } else false
                    }
                    Operator.IN -> {
                        if (paramIndex < effectiveParams.size) {
                            (effectiveParams[paramIndex] as? Collection<*>)?.contains(propertyValue) ?: false
                        } else false
                    }
                    Operator.NOT_IN -> {
                        if (paramIndex < effectiveParams.size) {
                            (effectiveParams[paramIndex] as? Collection<*>)?.contains(propertyValue)?.not() ?: true
                        } else true
                    }
                    Operator.LIKE -> {
                        if (paramIndex < effectiveParams.size) {
                            // Convert SQL LIKE pattern to regex pattern
                            val pattern = effectiveParams[paramIndex].toString()
                                .replace("%", ".*")
                                .replace("_", ".")
                            propertyValue?.toString()?.matches(Regex(pattern)) ?: false
                        } else false
                    }
                    Operator.NOT_LIKE -> {
                        if (paramIndex < effectiveParams.size) {
                            val pattern = effectiveParams[paramIndex].toString()
                                .replace("%", ".*")
                                .replace("_", ".")
                            propertyValue?.toString()?.matches(Regex(pattern))?.not() ?: true
                        } else true
                    }
                    Operator.CONTAINING -> {
                        if (paramIndex < effectiveParams.size) {
                            propertyValue?.toString()?.contains(effectiveParams[paramIndex].toString()) ?: false
                        } else false
                    }
                    Operator.NOT_CONTAINING -> {
                        if (paramIndex < effectiveParams.size) {
                            propertyValue?.toString()?.contains(effectiveParams[paramIndex].toString())?.not() ?: true
                        } else true
                    }
                    Operator.STARTING_WITH -> {
                        if (paramIndex < effectiveParams.size) {
                            propertyValue?.toString()?.startsWith(effectiveParams[paramIndex].toString()) ?: false
                        } else false
                    }
                    Operator.ENDING_WITH -> {
                        if (paramIndex < effectiveParams.size) {
                            propertyValue?.toString()?.endsWith(effectiveParams[paramIndex].toString()) ?: false
                        } else false
                    }
                    Operator.IS_NULL -> propertyValue == null
                    Operator.IS_NOT_NULL -> propertyValue != null
                    Operator.TRUE -> propertyValue == true
                    Operator.FALSE -> propertyValue == false
                }
                
                if (index == 0) {
                    currentGroupResult = conditionMatches
                } else {
                    val prevOp = parsedQuery.conditions[index - 1].logicalOperator
                    if (prevOp == LogicalOperator.AND) {
                        currentGroupResult = currentGroupResult && conditionMatches
                    } else { // OR
                        result = result || currentGroupResult
                        currentGroupResult = conditionMatches
                    }
                }
                
                paramIndex += paramCount
            }
            
            result || currentGroupResult
        }
    }
    
    private fun getPropertyValue(entity: T, propertyName: String): Any? {
        // Use reflection to get property value
        return try {
            val property = entity::class.members.find { it.name == propertyName }
            property?.call(entity)
        } catch (e: Exception) {
            logger.error("Failed to get property $propertyName from entity", e)
            null
        }
    }
    
    private fun compareValues(a: Any?, b: Any?): Int {
        return when {
            a == null && b == null -> 0
            a == null -> -1
            b == null -> 1
            a is Comparable<*> && b is Comparable<*> -> {
                @Suppress("UNCHECKED_CAST")
                (a as Comparable<Any>).compareTo(b)
            }
            else -> 0
        }
    }
    
    private fun sortResults(results: List<T>, orderBy: List<OrderClause>): List<T> {
        if (orderBy.isEmpty()) return results
        
        return results.sortedWith(compareBy { entity ->
            orderBy.map { clause ->
                val value = getPropertyValue(entity, clause.propertyName)
                if (clause.direction == OrderDirection.DESC) {
                    // Reverse order for DESC
                    value?.let { -it.hashCode() } ?: Int.MIN_VALUE
                } else {
                    value?.hashCode() ?: Int.MAX_VALUE
                }
            }.sum()
        })
    }
    
    private fun getParameterCount(operator: Operator): Int {
        return when (operator) {
            Operator.BETWEEN -> 2
            Operator.IS_NULL, Operator.IS_NOT_NULL, Operator.TRUE, Operator.FALSE -> 0
            else -> 1
        }
    }
    
    private fun calculateExpectedParameterCount(parsedQuery: ParsedQuery): Int {
        var count = parsedQuery.conditions.sumOf { getParameterCount(it.operator) }
        if (parsedQuery.asOf && parsedQuery.conditions.isNotEmpty()) {
            // AsOf queries need an additional parameter for the business date
            count += 1
        }
        return count
    }
}