package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.Department
import io.github.reladomokotlin.core.BiTemporalEntity
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class DepartmentKt(
  public val id: Long?,
  public val companyId: Long,
  public val name: String,
  public val description: String?,
  public val parentDepartmentId: Long?,
  override val businessDate: Instant,
  override val processingDate: Instant,
) : BiTemporalEntity {
  public fun toReladomo(): Department {
    val obj = Department(Timestamp.from(this.businessDate), Timestamp.from(this.processingDate))
    this.id?.let { obj.id = it }
    obj.companyId = this.companyId
    obj.name = this.name
    this.description?.let { obj.description = it }
    this.parentDepartmentId?.let { obj.parentDepartmentId = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Department): DepartmentKt = DepartmentKt(
      id = obj.id,
      companyId = obj.companyId,
      name = obj.name,
      description = obj.description,
      parentDepartmentId = obj.parentDepartmentId,
      businessDate = obj.businessDate.toInstant(),
      processingDate = obj.processingDate.toInstant()
    )
  }
}
