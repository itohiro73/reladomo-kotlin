package io.github.kotlinreladomo.sample.domain.kotlin

import io.github.kotlinreladomo.sample.domain.Customer
import java.sql.Timestamp
import java.time.Instant
import kotlin.Long
import kotlin.String

public data class CustomerKt(
  public val customerId: Long?,
  public val name: String,
  public val email: String,
  public val phone: String?,
  public val address: String?,
  public val createdDate: Instant,
) {
  public fun toReladomo(): Customer {
    val obj = Customer()
    this.customerId?.let { obj.customerId = it }
    obj.name = this.name
    obj.email = this.email
    this.phone?.let { obj.phone = it }
    this.address?.let { obj.address = it }
    obj.createdDate = Timestamp.from(this.createdDate)
    return obj
  }

  public companion object {
    public fun fromReladomo(obj: Customer): CustomerKt = CustomerKt(
      customerId = obj.customerId,
      name = obj.name,
      email = obj.email,
      phone = obj.phone,
      address = obj.address,
      createdDate = obj.createdDate.toInstant()
    )
  }
}
