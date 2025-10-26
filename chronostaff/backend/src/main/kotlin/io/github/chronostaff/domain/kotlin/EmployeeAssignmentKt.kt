package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.EmployeeAssignment
import io.github.reladomokotlin.core.BiTemporalEntity
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class EmployeeAssignmentKt(
  public val id: Long?,
  public val employeeId: Long,
  public val departmentId: Long,
  public val positionId: Long,
  public val updatedBy: String?,
  override val businessDate: Instant,
  override val processingDate: Instant,
) : BiTemporalEntity {
  public fun toReladomo(): EmployeeAssignment {
    val obj = EmployeeAssignment(Timestamp.from(this.businessDate),
        Timestamp.from(this.processingDate))
    this.id?.let { obj.id = it }
    obj.employeeId = this.employeeId
    obj.departmentId = this.departmentId
    obj.positionId = this.positionId
    this.updatedBy?.let { obj.updatedBy = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: EmployeeAssignment): EmployeeAssignmentKt = EmployeeAssignmentKt(
      id = obj.id,
      employeeId = obj.employeeId,
      departmentId = obj.departmentId,
      positionId = obj.positionId,
      updatedBy = obj.updatedBy,
      businessDate = obj.businessDate.toInstant(),
      processingDate = obj.processingDate.toInstant()
    )
  }
}
