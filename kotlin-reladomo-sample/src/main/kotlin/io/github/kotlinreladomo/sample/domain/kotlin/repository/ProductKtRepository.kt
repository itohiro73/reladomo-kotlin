package io.github.kotlinreladomo.sample.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.kotlinreladomo.core.BaseRepository
import io.github.kotlinreladomo.core.BiTemporalEntity
import io.github.kotlinreladomo.core.BiTemporalRepository
import io.github.kotlinreladomo.core.ReladomoFinder
import io.github.kotlinreladomo.core.ReladomoObject
import io.github.kotlinreladomo.core.exceptions.EntityNotFoundException
import io.github.kotlinreladomo.query.QueryContext
import io.github.kotlinreladomo.query.query
import io.github.kotlinreladomo.sample.domain.Product
import io.github.kotlinreladomo.sample.domain.ProductFinder
import io.github.kotlinreladomo.sample.domain.kotlin.ProductKt
import io.github.kotlinreladomo.sample.domain.kotlin.query.ProductQueryDsl
import io.github.kotlinreladomo.sequence.SequenceGenerator
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
public class ProductKtRepository : BaseRepository<ProductKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: ProductKt): ProductKt {
    val obj = Product()
    val productId = entity.productId?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Product")
        ?: throw IllegalStateException("No ID provided and sequence generator not available")
    obj.productId = productId
    obj.name = entity.name
    entity.description?.let { obj.description = it }
    obj.price = entity.price
    obj.stockQuantity = entity.stockQuantity
    entity.category?.let { obj.category = it }
    obj.insert()
    return ProductKt.fromReladomo(obj)
  }

  override fun findById(id: Long): ProductKt? {
    val order = ProductFinder.findByPrimaryKey(id)
    return order?.let { ProductKt.fromReladomo(it) }
  }

  override fun update(entity: ProductKt): ProductKt {
    val existingOrder = ProductFinder.findByPrimaryKey(entity.productId!!)
        ?: throw EntityNotFoundException("Order not found with id: ${entity.productId}")

    // Update attributes
    existingOrder.setName(entity.name)
    entity.description?.let { existingOrder.setDescription(it) }
    existingOrder.setPrice(entity.price)
    existingOrder.setStockQuantity(entity.stockQuantity)
    entity.category?.let { existingOrder.setCategory(it) }

    return ProductKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = ProductFinder.findByPrimaryKey(id)
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<ProductKt> {
    val orders = ProductFinder.findMany(ProductFinder.all())
    return orders.map { ProductKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<ProductKt> {
    val orders = ProductFinder.findMany(operation)
    return orders.map { ProductKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long = ProductFinder.findMany(operation).size.toLong()

  override fun delete(entity: ProductKt) {
    entity.productId?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  public fun find(query: QueryContext.() -> Unit): List<ProductKt> {
    // Find entities using Query DSL
    // Use ProductQueryDsl extensions to access attribute properties
    val operation = io.github.kotlinreladomo.query.query(query)
    val results = ProductFinder.findMany(operation)
    return results.map { ProductKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): ProductKt? {
    // Find a single entity using Query DSL
    val operation = io.github.kotlinreladomo.query.query(query)
    val result = ProductFinder.findOne(operation)
    return result?.let { ProductKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.kotlinreladomo.query.query(query)
    return ProductFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.kotlinreladomo.query.query(query)
    return ProductFinder.findOne(operation) != null
  }
}
