package io.github.kotlinreladomo.sample.domain.kotlin

import io.github.kotlinreladomo.sample.domain.Product
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class ProductKt(
  public val productId: Long?,
  public val name: String,
  public val description: String?,
  public val price: BigDecimal,
  public val stockQuantity: Int,
  public val category: String?,
) {
  public fun toReladomo(): Product {
    val obj = Product()
    this.productId?.let { obj.productId = it }
    obj.name = this.name
    this.description?.let { obj.description = it }
    obj.price = this.price
    obj.stockQuantity = this.stockQuantity
    this.category?.let { obj.category = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Product): ProductKt = ProductKt(
      productId = obj.productId,
      name = obj.name,
      description = obj.description,
      price = obj.price,
      stockQuantity = obj.stockQuantity,
      category = obj.category
    )
  }
}
