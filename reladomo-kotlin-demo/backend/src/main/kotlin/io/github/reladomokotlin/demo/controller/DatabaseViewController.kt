package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.dto.DatabaseTableDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*

/**
 * This controller provides raw database views for demo purposes
 * Shows how temporal data is actually stored in the database
 */
@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = ["*"])
class DatabaseViewController(
    private val jdbcTemplate: JdbcTemplate
) {

    @GetMapping("/tables")
    fun getTables(): List<DatabaseTableDto> {
        return listOf(
            getCategoriesTable(),
            getProductsTable(),
            getProductPricesTable()
        )
    }

    private fun getCategoriesTable(): DatabaseTableDto {
        val sql = "SELECT * FROM CATEGORIES ORDER BY ID"
        val rows = jdbcTemplate.query(sql) { rs, _ ->
            mapOf<String, Any?>(
                "ID" to rs.getLong("ID"),
                "NAME" to rs.getString("NAME"),
                "DESCRIPTION" to rs.getString("DESCRIPTION")
            )
        }

        return DatabaseTableDto(
            name = "CATEGORIES",
            columns = listOf("ID", "NAME", "DESCRIPTION"),
            rows = rows
        )
    }

    private fun getProductsTable(): DatabaseTableDto {
        val sql = "SELECT * FROM PRODUCTS ORDER BY ID"
        val rows = jdbcTemplate.query(sql) { rs, _ ->
            mapOf<String, Any?>(
                "ID" to rs.getLong("ID"),
                "CATEGORY_ID" to rs.getLong("CATEGORY_ID"),
                "NAME" to rs.getString("NAME"),
                "DESCRIPTION" to rs.getString("DESCRIPTION")
            )
        }

        return DatabaseTableDto(
            name = "PRODUCTS",
            columns = listOf("ID", "CATEGORY_ID", "NAME", "DESCRIPTION"),
            rows = rows
        )
    }

    private fun getProductPricesTable(): DatabaseTableDto {
        val sql = """
            SELECT * FROM PRODUCT_PRICES
            ORDER BY PRODUCT_ID, BUSINESS_FROM, PROCESSING_FROM
        """.trimIndent()

        val rows = jdbcTemplate.query(sql) { rs, _ ->
            mapOf<String, Any?>(
                "ID" to rs.getLong("ID"),
                "PRODUCT_ID" to rs.getLong("PRODUCT_ID"),
                "PRICE" to rs.getBigDecimal("PRICE"),
                "UPDATED_BY" to rs.getString("UPDATED_BY"),
                "BUSINESS_FROM" to rs.getTimestamp("BUSINESS_FROM"),
                "BUSINESS_THRU" to rs.getTimestamp("BUSINESS_THRU"),
                "PROCESSING_FROM" to rs.getTimestamp("PROCESSING_FROM"),
                "PROCESSING_THRU" to rs.getTimestamp("PROCESSING_THRU")
            )
        }

        return DatabaseTableDto(
            name = "PRODUCT_PRICES",
            columns = listOf("ID", "PRODUCT_ID", "PRICE", "UPDATED_BY", "BUSINESS_FROM", "BUSINESS_THRU", "PROCESSING_FROM", "PROCESSING_THRU"),
            rows = rows
        )
    }
}
