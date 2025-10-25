package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductKtRepository
import io.github.reladomokotlin.demo.domain.kotlin.repository.CategoryKtRepository
import io.github.reladomokotlin.demo.domain.ProductFinder
import io.github.reladomokotlin.demo.dto.ProductDto
import io.github.reladomokotlin.demo.dto.CreateProductRequest
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.time.Instant

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = ["*"])
class ProductController(
    private val repository: ProductKtRepository,
    private val categoryRepository: CategoryKtRepository
) {

    @GetMapping
    fun getAll(): List<ProductDto> {
        // Get current products using equalsInfinity to get PROCESSING_THRU = infinity records
        val operation = ProductFinder.processingDate().equalsInfinity()
        val products = ProductFinder.findMany(operation)

        return products.map { reladomoProduct ->
            val category = categoryRepository.findById(reladomoProduct.categoryId)
            ProductDto(
                id = reladomoProduct.id,
                categoryId = reladomoProduct.categoryId,
                categoryName = category?.name,
                name = reladomoProduct.name,
                description = reladomoProduct.description,
                processingFrom = reladomoProduct.processingDateFrom?.toInstant(),
                processingThru = reladomoProduct.processingDateTo?.toInstant()
            )
        }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ProductDto {
        val product = repository.findById(id)
            ?: throw NotFoundException("Product not found: $id")

        val category = categoryRepository.findById(product.categoryId)
        return ProductDto(
            id = product.id,
            categoryId = product.categoryId,
            categoryName = category?.name,
            name = product.name,
            description = product.description
        )
    }

    @PostMapping
    fun create(@RequestBody request: CreateProductRequest): ProductDto {
        val product = repository.save(
            io.github.reladomokotlin.demo.domain.kotlin.ProductKt(
                id = null,
                categoryId = request.categoryId,
                name = request.name,
                description = request.description,
                processingDate = Instant.now()
            )
        )

        val category = categoryRepository.findById(product.categoryId)
        return ProductDto(
            id = product.id,
            categoryId = product.categoryId,
            categoryName = category?.name,
            name = product.name,
            description = product.description
        )
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        repository.deleteById(id)
    }

    /**
     * Get all versions of a product (version history)
     * Shows temporal evolution of product information
     */
    @GetMapping("/{id}/history")
    fun getHistory(@PathVariable id: Long): List<ProductDto> {
        // Get all versions using equalsEdgePoint
        val operation = ProductFinder.id().eq(id)
            .and(ProductFinder.processingDate().equalsEdgePoint())
        val products = ProductFinder.findMany(operation)

        return products.map { reladomoProduct ->
            val category = categoryRepository.findById(reladomoProduct.categoryId)
            ProductDto(
                id = reladomoProduct.id,
                categoryId = reladomoProduct.categoryId,
                categoryName = category?.name,
                name = reladomoProduct.name,
                description = reladomoProduct.description,
                processingFrom = reladomoProduct.processingDateFrom?.toInstant(),
                processingThru = reladomoProduct.processingDateTo?.toInstant()
            )
        }.sortedBy { it.processingFrom }
    }

    /**
     * Get all products as of a specific processing date
     * Example: /api/products/asof?processingDate=2025-08-01T00:00:00Z
     */
    @GetMapping("/asof")
    fun getAllAsOf(@RequestParam processingDate: String): List<ProductDto> {
        val instant = Instant.parse(processingDate)
        val operation = ProductFinder.processingDate().eq(Timestamp.from(instant))
        val products = ProductFinder.findMany(operation)

        return products.map { reladomoProduct ->
            val category = categoryRepository.findById(reladomoProduct.categoryId)
            ProductDto(
                id = reladomoProduct.id,
                categoryId = reladomoProduct.categoryId,
                categoryName = category?.name,
                name = reladomoProduct.name,
                description = reladomoProduct.description,
                processingFrom = reladomoProduct.processingDateFrom?.toInstant(),
                processingThru = reladomoProduct.processingDateTo?.toInstant()
            )
        }
    }

    /**
     * Get a specific product as of a specific processing date
     * Example: /api/products/1/asof?processingDate=2025-08-01T00:00:00Z
     */
    @GetMapping("/{id}/asof")
    fun getByIdAsOf(
        @PathVariable id: Long,
        @RequestParam processingDate: String
    ): ProductDto {
        val instant = Instant.parse(processingDate)
        val operation = ProductFinder.id().eq(id)
            .and(ProductFinder.processingDate().eq(Timestamp.from(instant)))
        val product = ProductFinder.findOne(operation)
            ?: throw NotFoundException("Product not found: $id at $processingDate")

        val category = categoryRepository.findById(product.categoryId)
        return ProductDto(
            id = product.id,
            categoryId = product.categoryId,
            categoryName = category?.name,
            name = product.name,
            description = product.description,
            processingFrom = product.processingDateFrom?.toInstant(),
            processingThru = product.processingDateTo?.toInstant()
        )
    }
}
