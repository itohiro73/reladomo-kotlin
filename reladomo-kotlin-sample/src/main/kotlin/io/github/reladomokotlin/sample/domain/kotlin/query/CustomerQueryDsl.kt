// Generated Query DSL extensions for Customer
package io.github.reladomokotlin.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import io.github.reladomokotlin.query.AsOfAttributeProperty
import io.github.reladomokotlin.query.AttributeProperty
import io.github.reladomokotlin.query.NumericAttributeProperty
import io.github.reladomokotlin.query.QueryContext
import io.github.reladomokotlin.query.StringAttributeProperty
import io.github.reladomokotlin.query.TemporalAttributeProperty
import io.github.reladomokotlin.query.asOfAttribute
import io.github.reladomokotlin.query.attribute
import io.github.reladomokotlin.query.bigDecimalAttribute
import io.github.reladomokotlin.query.dateAttribute
import io.github.reladomokotlin.query.doubleAttribute
import io.github.reladomokotlin.query.floatAttribute
import io.github.reladomokotlin.query.intAttribute
import io.github.reladomokotlin.query.longAttribute
import io.github.reladomokotlin.query.stringAttribute
import io.github.reladomokotlin.query.timestampAttribute
import io.github.reladomokotlin.sample.domain.CustomerFinder
import java.sql.Timestamp
import kotlin.Long

/**
 * Query DSL extensions for Customer
 */
public object CustomerQueryDsl {
  public val QueryContext.customerId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(CustomerFinder.customerId())

  public val QueryContext.name: StringAttributeProperty
    get() = stringAttribute(CustomerFinder.name())

  public val QueryContext.email: StringAttributeProperty
    get() = stringAttribute(CustomerFinder.email())

  public val QueryContext.phone: StringAttributeProperty
    get() = stringAttribute(CustomerFinder.phone())

  public val QueryContext.address: StringAttributeProperty
    get() = stringAttribute(CustomerFinder.address())

  public val QueryContext.createdDate: TemporalAttributeProperty<Timestamp, TimestampAttribute<*>>
    get() = timestampAttribute(CustomerFinder.createdDate())
}
