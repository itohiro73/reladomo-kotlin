package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.Position
import io.github.reladomokotlin.core.BiTemporalEntity
import java.sql.Timestamp
import java.time.Instant
import kotlin.Int
import kotlin.Long
import kotlin.String

public data class PositionKt(
  public val id: Long?,
  public val companyId: Long,
  public val name: String,
  public val level: Int,
  public val description: String?,
  override val businessDate: Instant,
  override val processingDate: Instant,
) : BiTemporalEntity {
  public fun toReladomo(): Position {
    val obj = Position(Timestamp.from(this.businessDate), Timestamp.from(this.processingDate))
    this.id?.let { obj.id = it }
    obj.companyId = this.companyId
    obj.name = this.name
    obj.level = this.level
    this.description?.let { obj.description = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Position): PositionKt = PositionKt(
      id = obj.id,
      companyId = obj.companyId,
      name = obj.name,
      level = obj.level,
      description = obj.description,
      businessDate = obj.businessDate.toInstant(),
      processingDate = obj.processingDate.toInstant()
    )
  }
}
