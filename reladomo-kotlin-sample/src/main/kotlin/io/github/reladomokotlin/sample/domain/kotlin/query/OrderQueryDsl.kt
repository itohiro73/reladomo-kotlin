// Generated Query DSL extensions for Order
package io.github.reladomokotlin.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
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
import io.github.reladomokotlin.sample.domain.OrderFinder
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
