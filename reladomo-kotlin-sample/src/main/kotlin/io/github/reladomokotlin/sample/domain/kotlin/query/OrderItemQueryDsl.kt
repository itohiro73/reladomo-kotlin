// Generated Query DSL extensions for OrderItem
package io.github.reladomokotlin.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
import com.gs.fw.common.mithra.attribute.IntegerAttribute
import com.gs.fw.common.mithra.attribute.LongAttribute
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
import io.github.reladomokotlin.sample.domain.OrderItemFinder
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
