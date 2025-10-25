package io.github.reladomokotlin.demo.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.core.ReladomoFinder
import io.github.reladomokotlin.core.ReladomoObject
import io.github.reladomokotlin.core.exceptions.EntityNotFoundException
import io.github.reladomokotlin.demo.domain.ProductPrice
import io.github.reladomokotlin.demo.domain.ProductPriceFinder
import io.github.reladomokotlin.demo.domain.kotlin.ProductPriceKt
import io.github.reladomokotlin.demo.domain.kotlin.query.ProductPriceQueryDsl
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
public class ProductPriceKtRepository : BiTemporalRepository<ProductPriceKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: ProductPriceKt): ProductPriceKt {
    val obj = ProductPrice(Timestamp.from(entity.businessDate))
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("ProductPrice") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.productId = entity.productId
    obj.price = entity.price
    entity.updatedBy?.let { obj.updatedBy = it }
    obj.insert()
    return ProductPriceKt.fromReladomo(obj)
  }

  override fun findById(id: Long): ProductPriceKt? {
    // For bitemporal objects, find active record (businessDate at infinity, processingDate at transaction time)
    val operation = ProductPriceFinder.id().eq(id)
        .and(ProductPriceFinder.businessDate().equalsInfinity())
        .and(ProductPriceFinder.processingDate().equalsEdgePoint())
    val entity = ProductPriceFinder.findOne(operation)
    return entity?.let { ProductPriceKt.fromReladomo(it) }
  }

  override fun update(entity: ProductPriceKt, businessDate: Instant): ProductPriceKt {
    // For bitemporal objects, find record with infinity processing date at specified business date
    val operation = ProductPriceFinder.id().eq(entity.id!!)
        .and(ProductPriceFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(ProductPriceFinder.processingDate().equalsInfinity())
    val existingEntity = ProductPriceFinder.findOne(operation)
        ?: throw EntityNotFoundException("ProductPrice not found with id: ${entity.id}")

    // Update fields - Reladomo handles bitemporal chaining
    existingEntity.setProductId(entity.productId)
    existingEntity.setPrice(entity.price)
    entity.updatedBy?.let { existingEntity.setUpdatedBy(it) }

    return ProductPriceKt.fromReladomo(existingEntity)
  }

  override fun deleteById(id: Long) {
    deleteByIdAsOf(id, Instant.now())
  }

  override fun findAll(): List<ProductPriceKt> {
    // For bitemporal queries, use equalsEdgePoint to get active records
    val operation = ProductPriceFinder.businessDate().equalsEdgePoint()
        .and(ProductPriceFinder.processingDate().equalsEdgePoint())

    val orders = ProductPriceFinder.findMany(operation)
    return orders.map { ProductPriceKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<ProductPriceKt> {
    val orders = ProductPriceFinder.findMany(operation)
    return orders.map { ProductPriceKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      ProductPriceFinder.findMany(operation).size.toLong()

  override fun delete(entity: ProductPriceKt) {
    entity.id?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  override fun findByIdAsOf(
    id: Long,
    businessDate: Instant,
    processingDate: Instant,
  ): ProductPriceKt? {
    // Find by primary key as of specific business and processing dates
    // Use operation-based query to handle infinity dates correctly
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = ProductPriceFinder.id().eq(id)
        .and(ProductPriceFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(if (processingDate.isAfter(infinityThreshold))
        ProductPriceFinder.processingDate().equalsInfinity() else
        ProductPriceFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = ProductPriceFinder.findOne(operation)
    return entity?.let { ProductPriceKt.fromReladomo(it) }
  }

  override fun update(entity: ProductPriceKt): ProductPriceKt = update(entity, Instant.now())

  override fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<ProductPriceKt> {
    val operation = ProductPriceFinder.businessDate().eq(Timestamp.from(businessDate))
        .and(ProductPriceFinder.processingDate().eq(Timestamp.from(processingDate)))

    val orders = ProductPriceFinder.findMany(operation)
    return orders.map { ProductPriceKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<ProductPriceKt> {
    // Get all versions of the entity across time
    // For now, returns current version only. Full temporal history query requires
    // using MithraManager API or database-specific queries.
    val current = findById(id)
    return if (current != null) listOf(current) else emptyList()
  }

  override fun deleteByIdAsOf(id: Long, businessDate: Instant) {
    // For bitemporal objects, find record with infinity processing date at specified business date for termination
    val operation = ProductPriceFinder.id().eq(id)
        .and(ProductPriceFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(ProductPriceFinder.processingDate().equalsInfinity())
    val entity = ProductPriceFinder.findOne(operation)
        ?: throw EntityNotFoundException("ProductPrice not found with id: $id")
    entity.terminate()
  }

  public fun find(query: QueryContext.() -> Unit): List<ProductPriceKt> {
    // Find entities using Query DSL
    // Use ProductPriceQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = ProductPriceFinder.findMany(operation)
    return results.map { ProductPriceKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): ProductPriceKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = ProductPriceFinder.findOne(operation)
    return result?.let { ProductPriceKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return ProductPriceFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return ProductPriceFinder.findOne(operation) != null
  }
}
