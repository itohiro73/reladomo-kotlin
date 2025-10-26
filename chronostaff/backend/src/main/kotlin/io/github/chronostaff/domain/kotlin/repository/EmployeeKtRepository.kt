package io.github.chronostaff.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.chronostaff.domain.Employee
import io.github.chronostaff.domain.EmployeeFinder
import io.github.chronostaff.domain.kotlin.EmployeeKt
import io.github.chronostaff.domain.kotlin.query.EmployeeQueryDsl
import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.core.ReladomoFinder
import io.github.reladomokotlin.core.ReladomoObject
import io.github.reladomokotlin.core.UniTemporalEntity
import io.github.reladomokotlin.core.UniTemporalRepository
import io.github.reladomokotlin.core.exceptions.EntityNotFoundException
import io.github.reladomokotlin.query.QueryContext
import io.github.reladomokotlin.query.query
import io.github.reladomokotlin.sequence.SequenceGenerator
import java.sql.Timestamp
import java.time.Instant
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.Unit
import kotlin.collections.List
import org.springframework.beans.factory.`annotation`.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.`annotation`.Transactional

@Repository
@Transactional
public class EmployeeKtRepository : UniTemporalRepository<EmployeeKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: EmployeeKt): EmployeeKt {
    val obj = Employee()
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Employee") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.employeeNumber = entity.employeeNumber
    obj.name = entity.name
    obj.email = entity.email
    obj.hireDate = Timestamp.from(entity.hireDate)
    obj.insert()
    return EmployeeKt.fromReladomo(obj)
  }

  override fun findById(id: Long): EmployeeKt? {
    val entity = EmployeeFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
    return entity?.let { EmployeeKt.fromReladomo(it) }
  }

  override fun update(entity: EmployeeKt): EmployeeKt {
    val existingOrder = EmployeeFinder.findByPrimaryKey(entity.id!!,  Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: ${entity.id}")

    // Update attributes
    existingOrder.setEmployeeNumber(entity.employeeNumber)
    existingOrder.setName(entity.name)
    existingOrder.setEmail(entity.email)
    existingOrder.setHireDate(Timestamp.from(entity.hireDate))

    return EmployeeKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = EmployeeFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<EmployeeKt> {
    // For unitemporal queries, use equalsInfinity to get current records (PROCESSING_THRU = infinity)
    val operation = EmployeeFinder.processingDate().equalsInfinity()
    val orders = EmployeeFinder.findMany(operation)
    return orders.map { EmployeeKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<EmployeeKt> {
    val orders = EmployeeFinder.findMany(operation)
    return orders.map { EmployeeKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      EmployeeFinder.findMany(operation).size.toLong()

  override fun delete(entity: EmployeeKt) {
    entity.id?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  override fun findByIdAsOf(id: Long, processingDate: Instant): EmployeeKt? {
    // Find by primary key as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = EmployeeFinder.id().eq(id)
        .and(if (processingDate.isAfter(infinityThreshold))
        EmployeeFinder.processingDate().equalsInfinity() else
        EmployeeFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = EmployeeFinder.findOne(operation)
    return entity?.let { EmployeeKt.fromReladomo(it) }
  }

  override fun findAllAsOf(processingDate: Instant): List<EmployeeKt> {
    // Find all entities as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = if (processingDate.isAfter(infinityThreshold)) {
        EmployeeFinder.processingDate().equalsInfinity()
    } else {
        EmployeeFinder.processingDate().eq(Timestamp.from(processingDate))
    }
    val entities = EmployeeFinder.findMany(operation)
    return entities.map { EmployeeKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<EmployeeKt> {
    // Get all versions of the entity across processing time
    // Use equalsEdgePoint() to retrieve ALL historical records
    // Returns history sorted by processing date (oldest first)
    val operation = EmployeeFinder.id().eq(id)
        .and(EmployeeFinder.processingDate().equalsEdgePoint())
    val entities = EmployeeFinder.findMany(operation)
    return entities.map { EmployeeKt.fromReladomo(it) }.sortedBy { it.processingDate }
  }

  public fun find(query: QueryContext.() -> Unit): List<EmployeeKt> {
    // Find entities using Query DSL
    // Use EmployeeQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = EmployeeFinder.findMany(operation)
    return results.map { EmployeeKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): EmployeeKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = EmployeeFinder.findOne(operation)
    return result?.let { EmployeeKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return EmployeeFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return EmployeeFinder.findOne(operation) != null
  }
}
