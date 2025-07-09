package io.github.reladomokotlin.spring.repository

import kotlin.reflect.KClass

/**
 * Metadata about a Reladomo entity.
 */
data class ReladomoEntityMetadata<T : Any, ID : Any>(
    val entityType: KClass<T>,
    val idType: KClass<ID>,
    val isBitemporal: Boolean = false
)