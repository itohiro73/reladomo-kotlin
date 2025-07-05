package io.github.kotlinreladomo.sample.domain.kotlin

import io.github.kotlinreladomo.core.BiTemporalEntity
import io.github.kotlinreladomo.sample.domain.Order
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class OrderKt(
  public val orderId: Long?,
  public val customerId: Long,
  public val orderDate: Instant,
  public val amount: BigDecimal,
  public val status: String,
  public val description: String?,
  override val businessDate: Instant,
  override val processingDate: Instant,
) : BiTemporalEntity {
  public fun toReladomo(): Order {
    val obj = Order(Timestamp.from(this.businessDate), Timestamp.from(this.processingDate))
    this.orderId?.let { obj.orderId = it }
    obj.customerId = this.customerId
    obj.orderDate = Timestamp.from(this.orderDate)
    obj.amount = this.amount
    obj.status = this.status
    this.description?.let { obj.description = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Order): OrderKt = OrderKt(
      orderId = obj.orderId,
      customerId = obj.customerId,
      orderDate = obj.orderDate.toInstant(),
      amount = obj.amount,
      status = obj.status,
      description = obj.description,
      businessDate = obj.businessDate.toInstant(),
      processingDate = obj.processingDate.toInstant()
    )
  }
}
