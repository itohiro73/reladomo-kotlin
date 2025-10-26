// Generated Query DSL extensions for Employee
package io.github.chronostaff.domain.kotlin.query

import com.gs.fw.common.mithra.attribute.LongAttribute
import com.gs.fw.common.mithra.attribute.StringAttribute
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import io.github.chronostaff.domain.EmployeeFinder
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
import java.sql.Timestamp
import kotlin.Long

/**
 * Query DSL extensions for Employee
 */
public object EmployeeQueryDsl {
  public val QueryContext.id: NumericAttributeProperty<Long, LongAttribute<*>>
    get() = longAttribute(EmployeeFinder.id())

  public val QueryContext.employeeNumber: StringAttributeProperty
    get() = stringAttribute(EmployeeFinder.employeeNumber())

  public val QueryContext.name: StringAttributeProperty
    get() = stringAttribute(EmployeeFinder.name())

  public val QueryContext.email: StringAttributeProperty
    get() = stringAttribute(EmployeeFinder.email())

  public val QueryContext.hireDate: TemporalAttributeProperty<Timestamp, TimestampAttribute<*>>
    get() = timestampAttribute(EmployeeFinder.hireDate())
}
