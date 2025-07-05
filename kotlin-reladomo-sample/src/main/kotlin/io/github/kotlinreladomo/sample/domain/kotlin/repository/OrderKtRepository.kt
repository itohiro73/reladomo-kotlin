package io.github.kotlinreladomo.sample.domain.kotlin.repository

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

@Repository
public class OrderKtRepository {
  public fun save(entity: OrderKt): OrderKt {
    val reladomoObject = entity.toReladomo()
    reladomoObject.insert()
    return OrderKt.fromReladomo(reladomoObject)
  }

  public fun findById(id: Long): OrderKt? {
    val now = Instant.now()
    val order = OrderFinder.findByPrimaryKey(id, Timestamp.from(now), Timestamp.from(now))
    return order?.let { OrderKt.fromReladomo(it) }
  }

  public fun findByIdAsOf(
    id: Long,
    businessDate: Instant,
    processingDate: Instant,
  ): OrderKt? {
    val order = OrderFinder.findByPrimaryKey(id, Timestamp.from(businessDate),
        Timestamp.from(processingDate))
    return order?.let { OrderKt.fromReladomo(it) }
  }

  public fun update(entity: OrderKt): OrderKt {
    val now = Instant.now()
    val existingOrder = OrderFinder.findByPrimaryKey(entity.orderId!!, Timestamp.from(now),
        Timestamp.from(now))
        ?: throw EntityNotFoundException("Order not found with id: ${entity.orderId}")

    // For bitemporal update, terminate the old and insert new
    existingOrder.terminate()
    val newOrder = entity.toReladomo()
    newOrder.insert()
    return OrderKt.fromReladomo(newOrder)
  }

  public fun deleteById(id: Long) {
    val now = Instant.now()
    val order = OrderFinder.findByPrimaryKey(id, Timestamp.from(now), Timestamp.from(now))
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
