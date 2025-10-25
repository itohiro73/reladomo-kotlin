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
                description = request.description
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
}
