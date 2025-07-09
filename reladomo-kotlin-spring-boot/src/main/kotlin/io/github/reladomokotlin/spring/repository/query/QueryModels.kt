package io.github.reladomokotlin.spring.repository.query

/**
 * Represents different types of queries.
 */
enum class QueryType {
    FIND,
    COUNT,
    EXISTS,
    DELETE
}

/**
 * Represents logical operators for combining conditions.
 */
enum class LogicalOperator {
    AND,
    OR
}

/**
 * Represents comparison operators.
 */
enum class Operator {
    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    LESS_THAN_EQUAL,
    GREATER_THAN,
    GREATER_THAN_EQUAL,
    BETWEEN,
    IN,
    NOT_IN,
    LIKE,
    NOT_LIKE,
    CONTAINING,
    NOT_CONTAINING,
    STARTING_WITH,
    ENDING_WITH,
    IS_NULL,
    IS_NOT_NULL,
    TRUE,
    FALSE
}

/**
 * Represents sort direction.
 */
enum class OrderDirection {
    ASC,
    DESC
}

/**
 * Represents a query condition.
 */
data class Condition(
    val propertyName: String,
    val operator: Operator,
    val logicalOperator: LogicalOperator
)

/**
 * Represents an order by clause.
 */
data class OrderClause(
    val propertyName: String,
    val direction: OrderDirection
)

/**
 * Represents a parsed query.
 */
data class ParsedQuery(
    val queryType: QueryType,
    val conditions: List<Condition> = emptyList(),
    val orderBy: List<OrderClause> = emptyList(),
    val limit: Int? = null,
    val distinct: Boolean = false,
    val asOf: Boolean = false
)