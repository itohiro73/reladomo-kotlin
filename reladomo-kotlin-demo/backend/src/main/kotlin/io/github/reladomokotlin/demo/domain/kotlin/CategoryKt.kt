package io.github.reladomokotlin.demo.domain.kotlin

import io.github.reladomokotlin.demo.domain.Category
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class CategoryKt(
  public val id: Long?,
  public val name: String,
  public val description: String?,
) {
  public fun toReladomo(): Category {
    val obj = Category()
    this.id?.let { obj.id = it }
    obj.name = this.name
    this.description?.let { obj.description = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Category): CategoryKt = CategoryKt(
      id = obj.id,
      name = obj.name,
      description = obj.description
    )
  }
}
