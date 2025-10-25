package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.dto.DatabaseRowDto
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

    @GetMapping("/categories")
    fun getCategoriesTable(): List<DatabaseRowDto> {
        val sql = "SELECT * FROM CATEGORIES ORDER BY ID"
        return jdbcTemplate.query(sql) { rs, _ ->
            DatabaseRowDto(
                tableName = "CATEGORIES",
                columns = mapOf(
                    "ID" to rs.getLong("ID"),
                    "NAME" to rs.getString("NAME"),
                    "DESCRIPTION" to rs.getString("DESCRIPTION")
                )
            )
        }
    }

    @GetMapping("/products")
    fun getProductsTable(): List<DatabaseRowDto> {
        val sql = """
            SELECT * FROM PRODUCTS
            ORDER BY ID, VALID_FROM
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            DatabaseRowDto(
                tableName = "PRODUCTS",
                columns = mapOf(
                    "ID" to rs.getLong("ID"),
                    "CATEGORY_ID" to rs.getLong("CATEGORY_ID"),
                    "NAME" to rs.getString("NAME"),
                    "DESCRIPTION" to rs.getString("DESCRIPTION"),
                    "VALID_FROM" to rs.getTimestamp("VALID_FROM").toString(),
                    "VALID_TO" to rs.getTimestamp("VALID_TO").toString()
                )
            )
        }
    }

    @GetMapping("/product-prices")
    fun getProductPricesTable(): List<DatabaseRowDto> {
        val sql = """
            SELECT * FROM PRODUCT_PRICES
            ORDER BY PRODUCT_ID, BUSINESS_FROM, PROCESSING_FROM
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            DatabaseRowDto(
                tableName = "PRODUCT_PRICES",
                columns = mapOf(
                    "ID" to rs.getLong("ID"),
                    "PRODUCT_ID" to rs.getLong("PRODUCT_ID"),
                    "PRICE" to rs.getBigDecimal("PRICE").toString(),
                    "BUSINESS_FROM" to rs.getTimestamp("BUSINESS_FROM").toString(),
                    "BUSINESS_THRU" to rs.getTimestamp("BUSINESS_THRU").toString(),
                    "PROCESSING_FROM" to rs.getTimestamp("PROCESSING_FROM").toString(),
                    "PROCESSING_THRU" to rs.getTimestamp("PROCESSING_THRU").toString()
                )
            )
        }
    }

    @GetMapping("/all")
    fun getAllTables(): Map<String, List<DatabaseRowDto>> {
        return mapOf(
            "categories" to getCategoriesTable(),
            "products" to getProductsTable(),
            "productPrices" to getProductPricesTable()
        )
    }
}
