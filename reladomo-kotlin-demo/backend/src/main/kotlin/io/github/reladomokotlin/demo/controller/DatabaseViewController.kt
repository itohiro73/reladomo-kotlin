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
        // Database stores UTC, but display in JST for user-friendliness
        // Add 9 hours to convert UTC to JST for display (unitemporal: only PROCESSING_FROM/THRU)
        val sql = """
            SELECT
                ID,
                CATEGORY_ID,
                NAME,
                DESCRIPTION,
                FORMATDATETIME(DATEADD('HOUR', 9, PROCESSING_FROM), 'yyyy-MM-dd HH:mm:ss') as PROCESSING_FROM_JST,
                FORMATDATETIME(DATEADD('HOUR', 9, PROCESSING_THRU), 'yyyy-MM-dd HH:mm:ss') as PROCESSING_THRU_JST
            FROM PRODUCTS
            ORDER BY ID, PROCESSING_FROM
        """.trimIndent()

        val rows = jdbcTemplate.query(sql) { rs, _ ->
            mapOf<String, Any?>(
                "ID" to rs.getLong("ID"),
                "CATEGORY_ID" to rs.getLong("CATEGORY_ID"),
                "NAME" to rs.getString("NAME"),
                "DESCRIPTION" to rs.getString("DESCRIPTION"),
                "PROCESSING_FROM" to rs.getString("PROCESSING_FROM_JST"),
                "PROCESSING_THRU" to rs.getString("PROCESSING_THRU_JST")
            )
        }

        return DatabaseTableDto(
            name = "PRODUCTS",
            columns = listOf("ID", "CATEGORY_ID", "NAME", "DESCRIPTION", "PROCESSING_FROM", "PROCESSING_THRU"),
            rows = rows
        )
    }

    private fun getProductPricesTable(): DatabaseTableDto {
        // Database stores UTC, but display in JST for user-friendliness
        // Add 9 hours to convert UTC to JST for display
        val sql = """
            SELECT
                ID,
                PRODUCT_ID,
                PRICE,
                UPDATED_BY,
                FORMATDATETIME(DATEADD('HOUR', 9, BUSINESS_FROM), 'yyyy-MM-dd HH:mm:ss') as BUSINESS_FROM_JST,
                FORMATDATETIME(DATEADD('HOUR', 9, BUSINESS_THRU), 'yyyy-MM-dd HH:mm:ss') as BUSINESS_THRU_JST,
                FORMATDATETIME(DATEADD('HOUR', 9, PROCESSING_FROM), 'yyyy-MM-dd HH:mm:ss') as PROCESSING_FROM_JST,
                FORMATDATETIME(DATEADD('HOUR', 9, PROCESSING_THRU), 'yyyy-MM-dd HH:mm:ss') as PROCESSING_THRU_JST
            FROM PRODUCT_PRICES
            ORDER BY PRODUCT_ID, BUSINESS_FROM, PROCESSING_FROM
        """.trimIndent()

        val rows = jdbcTemplate.query(sql) { rs, _ ->
            mapOf<String, Any?>(
                "ID" to rs.getLong("ID"),
                "PRODUCT_ID" to rs.getLong("PRODUCT_ID"),
                "PRICE" to rs.getBigDecimal("PRICE"),
                "UPDATED_BY" to rs.getString("UPDATED_BY"),
                "BUSINESS_FROM" to rs.getString("BUSINESS_FROM_JST"),
                "BUSINESS_THRU" to rs.getString("BUSINESS_THRU_JST"),
                "PROCESSING_FROM" to rs.getString("PROCESSING_FROM_JST"),
                "PROCESSING_THRU" to rs.getString("PROCESSING_THRU_JST")
            )
        }

        return DatabaseTableDto(
            name = "PRODUCT_PRICES",
            columns = listOf("ID", "PRODUCT_ID", "PRICE", "UPDATED_BY", "BUSINESS_FROM", "BUSINESS_THRU", "PROCESSING_FROM", "PROCESSING_THRU"),
            rows = rows
        )
    }
}
