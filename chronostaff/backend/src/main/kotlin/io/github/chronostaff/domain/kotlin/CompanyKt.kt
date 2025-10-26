package io.github.chronostaff.domain.kotlin

import io.github.chronostaff.domain.Company
import io.github.reladomokotlin.core.UniTemporalEntity
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class CompanyKt(
  public val id: Long?,
  public val name: String,
  override val processingDate: Instant,
) : UniTemporalEntity {
  public fun toReladomo(): Company {
    val obj = Company()
    this.id?.let { obj.id = it }
    obj.name = this.name
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Company): CompanyKt = CompanyKt(
      id = obj.id,
      name = obj.name,
      processingDate = obj.processingDate.toInstant()
    )
  }
}
