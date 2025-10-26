package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.Salary
import io.github.reladomokotlin.core.BiTemporalEntity
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class SalaryKt(
  public val id: Long?,
  public val employeeId: Long,
  public val amount: BigDecimal,
  public val currency: String,
  public val updatedBy: String?,
  override val businessDate: Instant,
  override val processingDate: Instant,
) : BiTemporalEntity {
  public fun toReladomo(): Salary {
    val obj = Salary(Timestamp.from(this.businessDate), Timestamp.from(this.processingDate))
    this.id?.let { obj.id = it }
    obj.employeeId = this.employeeId
    obj.amount = this.amount
    obj.currency = this.currency
    this.updatedBy?.let { obj.updatedBy = it }
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Salary): SalaryKt = SalaryKt(
      id = obj.id,
      employeeId = obj.employeeId,
      amount = obj.amount,
      currency = obj.currency,
      updatedBy = obj.updatedBy,
      businessDate = obj.businessDate.toInstant(),
      processingDate = obj.processingDate.toInstant()
    )
  }
}
