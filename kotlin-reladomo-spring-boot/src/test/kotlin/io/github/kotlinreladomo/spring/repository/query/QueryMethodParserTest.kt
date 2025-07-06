package io.github.kotlinreladomo.spring.repository.query

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class QueryMethodParserTest {
    
    private val parser = QueryMethodParser()
    
    @Test
    fun `parse simple findBy query`() {
        val result = parser.parse("findByName")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("name", result.conditions[0].propertyName)
        assertEquals(Operator.EQUALS, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with multiple conditions`() {
        val result = parser.parse("findByNameAndAge")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(2, result.conditions.size)
        assertEquals("name", result.conditions[0].propertyName)
        assertEquals(Operator.EQUALS, result.conditions[0].operator)
        assertEquals("age", result.conditions[1].propertyName)
        assertEquals(Operator.EQUALS, result.conditions[1].operator)
    }
    
    @Test
    fun `parse query with OR condition`() {
        val result = parser.parse("findByNameOrAge")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(2, result.conditions.size)
        assertEquals("name", result.conditions[0].propertyName)
        assertEquals("age", result.conditions[1].propertyName)
        assertEquals(LogicalOperator.OR, result.conditions[1].logicalOperator)
    }
    
    @Test
    fun `parse query with greater than operator`() {
        val result = parser.parse("findByAgeGreaterThan")
        
        assertEquals(1, result.conditions.size)
        assertEquals("age", result.conditions[0].propertyName)
        assertEquals(Operator.GREATER_THAN, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with less than equal operator`() {
        val result = parser.parse("findByAgeLessThanEqual")
        
        assertEquals(1, result.conditions.size)
        assertEquals("age", result.conditions[0].propertyName)
        assertEquals(Operator.LESS_THAN_EQUAL, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with between operator`() {
        val result = parser.parse("findByAgeBetween")
        
        assertEquals(1, result.conditions.size)
        assertEquals("age", result.conditions[0].propertyName)
        assertEquals(Operator.BETWEEN, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with like operator`() {
        val result = parser.parse("findByNameLike")
        
        assertEquals(1, result.conditions.size)
        assertEquals("name", result.conditions[0].propertyName)
        assertEquals(Operator.LIKE, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with containing operator`() {
        val result = parser.parse("findByDescriptionContaining")
        
        assertEquals(1, result.conditions.size)
        assertEquals("description", result.conditions[0].propertyName)
        assertEquals(Operator.CONTAINING, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with starting with operator`() {
        val result = parser.parse("findByNameStartingWith")
        
        assertEquals(1, result.conditions.size)
        assertEquals("name", result.conditions[0].propertyName)
        assertEquals(Operator.STARTING_WITH, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with ending with operator`() {
        val result = parser.parse("findByNameEndingWith")
        
        assertEquals(1, result.conditions.size)
        assertEquals("name", result.conditions[0].propertyName)
        assertEquals(Operator.ENDING_WITH, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with IN operator`() {
        val result = parser.parse("findByStatusIn")
        
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
        assertEquals(Operator.IN, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with NOT IN operator`() {
        val result = parser.parse("findByStatusNotIn")
        
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
        assertEquals(Operator.NOT_IN, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with IS NULL operator`() {
        val result = parser.parse("findByDescriptionIsNull")
        
        assertEquals(1, result.conditions.size)
        assertEquals("description", result.conditions[0].propertyName)
        assertEquals(Operator.IS_NULL, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with IS NOT NULL operator`() {
        val result = parser.parse("findByDescriptionIsNotNull")
        
        assertEquals(1, result.conditions.size)
        assertEquals("description", result.conditions[0].propertyName)
        assertEquals(Operator.IS_NOT_NULL, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with TRUE operator`() {
        val result = parser.parse("findByActiveTrue")
        
        assertEquals(1, result.conditions.size)
        assertEquals("active", result.conditions[0].propertyName)
        assertEquals(Operator.TRUE, result.conditions[0].operator)
    }
    
    @Test
    fun `parse query with FALSE operator`() {
        val result = parser.parse("findByActiveFalse")
        
        assertEquals(1, result.conditions.size)
        assertEquals("active", result.conditions[0].propertyName)
        assertEquals(Operator.FALSE, result.conditions[0].operator)
    }
    
    @Test
    fun `parse count query`() {
        val result = parser.parse("countByStatus")
        
        assertEquals(QueryType.COUNT, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse exists query`() {
        val result = parser.parse("existsByEmail")
        
        assertEquals(QueryType.EXISTS, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("email", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse delete query`() {
        val result = parser.parse("deleteByStatus")
        
        assertEquals(QueryType.DELETE, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with limit - First`() {
        val result = parser.parse("findFirst5ByStatus")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(5, result.limit)
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with limit - Top`() {
        val result = parser.parse("findTop10ByName")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(10, result.limit)
        assertEquals(1, result.conditions.size)
        assertEquals("name", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with single First`() {
        val result = parser.parse("findFirstByEmail")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.limit)
        assertEquals(1, result.conditions.size)
        assertEquals("email", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with distinct`() {
        val result = parser.parse("findDistinctByStatus")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertTrue(result.distinct)
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with order by`() {
        val result = parser.parse("findByStatusOrderByName")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
        assertEquals(1, result.orderBy.size)
        assertEquals("name", result.orderBy[0].propertyName)
        assertEquals(OrderDirection.ASC, result.orderBy[0].direction)
    }
    
    @Test
    fun `parse query with order by descending`() {
        val result = parser.parse("findByStatusOrderByNameDesc")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.orderBy.size)
        assertEquals("name", result.orderBy[0].propertyName)
        assertEquals(OrderDirection.DESC, result.orderBy[0].direction)
    }
    
    @Test
    fun `parse query with multiple order by`() {
        val result = parser.parse("findByStatusOrderByNameAscAgeDesc")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(2, result.orderBy.size)
        assertEquals("name", result.orderBy[0].propertyName)
        assertEquals(OrderDirection.ASC, result.orderBy[0].direction)
        assertEquals("age", result.orderBy[1].propertyName)
        assertEquals(OrderDirection.DESC, result.orderBy[1].direction)
    }
    
    @Test
    fun `parse complex query`() {
        val result = parser.parse("findTop5ByStatusAndAmountGreaterThanOrNameLikeOrderByCreatedAtDesc")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(5, result.limit)
        assertEquals(3, result.conditions.size)
        
        assertEquals("status", result.conditions[0].propertyName)
        assertEquals(Operator.EQUALS, result.conditions[0].operator)
        assertEquals(LogicalOperator.AND, result.conditions[1].logicalOperator)
        
        assertEquals("amount", result.conditions[1].propertyName)
        assertEquals(Operator.GREATER_THAN, result.conditions[1].operator)
        assertEquals(LogicalOperator.OR, result.conditions[2].logicalOperator)
        
        assertEquals("name", result.conditions[2].propertyName)
        assertEquals(Operator.LIKE, result.conditions[2].operator)
        
        assertEquals(1, result.orderBy.size)
        assertEquals("createdAt", result.orderBy[0].propertyName)
        assertEquals(OrderDirection.DESC, result.orderBy[0].direction)
    }
    
    @Test
    fun `parse query with camelCase property names`() {
        val result = parser.parse("findByCustomerIdAndOrderDate")
        
        assertEquals(2, result.conditions.size)
        assertEquals("customerId", result.conditions[0].propertyName)
        assertEquals("orderDate", result.conditions[1].propertyName)
    }
    
    @Test
    fun `parse query with read prefix`() {
        val result = parser.parse("readByEmail")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("email", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with get prefix`() {
        val result = parser.parse("getByUsername")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("username", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with query prefix`() {
        val result = parser.parse("queryByStatus")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with search prefix`() {
        val result = parser.parse("searchByKeyword")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("keyword", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse query with remove prefix for delete`() {
        val result = parser.parse("removeByStatus")
        
        assertEquals(QueryType.DELETE, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("status", result.conditions[0].propertyName)
    }
    
    @Test
    fun `parse bitemporal query`() {
        val result = parser.parse("findByCustomerIdAsOf")
        
        assertEquals(QueryType.FIND, result.queryType)
        assertEquals(1, result.conditions.size)
        assertEquals("customerId", result.conditions[0].propertyName)
        assertTrue(result.asOf)
    }
    
    @Test
    fun `invalid query without By throws exception`() {
        assertThrows<IllegalArgumentException> {
            parser.parse("findName")
        }
    }
    
    @Test
    fun `invalid query with empty conditions throws exception`() {
        assertThrows<IllegalArgumentException> {
            parser.parse("findBy")
        }
    }
    
    @Test
    fun `invalid query with unknown prefix throws exception`() {
        assertThrows<IllegalArgumentException> {
            parser.parse("selectByName")
        }
    }
}