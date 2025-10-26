// Generated Query DSL extensions for Department
package io.github.chronostaff.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
import io.github.chronostaff.domain.DepartmentFinder
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
import kotlin.Long

/**
 * Query DSL extensions for Department
 */
public object DepartmentQueryDsl {
  public val QueryContext.id: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(DepartmentFinder.id())

  public val QueryContext.companyId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(DepartmentFinder.companyId())

  public val QueryContext.name: StringAttributeProperty
    get() = stringAttribute(DepartmentFinder.name())

  public val QueryContext.description: StringAttributeProperty
    get() = stringAttribute(DepartmentFinder.description())

  public val QueryContext.parentDepartmentId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(DepartmentFinder.parentDepartmentId())

  public val QueryContext.businessDate: AsOfAttributeProperty
    get() = asOfAttribute(DepartmentFinder.businessDate())

  public val QueryContext.processingDate: AsOfAttributeProperty
    get() = asOfAttribute(DepartmentFinder.processingDate())
}
