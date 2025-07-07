// Generated Query DSL extensions for Customer
package io.github.kotlinreladomo.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import io.github.kotlinreladomo.query.AsOfAttributeProperty
import io.github.kotlinreladomo.query.AttributeProperty
import io.github.kotlinreladomo.query.NumericAttributeProperty
import io.github.kotlinreladomo.query.QueryContext
import io.github.kotlinreladomo.query.StringAttributeProperty
import io.github.kotlinreladomo.query.TemporalAttributeProperty
import io.github.kotlinreladomo.query.asOfAttribute
import io.github.kotlinreladomo.query.attribute
import io.github.kotlinreladomo.query.bigDecimalAttribute
import io.github.kotlinreladomo.query.dateAttribute
import io.github.kotlinreladomo.query.doubleAttribute
import io.github.kotlinreladomo.query.floatAttribute
import io.github.kotlinreladomo.query.intAttribute
import io.github.kotlinreladomo.query.longAttribute
import io.github.kotlinreladomo.query.stringAttribute
import io.github.kotlinreladomo.query.timestampAttribute
import io.github.kotlinreladomo.sample.domain.CustomerFinder
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
