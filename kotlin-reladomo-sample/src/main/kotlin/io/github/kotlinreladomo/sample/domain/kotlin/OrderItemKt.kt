package io.github.kotlinreladomo.sample.domain.kotlin

import io.github.kotlinreladomo.sample.domain.OrderItem
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import kotlin.Int
import kotlin.Long

public data class OrderItemKt(
  public val orderItemId: Long?,
  public val orderId: Long,
  public val productId: Long,
  public val quantity: Int,
  public val unitPrice: BigDecimal,
  public val totalPrice: BigDecimal,
) {
  public fun toReladomo(): OrderItem {
    val obj = OrderItem()
    this.orderItemId?.let { obj.orderItemId = it }
    obj.orderId = this.orderId
    obj.productId = this.productId
    obj.quantity = this.quantity
    obj.unitPrice = this.unitPrice
    obj.totalPrice = this.totalPrice
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: OrderItem): OrderItemKt = OrderItemKt(
      orderItemId = obj.orderItemId,
      orderId = obj.orderId,
      productId = obj.productId,
      quantity = obj.quantity,
      unitPrice = obj.unitPrice,
      totalPrice = obj.totalPrice
    )
  }
}
