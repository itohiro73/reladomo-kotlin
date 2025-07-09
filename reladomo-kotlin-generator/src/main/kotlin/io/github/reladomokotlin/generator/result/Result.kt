package io.github.reladomokotlin.generator.result

/**
 * Result type for safe error handling in code generation
 */
sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val error: GeneratorError) : Result<T>()
    
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> Failure(error)
    }
    
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(value)
        is Failure -> Failure(error)
    }
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }
    
    inline fun onFailure(action: (GeneratorError) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }
    
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error.toException()
    }
    
    fun getOrElse(default: T): T = when (this) {
        is Success -> value
        is Failure -> default
    }
    
    inline fun getOrElse(default: () -> T): T = when (this) {
        is Success -> value
        is Failure -> default()
    }
    
    companion object {
        fun <T> success(value: T): Result<T> = Success(value)
        fun <T> failure(error: GeneratorError): Result<T> = Failure(error)
        
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Failure(GeneratorError.UnexpectedError(e.message ?: "Unknown error", e))
            }
        }
    }
}

/**
 * Sealed class representing different types of generator errors
 */
sealed class GeneratorError {
    abstract val message: String
    
    data class ParseError(
        override val message: String,
        val file: String? = null,
        val line: Int? = null
    ) : GeneratorError()
    
    data class ValidationError(
        override val message: String,
        val objectName: String? = null,
        val attributeName: String? = null
    ) : GeneratorError()
    
    data class TypeMappingError(
        override val message: String,
        val xmlType: String,
        val context: String? = null
    ) : GeneratorError()
    
    data class CodeGenerationError(
        override val message: String,
        val className: String? = null,
        val cause: Throwable? = null
    ) : GeneratorError()
    
    data class FileSystemError(
        override val message: String,
        val path: String? = null,
        val cause: Throwable? = null
    ) : GeneratorError()
    
    data class ConfigurationError(
        override val message: String,
        val property: String? = null
    ) : GeneratorError()
    
    data class UnexpectedError(
        override val message: String,
        val cause: Throwable? = null
    ) : GeneratorError()
    
    fun toException(): GeneratorException = GeneratorException(this)
    
    override fun toString(): String = when (this) {
        is ParseError -> "Parse Error${file?.let { " in $it" } ?: ""}${line?.let { " at line $it" } ?: ""}: $message"
        is ValidationError -> "Validation Error${objectName?.let { " in $it" } ?: ""}${attributeName?.let { ".$it" } ?: ""}: $message"
        is TypeMappingError -> "Type Mapping Error for '$xmlType'${context?.let { " in $it" } ?: ""}: $message"
        is CodeGenerationError -> "Code Generation Error${className?.let { " for $it" } ?: ""}: $message"
        is FileSystemError -> "File System Error${path?.let { " at $it" } ?: ""}: $message"
        is ConfigurationError -> "Configuration Error${property?.let { " for property '$it'" } ?: ""}: $message"
        is UnexpectedError -> "Unexpected Error: $message"
    }
}

/**
 * Exception wrapper for generator errors
 */
class GeneratorException(val error: GeneratorError) : Exception(error.toString(), 
    when (error) {
        is GeneratorError.CodeGenerationError -> error.cause
        is GeneratorError.FileSystemError -> error.cause
        is GeneratorError.UnexpectedError -> error.cause
        else -> null
    }
)

/**
 * Extension functions for working with lists of Results
 */
fun <T> List<Result<T>>.sequence(): Result<List<T>> {
    val successes = mutableListOf<T>()
    
    for (result in this) {
        when (result) {
            is Result.Success -> successes.add(result.value)
            is Result.Failure -> return Result.Failure(result.error)
        }
    }
    
    return Result.Success(successes)
}

fun <T, R> List<T>.traverse(transform: (T) -> Result<R>): Result<List<R>> {
    return this.map(transform).sequence()
}