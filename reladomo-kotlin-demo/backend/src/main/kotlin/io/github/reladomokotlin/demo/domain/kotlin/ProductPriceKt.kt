package io.github.reladomokotlin.demo.domain.kotlin

import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.demo.domain.ProductPrice
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long

public data class ProductPriceKt(
  public val id: Long?,
  public val productId: Long,
  public val price: BigDecimal,
  override val businessDate: Instant,
  override val processingDate: Instant,
) : BiTemporalEntity {
  public fun toReladomo(): ProductPrice {
    val obj = ProductPrice(Timestamp.from(this.businessDate), Timestamp.from(this.processingDate))
    this.id?.let { obj.id = it }
    obj.productId = this.productId
    obj.price = this.price
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: ProductPrice): ProductPriceKt = ProductPriceKt(
      id = obj.id,
      productId = obj.productId,
      price = obj.price,
      businessDate = obj.businessDate.toInstant(),
      processingDate = obj.processingDate.toInstant()
    )
  }
}
