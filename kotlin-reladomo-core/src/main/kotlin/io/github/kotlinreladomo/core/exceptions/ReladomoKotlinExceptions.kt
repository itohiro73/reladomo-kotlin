package io.github.kotlinreladomo.core.exceptions

import kotlin.reflect.KClass

/**
 * Base exception for all Kotlin Reladomo exceptions.
 */
open class ReladomoKotlinException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Thrown when an entity is not found in the database.
 */
class EntityNotFoundException : ReladomoKotlinException {
    constructor(message: String) : super(message)
    constructor(entityType: KClass<*>, id: Any) : super(
        "Entity ${entityType.simpleName} with id $id not found"
    )
}

/**
 * Thrown when a temporal constraint is violated.
 */
class TemporalConstraintViolationException(
    message: String
) : ReladomoKotlinException(message)

/**
 * Thrown when an optimistic lock failure occurs.
 */
class OptimisticLockException(
    entity: Any,
    expectedVersion: Int,
    actualVersion: Int
) : ReladomoKotlinException(
    "Optimistic lock failed for $entity. Expected version: $expectedVersion, Actual: $actualVersion"
)

/**
 * Thrown when there's an error in configuration.
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null
) : ReladomoKotlinException(message, cause)