// Generated Query DSL extensions for OrderItem
package io.github.kotlinreladomo.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
import com.gs.fw.common.mithra.attribute.IntegerAttribute
import com.gs.fw.common.mithra.attribute.LongAttribute
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
import io.github.kotlinreladomo.sample.domain.OrderItemFinder
import java.math.BigDecimal
import kotlin.Int
import kotlin.Long

/**
 * Query DSL extensions for OrderItem
 */
public object OrderItemQueryDsl {
  public val QueryContext.orderItemId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(OrderItemFinder.orderItemId())

  public val QueryContext.orderId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(OrderItemFinder.orderId())

  public val QueryContext.productId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(OrderItemFinder.productId())

  public val QueryContext.quantity: NumericAttributeProperty<Int, IntegerAttribute<*>>
    get() = intAttribute(OrderItemFinder.quantity())

  public val QueryContext.unitPrice: NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>
    get() = bigDecimalAttribute(OrderItemFinder.unitPrice())

  public val QueryContext.totalPrice: NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>
    get() = bigDecimalAttribute(OrderItemFinder.totalPrice())
}
