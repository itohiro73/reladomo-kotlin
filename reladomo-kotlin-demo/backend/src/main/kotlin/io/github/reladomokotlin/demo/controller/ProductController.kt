package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.domain.kotlin.repository.ProductKtRepository
import io.github.reladomokotlin.demo.domain.kotlin.repository.CategoryKtRepository
import io.github.reladomokotlin.demo.dto.ProductDto
import io.github.reladomokotlin.demo.dto.CreateProductRequest
import org.springframework.web.bind.annotation.*
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
        return repository.findAll().map { product ->
            val category = categoryRepository.findById(product.categoryId)
            ProductDto(
                id = product.id,
                categoryId = product.categoryId,
                categoryName = category?.name,
                name = product.name,
                description = product.description
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
        return repository.getHistory(id).map { product ->
            val category = categoryRepository.findById(product.categoryId)
            ProductDto(
                id = product.id,
                categoryId = product.categoryId,
                categoryName = category?.name,
                name = product.name,
                description = product.description,
                processingFrom = product.processingDate,
                processingThru = product.processingDate  // TODO: Fix to get actual PROCESSING_THRU
            )
        }
    }

    /**
     * Get all products as of a specific processing date
     * Example: /api/products/asof?processingDate=2025-08-01T00:00:00Z
     */
    @GetMapping("/asof")
    fun getAllAsOf(@RequestParam processingDate: String): List<ProductDto> {
        val instant = Instant.parse(processingDate)
        return repository.findAllAsOf(instant).map { product ->
            val category = categoryRepository.findById(product.categoryId)
            ProductDto(
                id = product.id,
                categoryId = product.categoryId,
                categoryName = category?.name,
                name = product.name,
                description = product.description,
                processingFrom = product.processingDate,
                processingThru = product.processingDate  // TODO: Fix to get actual PROCESSING_THRU
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
        val product = repository.findByIdAsOf(id, instant)
            ?: throw NotFoundException("Product not found: $id at $processingDate")

        val category = categoryRepository.findById(product.categoryId)
        return ProductDto(
            id = product.id,
            categoryId = product.categoryId,
            categoryName = category?.name,
            name = product.name,
            description = product.description,
            processingFrom = product.processingDate,
            processingThru = product.processingDate  // TODO: Fix to get actual PROCESSING_THRU
        )
    }
}
