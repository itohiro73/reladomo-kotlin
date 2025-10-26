package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.Department
import io.github.reladomokotlin.core.UniTemporalEntity
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
  override val processingDate: Instant,
) : UniTemporalEntity {
  public fun toReladomo(): Department {
    val obj = Department()
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
      processingDate = obj.processingDate.toInstant()
    )
  }
}
