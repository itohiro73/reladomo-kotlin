package io.github.kotlinreladomo.spring.repository.query

import org.slf4j.LoggerFactory

/**
 * Parses Spring Data style query method names into structured queries.
 */
class QueryMethodParser {
    
    private val logger = LoggerFactory.getLogger(QueryMethodParser::class.java)
    
    companion object {
        private val QUERY_PREFIXES = setOf("find", "read", "get", "query", "search")
        private val COUNT_PREFIXES = setOf("count")
        private val EXISTS_PREFIXES = setOf("exists")
        private val DELETE_PREFIXES = setOf("delete", "remove")
        
        private val OPERATOR_KEYWORDS = mapOf(
            "GreaterThanEqual" to Operator.GREATER_THAN_EQUAL,
            "LessThanEqual" to Operator.LESS_THAN_EQUAL,
            "GreaterThan" to Operator.GREATER_THAN,
            "LessThan" to Operator.LESS_THAN,
            "NotEquals" to Operator.NOT_EQUALS,
            "Equals" to Operator.EQUALS,
            "Is" to Operator.EQUALS,
            "Not" to Operator.NOT_EQUALS,
            "Between" to Operator.BETWEEN,
            "NotIn" to Operator.NOT_IN,
            "In" to Operator.IN,
            "NotLike" to Operator.NOT_LIKE,
            "Like" to Operator.LIKE,
            "NotContaining" to Operator.NOT_CONTAINING,
            "Containing" to Operator.CONTAINING,
            "Contains" to Operator.CONTAINING,
            "StartingWith" to Operator.STARTING_WITH,
            "StartsWith" to Operator.STARTING_WITH,
            "EndingWith" to Operator.ENDING_WITH,
            "EndsWith" to Operator.ENDING_WITH,
            "IsNotNull" to Operator.IS_NOT_NULL,
            "NotNull" to Operator.IS_NOT_NULL,
            "IsNull" to Operator.IS_NULL,
            "True" to Operator.TRUE,
            "False" to Operator.FALSE
        )
    }
    
    fun parse(methodName: String): ParsedQuery {
        logger.debug("Parsing method name: $methodName")
        
        // Determine query type
        val queryType = determineQueryType(methodName)
        
        // Extract the query part after "By"
        val byIndex = methodName.indexOf("By")
        if (byIndex == -1) {
            throw IllegalArgumentException("Query method must contain 'By': $methodName")
        }
        
        val beforeBy = methodName.substring(0, byIndex)
        val afterBy = methodName.substring(byIndex + 2)
        
        // Check for modifiers
        val distinct = beforeBy.contains("Distinct")
        val asOf = afterBy.endsWith("AsOf")
        
        // Extract limit
        val limit = extractLimit(beforeBy)
        
        // Parse conditions and order by
        val orderByIndex = afterBy.indexOf("OrderBy")
        val conditionsString = if (orderByIndex != -1) {
            afterBy.substring(0, orderByIndex)
        } else if (asOf) {
            afterBy.substring(0, afterBy.length - 4) // Remove "AsOf"
        } else {
            afterBy
        }
        
        val conditions = if (conditionsString.isNotEmpty()) {
            parseConditions(conditionsString)
        } else if (!asOf) {
            // For non-AsOf queries, empty conditions are not allowed
            throw IllegalArgumentException("Query method must have conditions after 'By': $methodName")
        } else {
            emptyList()
        }
        
        val orderBy = if (orderByIndex != -1) {
            val orderByString = if (asOf) {
                afterBy.substring(orderByIndex + 7, afterBy.length - 4)
            } else {
                afterBy.substring(orderByIndex + 7)
            }
            parseOrderBy(orderByString)
        } else {
            emptyList()
        }
        
        return ParsedQuery(
            queryType = queryType,
            conditions = conditions,
            orderBy = orderBy,
            limit = limit,
            distinct = distinct,
            asOf = asOf
        )
    }
    
    private fun determineQueryType(methodName: String): QueryType {
        val lowerMethodName = methodName.lowercase()
        return when {
            QUERY_PREFIXES.any { lowerMethodName.startsWith(it) } -> QueryType.FIND
            COUNT_PREFIXES.any { lowerMethodName.startsWith(it) } -> QueryType.COUNT
            EXISTS_PREFIXES.any { lowerMethodName.startsWith(it) } -> QueryType.EXISTS
            DELETE_PREFIXES.any { lowerMethodName.startsWith(it) } -> QueryType.DELETE
            else -> throw IllegalArgumentException("Unknown query type in method: $methodName")
        }
    }
    
    private fun extractLimit(beforeBy: String): Int? {
        // Check for First or Top
        val firstMatch = Regex("First(\\d*)").find(beforeBy)
        if (firstMatch != null) {
            val number = firstMatch.groupValues[1]
            return if (number.isEmpty()) 1 else number.toInt()
        }
        
        val topMatch = Regex("Top(\\d+)").find(beforeBy)
        if (topMatch != null) {
            return topMatch.groupValues[1].toInt()
        }
        
        return null
    }
    
    private fun parseConditions(conditionsString: String): List<Condition> {
        if (conditionsString.isEmpty()) return emptyList()
        
        val conditions = mutableListOf<Condition>()
        var remaining = conditionsString
        var currentLogicalOp = LogicalOperator.AND
        
        while (remaining.isNotEmpty()) {
            // Split by And/Or
            val andIndex = findOperatorIndex(remaining, "And")
            val orIndex = findOperatorIndex(remaining, "Or")
            
            val splitIndex = when {
                andIndex == -1 && orIndex == -1 -> remaining.length
                andIndex == -1 -> orIndex
                orIndex == -1 -> andIndex
                else -> minOf(andIndex, orIndex)
            }
            
            val conditionString = remaining.substring(0, splitIndex)
            val (propertyName, operator) = parseCondition(conditionString)
            
            conditions.add(Condition(propertyName, operator, currentLogicalOp))
            
            if (splitIndex < remaining.length) {
                currentLogicalOp = if (remaining.substring(splitIndex).startsWith("And")) {
                    remaining = remaining.substring(splitIndex + 3)
                    LogicalOperator.AND
                } else {
                    remaining = remaining.substring(splitIndex + 2)
                    LogicalOperator.OR
                }
            } else {
                break
            }
        }
        
        return conditions
    }
    
    private fun findOperatorIndex(text: String, operator: String): Int {
        var index = 0
        while (index < text.length) {
            val foundIndex = text.indexOf(operator, index)
            if (foundIndex == -1) return -1
            
            // Check if this is a standalone operator (not part of another word)
            if (foundIndex > 0 && foundIndex + operator.length < text.length) {
                val nextChar = text[foundIndex + operator.length]
                if (nextChar.isUpperCase()) {
                    return foundIndex
                }
            }
            index = foundIndex + 1
        }
        return -1
    }
    
    private fun parseCondition(conditionString: String): Pair<String, Operator> {
        // Find operator by checking from longest to shortest to avoid matching substrings
        for ((keyword, operator) in OPERATOR_KEYWORDS.entries.sortedByDescending { it.key.length }) {
            val index = conditionString.indexOf(keyword)
            if (index > 0) {
                val propertyName = conditionString.substring(0, index).decapitalize()
                return propertyName to operator
            }
        }
        
        // Default to equals
        return conditionString.decapitalize() to Operator.EQUALS
    }
    
    private fun parseOrderBy(orderByString: String): List<OrderClause> {
        if (orderByString.isEmpty()) return emptyList()
        
        val clauses = mutableListOf<OrderClause>()
        var remaining = orderByString
        
        while (remaining.isNotEmpty()) {
            // Find the end of the current property+direction clause
            var endIndex = remaining.length
            
            // Check if we have another property after direction indicators
            val ascIndex = remaining.indexOf("Asc")
            val descIndex = remaining.indexOf("Desc")
            
            if (ascIndex > 0) {
                val afterAsc = ascIndex + 3
                if (afterAsc < remaining.length && remaining[afterAsc].isUpperCase()) {
                    endIndex = afterAsc
                }
            }
            
            if (descIndex > 0) {
                val afterDesc = descIndex + 4
                if (afterDesc < remaining.length && remaining[afterDesc].isUpperCase()) {
                    endIndex = afterDesc
                }
            }
            
            // If no direction indicator found, look for next uppercase letter
            if (ascIndex == -1 && descIndex == -1) {
                val nextUpperIndex = remaining.drop(1).indexOfFirst { it.isUpperCase() }
                if (nextUpperIndex != -1) {
                    endIndex = nextUpperIndex + 1
                }
            }
            
            val clauseString = remaining.substring(0, endIndex)
            val direction = when {
                clauseString.endsWith("Desc") -> OrderDirection.DESC
                clauseString.endsWith("Asc") -> OrderDirection.ASC
                else -> OrderDirection.ASC
            }
            
            val propertyName = when (direction) {
                OrderDirection.DESC -> clauseString.substring(0, clauseString.length - 4)
                OrderDirection.ASC -> if (clauseString.endsWith("Asc")) {
                    clauseString.substring(0, clauseString.length - 3)
                } else {
                    clauseString
                }
            }.decapitalize()
            
            clauses.add(OrderClause(propertyName, direction))
            remaining = remaining.substring(endIndex)
        }
        
        return clauses
    }
}

// Extension function to decapitalize
private fun String.decapitalize(): String {
    return if (isNotEmpty() && this[0].isUpperCase()) {
        this[0].lowercase() + substring(1)
    } else {
        this
    }
}