package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductPriceKtRepository
import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductKtRepository
import io.github.reladomokotlin.demo.dto.ProductPriceDto
import io.github.reladomokotlin.demo.dto.CreateProductPriceRequest
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/product-prices")
@CrossOrigin(origins = ["*"])
class ProductPriceController(
    private val repository: ProductPriceKtRepository,
    private val productRepository: ProductKtRepository
) {

    /**
     * Get all product prices as of a specific business date and processing date
     * This demonstrates bitemporal querying
     */
    @GetMapping
    fun getAll(
        @RequestParam(required = false) businessDate: String?,
        @RequestParam(required = false) processingDate: String?
    ): List<ProductPriceDto> {
        val businessInstant = businessDate?.let { Instant.parse(it) } ?: Instant.now()
        val processingInstant = processingDate?.let { Instant.parse(it) } ?: Instant.now()

        return repository.findAllAsOf(businessInstant, processingInstant).map { price ->
            val product = productRepository.findById(price.productId!!)
            ProductPriceDto(
                id = price.id!!,
                productId = price.productId!!,
                productName = product?.name,
                price = price.price,
                businessFrom = price.businessDate,
                businessThru = price.businessDate, // Will be updated to show ranges
                processingFrom = price.processingDate,
                processingThru = price.processingDate
            )
        }
    }

    /**
     * Get price for a specific product with time-travel capability
     * Example: "What price did we think would be effective on Dec 1st, as of Nov 10th?"
     */
    @GetMapping("/product/{productId}")
    fun getByProductId(
        @PathVariable productId: Long,
        @RequestParam(required = false) businessDate: String?,
        @RequestParam(required = false) processingDate: String?
    ): ProductPriceDto? {
        val businessInstant = businessDate?.let { Instant.parse(it) } ?: Instant.now()
        val processingInstant = processingDate?.let { Instant.parse(it) } ?: Instant.now()

        // Query with both temporal dimensions
        val prices = repository.findAllAsOf(businessInstant, processingInstant)
        val price = prices.firstOrNull { it.productId == productId }
            ?: return null

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
     * Get full history of a product price (all versions across both time dimensions)
     */
    @GetMapping("/product/{productId}/history")
    fun getHistory(@PathVariable productId: Long): List<ProductPriceDto> {
        // This would require a special query to get ALL versions
        // For now, return current version
        val current = repository.findAll().filter { it.productId == productId }

        return current.map { price ->
            val product = productRepository.findById(price.productId!!)
            ProductPriceDto(
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
