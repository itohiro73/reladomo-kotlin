package io.github.reladomokotlin.sample.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.core.ReladomoFinder
import io.github.reladomokotlin.core.ReladomoObject
import io.github.reladomokotlin.core.exceptions.EntityNotFoundException
import io.github.reladomokotlin.query.QueryContext
import io.github.reladomokotlin.query.query
import io.github.reladomokotlin.sample.domain.Order
import io.github.reladomokotlin.sample.domain.OrderFinder
import io.github.reladomokotlin.sample.domain.kotlin.OrderKt
import io.github.reladomokotlin.sample.domain.kotlin.query.OrderQueryDsl
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
public class OrderKtRepository : BiTemporalRepository<OrderKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: OrderKt): OrderKt {
    val obj = Order(Timestamp.from(entity.businessDate))
    val orderId = entity.orderId?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Order") ?:
        throw IllegalStateException("No ID provided and sequence generator not available")
    obj.orderId = orderId
    obj.customerId = entity.customerId
    obj.orderDate = Timestamp.from(entity.orderDate)
    obj.amount = entity.amount
    obj.status = entity.status
    entity.description?.let { obj.description = it }
    obj.insert()
    return OrderKt.fromReladomo(obj)
  }

  override fun findById(id: Long): OrderKt? {
    // For bitemporal objects, find active record (businessDate at infinity, processingDate at transaction time)
    val operation = OrderFinder.orderId().eq(id)
        .and(OrderFinder.businessDate().equalsInfinity())
        .and(OrderFinder.processingDate().equalsEdgePoint())
    val entity = OrderFinder.findOne(operation)
    return entity?.let { OrderKt.fromReladomo(it) }
  }

  override fun update(entity: OrderKt, businessDate: Instant): OrderKt {
    // For bitemporal objects, find record with infinity processing date at specified business date
    val operation = OrderFinder.orderId().eq(entity.orderId!!)
        .and(OrderFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(OrderFinder.processingDate().equalsInfinity())
    val existingEntity = OrderFinder.findOne(operation)
        ?: throw EntityNotFoundException("Order not found with id: ${entity.orderId}")

    // Update fields - Reladomo handles bitemporal chaining
    existingEntity.setCustomerId(entity.customerId)
    existingEntity.setOrderDate(Timestamp.from(entity.orderDate))
    existingEntity.setAmount(entity.amount)
    existingEntity.setStatus(entity.status)
    entity.description?.let { existingEntity.setDescription(it) }

    return OrderKt.fromReladomo(existingEntity)
  }

  override fun deleteById(id: Long) {
    deleteByIdAsOf(id, Instant.now())
  }

  override fun findAll(): List<OrderKt> {
    // For bitemporal queries, use equalsEdgePoint to get active records
    val operation = OrderFinder.businessDate().equalsEdgePoint()
        .and(OrderFinder.processingDate().equalsEdgePoint())

    val orders = OrderFinder.findMany(operation)
    return orders.map { OrderKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<OrderKt> {
    val orders = OrderFinder.findMany(operation)
    return orders.map { OrderKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long = OrderFinder.findMany(operation).size.toLong()

  override fun delete(entity: OrderKt) {
    entity.orderId?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  public fun findByCustomerId(customerId: Long): List<OrderKt> {
    val operation = OrderFinder.customerId().eq(customerId)
        .and(OrderFinder.businessDate().equalsEdgePoint())
        .and(OrderFinder.processingDate().equalsEdgePoint())

    val orders = OrderFinder.findMany(operation)
    return orders.map { OrderKt.fromReladomo(it) }
  }

  override fun findByIdAsOf(
    id: Long,
    businessDate: Instant,
    processingDate: Instant,
  ): OrderKt? {
    // Find by primary key as of specific business and processing dates
    // Use operation-based query to handle infinity dates correctly
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = OrderFinder.orderId().eq(id)
        .and(OrderFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(if (processingDate.isAfter(infinityThreshold))
        OrderFinder.processingDate().equalsInfinity() else
        OrderFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = OrderFinder.findOne(operation)
    return entity?.let { OrderKt.fromReladomo(it) }
  }

  override fun update(entity: OrderKt): OrderKt = update(entity, Instant.now())

  override fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<OrderKt> {
    val operation = OrderFinder.businessDate().eq(Timestamp.from(businessDate))
        .and(OrderFinder.processingDate().eq(Timestamp.from(processingDate)))

    val orders = OrderFinder.findMany(operation)
    return orders.map { OrderKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<OrderKt> {
    // Get all versions of the entity across time
    // For now, returns current version only. Full temporal history query requires
    // using MithraManager API or database-specific queries.
    val current = findById(id)
    return if (current != null) listOf(current) else emptyList()
  }

  override fun deleteByIdAsOf(id: Long, businessDate: Instant) {
    // For bitemporal objects, find record with infinity processing date at specified business date for termination
    val operation = OrderFinder.orderId().eq(id)
        .and(OrderFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(OrderFinder.processingDate().equalsInfinity())
    val entity = OrderFinder.findOne(operation)
        ?: throw EntityNotFoundException("Order not found with id: $id")
    entity.terminate()
  }

  public fun find(query: QueryContext.() -> Unit): List<OrderKt> {
    // Find entities using Query DSL
    // Use OrderQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = OrderFinder.findMany(operation)
    return results.map { OrderKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): OrderKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = OrderFinder.findOne(operation)
    return result?.let { OrderKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return OrderFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return OrderFinder.findOne(operation) != null
  }
}
