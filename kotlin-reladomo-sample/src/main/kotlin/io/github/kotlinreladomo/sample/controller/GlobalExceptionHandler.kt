package io.github.kotlinreladomo.sample.controller

import io.github.kotlinreladomo.core.exceptions.EntityNotFoundException
import io.github.kotlinreladomo.core.exceptions.ReladomoKotlinException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                error = "NOT_FOUND",
                message = ex.message ?: "Entity not found",
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(ReladomoKotlinException::class)
    fun handleReladomoException(ex: ReladomoKotlinException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = "BAD_REQUEST",
                message = ex.message ?: "Invalid request",
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                error = "INTERNAL_SERVER_ERROR",
                message = "An unexpected error occurred",
                timestamp = Instant.now()
            ))
    }
}

data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: Instant
)