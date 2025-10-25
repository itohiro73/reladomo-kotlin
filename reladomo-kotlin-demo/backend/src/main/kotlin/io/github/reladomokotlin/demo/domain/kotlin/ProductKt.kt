package io.github.reladomokotlin.demo.domain.kotlin

import io.github.reladomokotlin.demo.domain.Product
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class ProductKt(
  public val id: Long?,
  public val categoryId: Long,
  public val name: String,
  public val description: String?,
) {
  public fun toReladomo(): Product {
    val obj = Product()
    this.id?.let { obj.id = it }
    obj.categoryId = this.categoryId
    obj.name = this.name
    this.description?.let { obj.description = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Product): ProductKt = ProductKt(
      id = obj.id,
      categoryId = obj.categoryId,
      name = obj.name,
      description = obj.description
    )
  }
}
