package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.Position
import java.sql.Timestamp
import java.time.Instant
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class PositionKt(
  public val id: Long?,
  public val name: String,
  public val level: Int,
  public val description: String?,
) {
  public fun toReladomo(): Position {
    val obj = Position()
    this.id?.let { obj.id = it }
    obj.name = this.name
    obj.level = this.level
    this.description?.let { obj.description = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Position): PositionKt = PositionKt(
      id = obj.id,
      name = obj.name,
      level = obj.level,
      description = obj.description
    )
  }
}
