package io.github.kotlinreladomo.sample.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.kotlinreladomo.core.AbstractBiTemporalRepository
import io.github.kotlinreladomo.core.ReladomoFinder
import io.github.kotlinreladomo.core.ReladomoObject
import io.github.kotlinreladomo.core.exceptions.EntityNotFoundException
import io.github.kotlinreladomo.sample.domain.Order
import io.github.kotlinreladomo.sample.domain.OrderFinder
import io.github.kotlinreladomo.sample.domain.kotlin.OrderKt
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.collections.List
import org.springframework.stereotype.Repository
import org.springframework.transaction.`annotation`.Transactional

@Repository
@Transactional
public class OrderKtRepository {
  public fun save(entity: OrderKt): OrderKt {
    val reladomoObject = entity.toReladomo()
    reladomoObject.insert()
    return OrderKt.fromReladomo(reladomoObject)
  }

  public fun findById(id: Long): OrderKt? {
    // For bitemporal objects, find the current active record
    // Use infinity processing date to get the latest version
    val now = Timestamp.from(Instant.now())
    val infinityTs = Timestamp.valueOf("9999-12-01 23:59:00.0")
    val order = OrderFinder.findByPrimaryKey(id, now, infinityTs)
    return order?.let { OrderKt.fromReladomo(it) }
  }

  public fun findByIdAsOf(
    id: Long,
    businessDate: Instant,
    processingDate: Instant,
  ): OrderKt? {
    // Find by primary key as of specific business and processing dates
    val order = OrderFinder.findByPrimaryKey(id, Timestamp.from(businessDate),
        Timestamp.from(processingDate))
    return order?.let { OrderKt.fromReladomo(it) }
  }

  public fun update(entity: OrderKt, businessDate: Instant = Instant.now()): OrderKt {
    // For bitemporal objects, fetch as of the business date and update
    // Processing date should be infinity to get the current active record
    val businessDateTs = Timestamp.from(businessDate)
    val infinityTs = Timestamp.valueOf("9999-12-01 23:59:00.0")

    val existingOrder = OrderFinder.findByPrimaryKey(entity.orderId!!, businessDateTs, infinityTs)
        ?: throw EntityNotFoundException("Order not found with id: ${entity.orderId}")

    // Update fields - Reladomo handles bitemporal chaining
    existingOrder.setCustomerId(entity.customerId!!)
    existingOrder.setOrderDate(Timestamp.from(entity.orderDate!!))
    existingOrder.setAmount(entity.amount!!)
    existingOrder.setStatus(entity.status!!)
    entity.description?.let { existingOrder.setDescription(it) }

    return OrderKt.fromReladomo(existingOrder)
  }

  public fun deleteById(id: Long, businessDate: Instant = Instant.now()) {
    // For bitemporal objects, use findByPrimaryKey with specific business date
    // Processing date should be infinity to get the current active record
    val businessDateTs = Timestamp.from(businessDate)
    val infinityTs = Timestamp.valueOf("9999-12-01 23:59:00.0")

    val order = OrderFinder.findByPrimaryKey(id, businessDateTs, infinityTs)
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.terminate()
  }

  public fun findAll(): List<OrderKt> {
    // For bitemporal queries, use the current time to get active records
    val currentTime = Timestamp.from(Instant.now())
    val operation = OrderFinder.businessDate().equalsEdgePoint()
        .and(OrderFinder.processingDate().equalsEdgePoint())

    val orders = OrderFinder.findMany(operation)
    return orders.map { OrderKt.fromReladomo(it) }
  }

  public fun findByCustomerId(customerId: Long): List<OrderKt> {
    val operation = OrderFinder.customerId().eq(customerId)
        .and(OrderFinder.businessDate().equalsEdgePoint())
        .and(OrderFinder.processingDate().equalsEdgePoint())

    val orders = OrderFinder.findMany(operation)
    return orders.map { OrderKt.fromReladomo(it) }
  }
}
