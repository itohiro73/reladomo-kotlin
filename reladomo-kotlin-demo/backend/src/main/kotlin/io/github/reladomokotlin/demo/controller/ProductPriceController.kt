package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductPriceKtRepository
import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductKtRepository
import io.github.reladomokotlin.demo.dto.ProductPriceDto
import io.github.reladomokotlin.demo.dto.CreateProductPriceRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/product-prices")
@CrossOrigin(origins = ["*"])
class ProductPriceController(
    private val repository: ProductPriceKtRepository,
    private val productRepository: ProductKtRepository,
    private val jdbcTemplate: JdbcTemplate
) {

    /**
     * Get all product prices - returns all versions from database with correct THRU timestamps
     * This demonstrates bitemporal data storage
     */
    @GetMapping
    fun getAll(): List<ProductPriceDto> {
        val sql = """
            SELECT pp.ID, pp.PRODUCT_ID, pp.PRICE,
                   pp.BUSINESS_FROM, pp.BUSINESS_THRU,
                   pp.PROCESSING_FROM, pp.PROCESSING_THRU,
                   pp.UPDATED_BY,
                   p.NAME as PRODUCT_NAME
            FROM PRODUCT_PRICES pp
            LEFT JOIN PRODUCTS p ON pp.PRODUCT_ID = p.ID
            ORDER BY pp.PRODUCT_ID, pp.BUSINESS_FROM, pp.PROCESSING_FROM
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            ProductPriceDto(
                id = rs.getLong("ID"),
                productId = rs.getLong("PRODUCT_ID"),
                productName = rs.getString("PRODUCT_NAME"),
                price = rs.getBigDecimal("PRICE"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant(),
                businessThru = rs.getTimestamp("BUSINESS_THRU").toInstant(),
                processingFrom = rs.getTimestamp("PROCESSING_FROM").toInstant(),
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant(),
                updatedBy = rs.getString("UPDATED_BY")
            )
        }
    }

    /**
     * Time-travel query: "As of processing date P, what prices were effective on business date B?"
     * This is the killer feature of bitemporal data!
     *
     * Example: GET /api/product-prices/asof?businessDate=2024-12-01T00:00:00Z&processingDate=2024-11-01T00:00:00Z
     * Returns: The price we THOUGHT would be effective on Dec 1st, as known on Nov 1st (before correction)
     */
    @GetMapping("/asof")
    fun getAsOf(
        @RequestParam businessDate: String,
        @RequestParam processingDate: String
    ): List<ProductPriceDto> {
        val sql = """
            SELECT pp.ID, pp.PRODUCT_ID, pp.PRICE,
                   pp.BUSINESS_FROM, pp.BUSINESS_THRU,
                   pp.PROCESSING_FROM, pp.PROCESSING_THRU,
                   pp.UPDATED_BY,
                   p.NAME as PRODUCT_NAME
            FROM PRODUCT_PRICES pp
            LEFT JOIN PRODUCTS p ON pp.PRODUCT_ID = p.ID
            WHERE ? >= pp.BUSINESS_FROM AND ? < pp.BUSINESS_THRU
              AND ? >= pp.PROCESSING_FROM AND ? < pp.PROCESSING_THRU
            ORDER BY pp.PRODUCT_ID
        """.trimIndent()

        val businessInstant = Instant.parse(businessDate)
        val processingInstant = Instant.parse(processingDate)

        return jdbcTemplate.query(sql, { rs, _ ->
            ProductPriceDto(
                id = rs.getLong("ID"),
                productId = rs.getLong("PRODUCT_ID"),
                productName = rs.getString("PRODUCT_NAME"),
                price = rs.getBigDecimal("PRICE"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant(),
                businessThru = rs.getTimestamp("BUSINESS_THRU").toInstant(),
                processingFrom = rs.getTimestamp("PROCESSING_FROM").toInstant(),
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant(),
                updatedBy = rs.getString("UPDATED_BY")
            )
        }, businessInstant, businessInstant, processingInstant, processingInstant)
    }

    /**
     * Get full history of a product price (all versions across both time dimensions)
     */
    @GetMapping("/product/{productId}/history")
    fun getHistory(@PathVariable productId: Long): List<ProductPriceDto> {
        val sql = """
            SELECT pp.ID, pp.PRODUCT_ID, pp.PRICE,
                   pp.BUSINESS_FROM, pp.BUSINESS_THRU,
                   pp.PROCESSING_FROM, pp.PROCESSING_THRU,
                   pp.UPDATED_BY,
                   p.NAME as PRODUCT_NAME
            FROM PRODUCT_PRICES pp
            LEFT JOIN PRODUCTS p ON pp.PRODUCT_ID = p.ID
            WHERE pp.PRODUCT_ID = ?
            ORDER BY pp.BUSINESS_FROM, pp.PROCESSING_FROM
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            ProductPriceDto(
                id = rs.getLong("ID"),
                productId = rs.getLong("PRODUCT_ID"),
                productName = rs.getString("PRODUCT_NAME"),
                price = rs.getBigDecimal("PRICE"),
                businessFrom = rs.getTimestamp("BUSINESS_FROM").toInstant(),
                businessThru = rs.getTimestamp("BUSINESS_THRU").toInstant(),
                processingFrom = rs.getTimestamp("PROCESSING_FROM").toInstant(),
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant(),
                updatedBy = rs.getString("UPDATED_BY")
            )
        }, productId)
    }

    /**
     * Debug endpoint: Get raw TIMESTAMP values as strings from database
     * This shows the actual values stored in H2 without any timezone conversion
     */
    @GetMapping("/raw")
    fun getRawTimestamps(): List<Map<String, Any?>> {
        val sql = """
            SELECT
                ID,
                PRODUCT_ID,
                PRICE,
                FORMATDATETIME(BUSINESS_FROM, 'yyyy-MM-dd HH:mm:ss') as BUSINESS_FROM_RAW,
                FORMATDATETIME(BUSINESS_THRU, 'yyyy-MM-dd HH:mm:ss') as BUSINESS_THRU_RAW,
                FORMATDATETIME(PROCESSING_FROM, 'yyyy-MM-dd HH:mm:ss') as PROCESSING_FROM_RAW,
                FORMATDATETIME(PROCESSING_THRU, 'yyyy-MM-dd HH:mm:ss') as PROCESSING_THRU_RAW,
                UPDATED_BY
            FROM PRODUCT_PRICES
            WHERE PRODUCT_ID = 1
            ORDER BY ID
        """.trimIndent()

        return jdbcTemplate.queryForList(sql)
    }

    /**
     * Update product price using Reladomo's bitemporal chaining pattern
     *
     * This is the CORRECT way to update bitemporal data in Reladomo:
     * 1. Find the record that is valid at the specified business date using AsOf query
     * 2. Update the price property by calling setter
     * 3. Reladomo automatically handles the chaining:
     *    - Terminates the old record by setting PROCESSING_THRU to now
     *    - Creates a new record with the updated price and PROCESSING_FROM = now
     *    - Preserves BUSINESS_FROM/THRU from the existing record
     */
    @PostMapping
    @Transactional
    fun create(@RequestBody request: CreateProductPriceRequest): ProductPriceDto {
        val businessDate = Instant.parse(request.businessDate)

        // Find existing price record valid at the specified business date using AsOf query
        // For bitemporal updates, we need to find the record that:
        // - productId matches
        // - The specified businessDate falls within [BUSINESS_FROM, BUSINESS_THRU)
        // - PROCESSING_THRU = infinity (currently valid record in processing time)
        val operation = io.github.reladomokotlin.demo.domain.ProductPriceFinder.productId().eq(request.productId)
            .and(io.github.reladomokotlin.demo.domain.ProductPriceFinder.businessDate().eq(java.sql.Timestamp.from(businessDate)))
            .and(io.github.reladomokotlin.demo.domain.ProductPriceFinder.processingDate().equalsInfinity())

        val existing = io.github.reladomokotlin.demo.domain.ProductPriceFinder.findOne(operation)

        if (existing != null) {
            // Update the price and audit info - Reladomo automatically creates new version
            existing.price = request.price
            existing.updatedBy = request.updatedBy
            // Reladomo detects the change and handles the bitemporal chaining:
            // - Old record: PROCESSING_THRU set to now
            // - New record: Same BUSINESS_FROM/THRU, PROCESSING_FROM = now, PROCESSING_THRU = infinity
        } else {
            // No existing record at this business date - create new one
            val newPrice = io.github.reladomokotlin.demo.domain.ProductPrice(java.sql.Timestamp.from(businessDate))
            val newId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(ID), 0) + 1 FROM PRODUCT_PRICES WHERE PRODUCT_ID = ?",
                Long::class.java,
                request.productId
            ) ?: 1L
            newPrice.id = newId
            newPrice.productId = request.productId
            newPrice.price = request.price
            newPrice.updatedBy = request.updatedBy
            newPrice.insert()
        }

        val product = productRepository.findById(request.productId)
        return ProductPriceDto(
            id = existing?.id ?: 0L,
            productId = request.productId,
            productName = product?.name,
            price = request.price,
            businessFrom = businessDate,
            businessThru = java.sql.Timestamp.valueOf("9999-12-01 23:59:00").toInstant(),
            processingFrom = Instant.now(),
            processingThru = java.sql.Timestamp.valueOf("9999-12-01 23:59:00").toInstant(),
            updatedBy = request.updatedBy
        )
    }
}
