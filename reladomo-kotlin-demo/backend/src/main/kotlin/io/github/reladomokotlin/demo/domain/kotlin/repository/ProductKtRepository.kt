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
import io.github.reladomokotlin.demo.domain.Product
import io.github.reladomokotlin.demo.domain.ProductFinder
import io.github.reladomokotlin.demo.domain.kotlin.ProductKt
import io.github.reladomokotlin.demo.domain.kotlin.query.ProductQueryDsl
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
public class ProductKtRepository : UniTemporalRepository<ProductKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: ProductKt): ProductKt {
    val obj = Product()
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Product") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.categoryId = entity.categoryId
    obj.name = entity.name
    entity.description?.let { obj.description = it }
    obj.insert()
    return ProductKt.fromReladomo(obj)
  }

  override fun findById(id: Long): ProductKt? {
    val entity = ProductFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
    return entity?.let { ProductKt.fromReladomo(it) }
  }

  override fun update(entity: ProductKt): ProductKt {
    val existingOrder = ProductFinder.findByPrimaryKey(entity.id!!,  Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: ${entity.id}")

    // Update attributes
    existingOrder.setCategoryId(entity.categoryId)
    existingOrder.setName(entity.name)
    entity.description?.let { existingOrder.setDescription(it) }

    return ProductKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = ProductFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<ProductKt> {
    // For unitemporal queries, use equalsInfinity to get current records (PROCESSING_THRU = infinity)
    val operation = ProductFinder.processingDate().equalsInfinity()
    val orders = ProductFinder.findMany(operation)
    return orders.map { ProductKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<ProductKt> {
    val orders = ProductFinder.findMany(operation)
    return orders.map { ProductKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long = ProductFinder.findMany(operation).size.toLong()

  override fun delete(entity: ProductKt) {
    entity.id?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  override fun findByIdAsOf(id: Long, processingDate: Instant): ProductKt? {
    // Find by primary key as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = ProductFinder.id().eq(id)
        .and(if (processingDate.isAfter(infinityThreshold))
        ProductFinder.processingDate().equalsInfinity() else
        ProductFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = ProductFinder.findOne(operation)
    return entity?.let { ProductKt.fromReladomo(it) }
  }

  override fun findAllAsOf(processingDate: Instant): List<ProductKt> {
    // Find all entities as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = if (processingDate.isAfter(infinityThreshold)) {
        ProductFinder.processingDate().equalsInfinity()
    } else {
        ProductFinder.processingDate().eq(Timestamp.from(processingDate))
    }
    val entities = ProductFinder.findMany(operation)
    return entities.map { ProductKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<ProductKt> {
    // Get all versions of the entity across processing time
    // Use equalsEdgePoint() to retrieve ALL historical records
    // Returns history sorted by processing date (oldest first)
    val operation = ProductFinder.id().eq(id)
        .and(ProductFinder.processingDate().equalsEdgePoint())
    val entities = ProductFinder.findMany(operation)
    return entities.map { ProductKt.fromReladomo(it) }.sortedBy { it.processingDate }
  }

  public fun find(query: QueryContext.() -> Unit): List<ProductKt> {
    // Find entities using Query DSL
    // Use ProductQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = ProductFinder.findMany(operation)
    return results.map { ProductKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): ProductKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = ProductFinder.findOne(operation)
    return result?.let { ProductKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return ProductFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return ProductFinder.findOne(operation) != null
  }
}
