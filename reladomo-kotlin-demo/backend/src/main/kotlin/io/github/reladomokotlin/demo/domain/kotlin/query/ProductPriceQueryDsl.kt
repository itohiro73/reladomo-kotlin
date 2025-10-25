// Generated Query DSL extensions for ProductPrice
package io.github.reladomokotlin.demo.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
import com.gs.fw.common.mithra.attribute.LongAttribute
import io.github.reladomokotlin.demo.domain.ProductPriceFinder
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
import java.math.BigDecimal
import kotlin.Long

/**
 * Query DSL extensions for ProductPrice
 */
public object ProductPriceQueryDsl {
  public val QueryContext.id: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(ProductPriceFinder.id())

  public val QueryContext.productId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(ProductPriceFinder.productId())

  public val QueryContext.price: NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>
    get() = bigDecimalAttribute(ProductPriceFinder.price())

  public val QueryContext.businessDate: AsOfAttributeProperty
    get() = asOfAttribute(ProductPriceFinder.businessDate())

  public val QueryContext.processingDate: AsOfAttributeProperty
    get() = asOfAttribute(ProductPriceFinder.processingDate())
}
