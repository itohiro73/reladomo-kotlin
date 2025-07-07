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
import io.github.kotlinreladomo.sample.domain.OrderItem
import io.github.kotlinreladomo.sample.domain.OrderItemFinder
import io.github.kotlinreladomo.sample.domain.kotlin.OrderItemKt
import io.github.kotlinreladomo.sample.domain.kotlin.query.OrderItemQueryDsl
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
public class OrderItemKtRepository : BaseRepository<OrderItemKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: OrderItemKt): OrderItemKt {
    val obj = OrderItem()
    val orderItemId = entity.orderItemId?.takeIf { it != 0L } ?:
        sequenceGenerator?.getNextId("OrderItem") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.orderItemId = orderItemId
    obj.orderId = entity.orderId
    obj.productId = entity.productId
    obj.quantity = entity.quantity
    obj.unitPrice = entity.unitPrice
    obj.totalPrice = entity.totalPrice
    obj.insert()
    return OrderItemKt.fromReladomo(obj)
  }

  override fun findById(id: Long): OrderItemKt? {
    val order = OrderItemFinder.findByPrimaryKey(id)
    return order?.let { OrderItemKt.fromReladomo(it) }
  }

  override fun update(entity: OrderItemKt): OrderItemKt {
    val existingOrder = OrderItemFinder.findByPrimaryKey(entity.orderItemId!!)
        ?: throw EntityNotFoundException("Order not found with id: ${entity.orderItemId}")

    // Update attributes
    existingOrder.setOrderId(entity.orderId)
    existingOrder.setProductId(entity.productId)
    existingOrder.setQuantity(entity.quantity)
    existingOrder.setUnitPrice(entity.unitPrice)
    existingOrder.setTotalPrice(entity.totalPrice)

    return OrderItemKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = OrderItemFinder.findByPrimaryKey(id)
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<OrderItemKt> {
    val orders = OrderItemFinder.findMany(OrderItemFinder.all())
    return orders.map { OrderItemKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<OrderItemKt> {
    val orders = OrderItemFinder.findMany(operation)
    return orders.map { OrderItemKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      OrderItemFinder.findMany(operation).size.toLong()

  override fun delete(entity: OrderItemKt) {
    entity.orderItemId?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  public fun find(query: QueryContext.() -> Unit): List<OrderItemKt> {
    // Find entities using Query DSL
    // Use OrderItemQueryDsl extensions to access attribute properties
    val operation = io.github.kotlinreladomo.query.query(query)
    val results = OrderItemFinder.findMany(operation)
    return results.map { OrderItemKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): OrderItemKt? {
    // Find a single entity using Query DSL
    val operation = io.github.kotlinreladomo.query.query(query)
    val result = OrderItemFinder.findOne(operation)
    return result?.let { OrderItemKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.kotlinreladomo.query.query(query)
    return OrderItemFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.kotlinreladomo.query.query(query)
    return OrderItemFinder.findOne(operation) != null
  }
}
