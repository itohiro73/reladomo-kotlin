package io.github.chronostaff.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.chronostaff.domain.Department
import io.github.chronostaff.domain.DepartmentFinder
import io.github.chronostaff.domain.kotlin.DepartmentKt
import io.github.chronostaff.domain.kotlin.query.DepartmentQueryDsl
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
public class DepartmentKtRepository : UniTemporalRepository<DepartmentKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: DepartmentKt): DepartmentKt {
    val obj = Department()
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Department") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.name = entity.name
    entity.description?.let { obj.description = it }
    entity.parentDepartmentId?.let { obj.parentDepartmentId = it }
    obj.insert()
    return DepartmentKt.fromReladomo(obj)
  }

  override fun findById(id: Long): DepartmentKt? {
    val entity = DepartmentFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
    return entity?.let { DepartmentKt.fromReladomo(it) }
  }

  override fun update(entity: DepartmentKt): DepartmentKt {
    val existingOrder = DepartmentFinder.findByPrimaryKey(entity.id!!, 
        Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: ${entity.id}")

    // Update attributes
    existingOrder.setName(entity.name)
    entity.description?.let { existingOrder.setDescription(it) }
    entity.parentDepartmentId?.let { existingOrder.setParentDepartmentId(it) }

    return DepartmentKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = DepartmentFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<DepartmentKt> {
    // For unitemporal queries, use equalsInfinity to get current records (PROCESSING_THRU = infinity)
    val operation = DepartmentFinder.processingDate().equalsInfinity()
    val orders = DepartmentFinder.findMany(operation)
    return orders.map { DepartmentKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<DepartmentKt> {
    val orders = DepartmentFinder.findMany(operation)
    return orders.map { DepartmentKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      DepartmentFinder.findMany(operation).size.toLong()

  override fun delete(entity: DepartmentKt) {
    entity.id?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  override fun findByIdAsOf(id: Long, processingDate: Instant): DepartmentKt? {
    // Find by primary key as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = DepartmentFinder.id().eq(id)
        .and(if (processingDate.isAfter(infinityThreshold))
        DepartmentFinder.processingDate().equalsInfinity() else
        DepartmentFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = DepartmentFinder.findOne(operation)
    return entity?.let { DepartmentKt.fromReladomo(it) }
  }

  override fun findAllAsOf(processingDate: Instant): List<DepartmentKt> {
    // Find all entities as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = if (processingDate.isAfter(infinityThreshold)) {
        DepartmentFinder.processingDate().equalsInfinity()
    } else {
        DepartmentFinder.processingDate().eq(Timestamp.from(processingDate))
    }
    val entities = DepartmentFinder.findMany(operation)
    return entities.map { DepartmentKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<DepartmentKt> {
    // Get all versions of the entity across processing time
    // Use equalsEdgePoint() to retrieve ALL historical records
    // Returns history sorted by processing date (oldest first)
    val operation = DepartmentFinder.id().eq(id)
        .and(DepartmentFinder.processingDate().equalsEdgePoint())
    val entities = DepartmentFinder.findMany(operation)
    return entities.map { DepartmentKt.fromReladomo(it) }.sortedBy { it.processingDate }
  }

  public fun find(query: QueryContext.() -> Unit): List<DepartmentKt> {
    // Find entities using Query DSL
    // Use DepartmentQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = DepartmentFinder.findMany(operation)
    return results.map { DepartmentKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): DepartmentKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = DepartmentFinder.findOne(operation)
    return result?.let { DepartmentKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return DepartmentFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return DepartmentFinder.findOne(operation) != null
  }
}
