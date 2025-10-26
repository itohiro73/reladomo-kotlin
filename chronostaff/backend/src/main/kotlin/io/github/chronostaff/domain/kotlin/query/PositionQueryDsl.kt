// Generated Query DSL extensions for Position
package io.github.chronostaff.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.IntegerAttribute
import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
import io.github.chronostaff.domain.PositionFinder
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
import kotlin.Int
import kotlin.Long

/**
 * Query DSL extensions for Position
 */
public object PositionQueryDsl {
  public val QueryContext.id: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(PositionFinder.id())

  public val QueryContext.name: StringAttributeProperty
    get() = stringAttribute(PositionFinder.name())

  public val QueryContext.level: NumericAttributeProperty<Int, IntegerAttribute<*>>
    get() = intAttribute(PositionFinder.level())

  public val QueryContext.description: StringAttributeProperty
    get() = stringAttribute(PositionFinder.description())
}
