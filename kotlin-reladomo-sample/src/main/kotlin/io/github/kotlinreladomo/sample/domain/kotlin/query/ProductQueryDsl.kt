// Generated Query DSL extensions for Product
package io.github.kotlinreladomo.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
import com.gs.fw.common.mithra.attribute.IntegerAttribute
import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
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
import io.github.kotlinreladomo.sample.domain.ProductFinder
import java.math.BigDecimal
import kotlin.Int
import kotlin.Long

/**
 * Query DSL extensions for Product
 */
public object ProductQueryDsl {
  public val QueryContext.productId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(ProductFinder.productId())

  public val QueryContext.name: StringAttributeProperty
    get() = stringAttribute(ProductFinder.name())

  public val QueryContext.description: StringAttributeProperty
    get() = stringAttribute(ProductFinder.description())

  public val QueryContext.price: NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>
    get() = bigDecimalAttribute(ProductFinder.price())

  public val QueryContext.stockQuantity: NumericAttributeProperty<Int, IntegerAttribute<*>>
    get() = intAttribute(ProductFinder.stockQuantity())

  public val QueryContext.category: StringAttributeProperty
    get() = stringAttribute(ProductFinder.category())
}
