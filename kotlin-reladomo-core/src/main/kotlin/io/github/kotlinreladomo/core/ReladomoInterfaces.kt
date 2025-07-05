package io.github.kotlinreladomo.core

import com.gs.fw.common.mithra.finder.Operation

/**
 * Interface representing a Reladomo object with basic operations.
 * This abstracts the actual MithraObject to avoid direct dependency in core module.
 */
interface ReladomoObject {
    fun insert()
    fun update()
    fun delete()
    fun terminate()
}

/**
 * Interface representing a Reladomo finder.
 */
interface ReladomoFinder<T> {
    fun findOne(operation: Operation): T?
    fun findMany(operation: Operation): List<T>
    fun all(): Operation
}