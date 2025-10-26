package io.github.chronostaff.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.chronostaff.domain.EmployeeAssignment
import io.github.chronostaff.domain.EmployeeAssignmentFinder
import io.github.chronostaff.domain.kotlin.EmployeeAssignmentKt
import io.github.chronostaff.domain.kotlin.query.EmployeeAssignmentQueryDsl
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
public class EmployeeAssignmentKtRepository : BiTemporalRepository<EmployeeAssignmentKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: EmployeeAssignmentKt): EmployeeAssignmentKt {
    val obj = EmployeeAssignment(Timestamp.from(entity.businessDate))
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("EmployeeAssignment") ?:
        throw IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.employeeId = entity.employeeId
    obj.departmentId = entity.departmentId
    obj.positionId = entity.positionId
    entity.updatedBy?.let { obj.updatedBy = it }
    obj.insert()
    return EmployeeAssignmentKt.fromReladomo(obj)
  }

  override fun findById(id: Long): EmployeeAssignmentKt? {
    // For bitemporal objects, find active record (businessDate at infinity, processingDate at transaction time)
    val operation = EmployeeAssignmentFinder.id().eq(id)
        .and(EmployeeAssignmentFinder.businessDate().equalsInfinity())
        .and(EmployeeAssignmentFinder.processingDate().equalsEdgePoint())
    val entity = EmployeeAssignmentFinder.findOne(operation)
    return entity?.let { EmployeeAssignmentKt.fromReladomo(it) }
  }

  override fun update(entity: EmployeeAssignmentKt, businessDate: Instant): EmployeeAssignmentKt {
    // For bitemporal objects, find record with infinity processing date at specified business date
    val operation = EmployeeAssignmentFinder.id().eq(entity.id!!)
        .and(EmployeeAssignmentFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
    val existingEntity = EmployeeAssignmentFinder.findOne(operation)
        ?: throw EntityNotFoundException("EmployeeAssignment not found with id: ${entity.id}")

    // Update fields - Reladomo handles bitemporal chaining
    existingEntity.setEmployeeId(entity.employeeId)
    existingEntity.setDepartmentId(entity.departmentId)
    existingEntity.setPositionId(entity.positionId)
    entity.updatedBy?.let { existingEntity.setUpdatedBy(it) }

    return EmployeeAssignmentKt.fromReladomo(existingEntity)
  }

  override fun deleteById(id: Long) {
    deleteByIdAsOf(id, Instant.now())
  }

  override fun findAll(): List<EmployeeAssignmentKt> {
    // For bitemporal queries, use equalsEdgePoint to get active records
    val operation = EmployeeAssignmentFinder.businessDate().equalsEdgePoint()
        .and(EmployeeAssignmentFinder.processingDate().equalsEdgePoint())

    val orders = EmployeeAssignmentFinder.findMany(operation)
    return orders.map { EmployeeAssignmentKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<EmployeeAssignmentKt> {
    val orders = EmployeeAssignmentFinder.findMany(operation)
    return orders.map { EmployeeAssignmentKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      EmployeeAssignmentFinder.findMany(operation).size.toLong()

  override fun delete(entity: EmployeeAssignmentKt) {
    entity.id?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  override fun findByIdAsOf(
    id: Long,
    businessDate: Instant,
    processingDate: Instant,
  ): EmployeeAssignmentKt? {
    // Find by primary key as of specific business and processing dates
    // Use operation-based query to handle infinity dates correctly
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = EmployeeAssignmentFinder.id().eq(id)
        .and(EmployeeAssignmentFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(if (processingDate.isAfter(infinityThreshold))
        EmployeeAssignmentFinder.processingDate().equalsInfinity() else
        EmployeeAssignmentFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = EmployeeAssignmentFinder.findOne(operation)
    return entity?.let { EmployeeAssignmentKt.fromReladomo(it) }
  }

  override fun update(entity: EmployeeAssignmentKt): EmployeeAssignmentKt = update(entity,
      Instant.now())

  override fun findAllAsOf(businessDate: Instant, processingDate: Instant):
      List<EmployeeAssignmentKt> {
    val operation = EmployeeAssignmentFinder.businessDate().eq(Timestamp.from(businessDate))
        .and(EmployeeAssignmentFinder.processingDate().eq(Timestamp.from(processingDate)))

    val orders = EmployeeAssignmentFinder.findMany(operation)
    return orders.map { EmployeeAssignmentKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<EmployeeAssignmentKt> {
    // Get all versions of the entity across time
    // For now, returns current version only. Full temporal history query requires
    // using MithraManager API or database-specific queries.
    val current = findById(id)
    return if (current != null) listOf(current) else emptyList()
  }

  override fun deleteByIdAsOf(id: Long, businessDate: Instant) {
    // For bitemporal objects, find record with infinity processing date at specified business date for termination
    val operation = EmployeeAssignmentFinder.id().eq(id)
        .and(EmployeeAssignmentFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
    val entity = EmployeeAssignmentFinder.findOne(operation)
        ?: throw EntityNotFoundException("EmployeeAssignment not found with id: $id")
    entity.terminate()
  }

  public fun find(query: QueryContext.() -> Unit): List<EmployeeAssignmentKt> {
    // Find entities using Query DSL
    // Use EmployeeAssignmentQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = EmployeeAssignmentFinder.findMany(operation)
    return results.map { EmployeeAssignmentKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): EmployeeAssignmentKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = EmployeeAssignmentFinder.findOne(operation)
    return result?.let { EmployeeAssignmentKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return EmployeeAssignmentFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return EmployeeAssignmentFinder.findOne(operation) != null
  }
}
