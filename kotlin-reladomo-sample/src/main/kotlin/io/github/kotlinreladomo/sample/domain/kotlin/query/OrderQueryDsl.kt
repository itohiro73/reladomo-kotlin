// Generated Query DSL extensions for Order
package io.github.kotlinreladomo.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
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
import io.github.kotlinreladomo.sample.domain.OrderFinder
import java.math.BigDecimal
import java.sql.Timestamp
import kotlin.Long

/**
 * Query DSL extensions for Order
 */
public object OrderQueryDsl {
  public val QueryContext.orderId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(OrderFinder.orderId())

  public val QueryContext.customerId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(OrderFinder.customerId())

  public val QueryContext.orderDate: TemporalAttributeProperty<Timestamp, TimestampAttribute<*>>
    get() = timestampAttribute(OrderFinder.orderDate())

  public val QueryContext.amount: NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>
    get() = bigDecimalAttribute(OrderFinder.amount())

  public val QueryContext.status: StringAttributeProperty
    get() = stringAttribute(OrderFinder.status())

  public val QueryContext.description: StringAttributeProperty
    get() = stringAttribute(OrderFinder.description())

  public val QueryContext.businessDate: AsOfAttributeProperty
    get() = asOfAttribute(OrderFinder.businessDate())

  public val QueryContext.processingDate: AsOfAttributeProperty
    get() = asOfAttribute(OrderFinder.processingDate())
}
