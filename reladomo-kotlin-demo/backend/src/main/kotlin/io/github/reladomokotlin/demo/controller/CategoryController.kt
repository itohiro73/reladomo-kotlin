package io.github.reladomokotlin.demo.controller

import io.github.reladomokotlin.demo.domain.kotlin.repository.CategoryKtRepository
import io.github.reladomokotlin.demo.dto.CategoryDto
import io.github.reladomokotlin.demo.dto.CreateCategoryRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = ["*"])
class CategoryController(
    private val repository: CategoryKtRepository
) {

    @GetMapping
    fun getAll(): List<CategoryDto> {
        return repository.findAll().map { category ->
            CategoryDto(
                id = category.id,
                name = category.name,
                description = category.description
            )
        }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): CategoryDto {
        val category = repository.findById(id)
            ?: throw NotFoundException("Category not found: $id")
        return CategoryDto(
            id = category.id,
            name = category.name,
            description = category.description
        )
    }

    @PostMapping
    fun create(@RequestBody request: CreateCategoryRequest): CategoryDto {
        val category = repository.save(
            io.github.reladomokotlin.demo.domain.kotlin.CategoryKt(
                id = null,
                name = request.name,
                description = request.description
            )
        )
        return CategoryDto(
            id = category.id,
            name = category.name,
            description = category.description
        )
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: CreateCategoryRequest): CategoryDto {
        val existing = repository.findById(id)
            ?: throw NotFoundException("Category not found: $id")

        val updated = repository.save(
            existing.copy(
                name = request.name,
                description = request.description
            )
        )
        return CategoryDto(
            id = updated.id,
            name = updated.name,
            description = updated.description
        )
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) {
        repository.deleteById(id)
    }
}

@ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
class NotFoundException(message: String) : RuntimeException(message)
