package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.Employee
import io.github.reladomokotlin.core.UniTemporalEntity
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class EmployeeKt(
  public val id: Long?,
  public val companyId: Long,
  public val employeeNumber: String,
  public val name: String,
  public val email: String,
  public val hireDate: Instant,
  override val processingDate: Instant,
) : UniTemporalEntity {
  public fun toReladomo(): Employee {
    val obj = Employee()
    this.id?.let { obj.id = it }
    obj.companyId = this.companyId
    obj.employeeNumber = this.employeeNumber
    obj.name = this.name
    obj.email = this.email
    obj.hireDate = Timestamp.from(this.hireDate)
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Employee): EmployeeKt = EmployeeKt(
      id = obj.id,
      companyId = obj.companyId,
      employeeNumber = obj.employeeNumber,
      name = obj.name,
      email = obj.email,
      hireDate = obj.hireDate.toInstant(),
      processingDate = obj.processingDate.toInstant()
    )
  }
}
