// Generated Query DSL extensions for Salary
package io.github.chronostaff.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.BigDecimalAttribute
import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
import io.github.chronostaff.domain.SalaryFinder
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
 * Query DSL extensions for Salary
 */
public object SalaryQueryDsl {
  public val QueryContext.id: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(SalaryFinder.id())

  public val QueryContext.employeeId: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(SalaryFinder.employeeId())

  public val QueryContext.amount: NumericAttributeProperty<BigDecimal, BigDecimalAttribute<*>>
    get() = bigDecimalAttribute(SalaryFinder.amount())

  public val QueryContext.currency: StringAttributeProperty
    get() = stringAttribute(SalaryFinder.currency())

  public val QueryContext.updatedBy: StringAttributeProperty
    get() = stringAttribute(SalaryFinder.updatedBy())

  public val QueryContext.businessDate: AsOfAttributeProperty
    get() = asOfAttribute(SalaryFinder.businessDate())

  public val QueryContext.processingDate: AsOfAttributeProperty
    get() = asOfAttribute(SalaryFinder.processingDate())
}
