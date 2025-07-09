// Generated Query DSL extensions for Product
package io.github.reladomokotlin.sample.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
import com.gs.fw.common.mithra.attribute.IntegerAttribute
import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
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
import io.github.reladomokotlin.sample.domain.ProductFinder
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
