package io.github.reladomokotlin.demo.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.core.ReladomoFinder
import io.github.reladomokotlin.core.ReladomoObject
import io.github.reladomokotlin.core.UniTemporalEntity
import io.github.reladomokotlin.core.UniTemporalRepository
import io.github.reladomokotlin.core.exceptions.EntityNotFoundException
import io.github.reladomokotlin.demo.domain.Category
import io.github.reladomokotlin.demo.domain.CategoryFinder
import io.github.reladomokotlin.demo.domain.kotlin.CategoryKt
import io.github.reladomokotlin.demo.domain.kotlin.query.CategoryQueryDsl
import io.github.reladomokotlin.query.QueryContext
import io.github.reladomokotlin.query.query
import io.github.reladomokotlin.sequence.SequenceGenerator
import java.sql.Timestamp
import java.time.Instant
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.Unit
import kotlin.collections.List
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.`annotation`.Transactional

@Repository
@Transactional
public class CategoryKtRepository : BaseRepository<CategoryKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: CategoryKt): CategoryKt {
    val obj = Category()
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Category") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.name = entity.name
    entity.description?.let { obj.description = it }
    obj.insert()
    return CategoryKt.fromReladomo(obj)
  }

  override fun findById(id: Long): CategoryKt? {
    val entity = CategoryFinder.findByPrimaryKey(id)
    return entity?.let { CategoryKt.fromReladomo(it) }
  }

  override fun update(entity: CategoryKt): CategoryKt {
    val existingOrder = CategoryFinder.findByPrimaryKey(entity.id!!)
        ?: throw EntityNotFoundException("Order not found with id: ${entity.id}")

    // Update attributes
    existingOrder.setName(entity.name)
    entity.description?.let { existingOrder.setDescription(it) }

    return CategoryKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = CategoryFinder.findByPrimaryKey(id)
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<CategoryKt> {
    val orders = CategoryFinder.findMany(CategoryFinder.all())
    return orders.map { CategoryKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<CategoryKt> {
    val orders = CategoryFinder.findMany(operation)
    return orders.map { CategoryKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      CategoryFinder.findMany(operation).size.toLong()

  override fun delete(entity: CategoryKt) {
    entity.id?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  public fun find(query: QueryContext.() -> Unit): List<CategoryKt> {
    // Find entities using Query DSL
    // Use CategoryQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = CategoryFinder.findMany(operation)
    return results.map { CategoryKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): CategoryKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = CategoryFinder.findOne(operation)
    return result?.let { CategoryKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return CategoryFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return CategoryFinder.findOne(operation) != null
  }
}
