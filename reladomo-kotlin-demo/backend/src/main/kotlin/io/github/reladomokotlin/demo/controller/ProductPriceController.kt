package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductPriceKtRepository
import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductKtRepository
import io.github.reladomokotlin.demo.dto.ProductPriceDto
import io.github.reladomokotlin.demo.dto.CreateProductPriceRequest
import org.springframework.jdbc.core.JdbcTemplate
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
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant()
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
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant()
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
                processingThru = rs.getTimestamp("PROCESSING_THRU").toInstant()
            )
        }, productId)
    }

    /**
     * Create a new product price effective from a specific business date
     */
    @PostMapping
    fun create(@RequestBody request: CreateProductPriceRequest): ProductPriceDto {
        val businessDate = Instant.parse(request.businessDate)

        val price = repository.save(
            io.github.reladomokotlin.demo.domain.kotlin.ProductPriceKt(
                id = 0L, // Will be generated
                productId = request.productId,
                price = request.price,
                businessDate = businessDate,
                processingDate = Instant.now()
            )
        )

        val product = productRepository.findById(price.productId!!)
        return ProductPriceDto(
            id = price.id!!,
            productId = price.productId!!,
            productName = product?.name,
            price = price.price,
            businessFrom = price.businessDate,
            businessThru = price.businessDate,
            processingFrom = price.processingDate,
            processingThru = price.processingDate
        )
    }

    /**
     * Update an existing price (creates a new version in processing time)
     */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: CreateProductPriceRequest
    ): ProductPriceDto {
        // Find the current price
        val existing = repository.findById(id)
            ?: throw NotFoundException("Price not found: $id")

        // Terminate the old version by updating processing date
        // Then insert new version
        val businessDate = Instant.parse(request.businessDate)

        val updated = repository.update(
            existing.copy(
                price = request.price,
                businessDate = businessDate
            ),
            businessDate
        )

        val product = productRepository.findById(updated.productId!!)
        return ProductPriceDto(
            id = updated.id!!,
            productId = updated.productId!!,
            productName = product?.name,
            price = updated.price,
            businessFrom = updated.businessDate,
            businessThru = updated.businessDate,
            processingFrom = updated.processingDate,
            processingThru = updated.processingDate
        )
    }
}
