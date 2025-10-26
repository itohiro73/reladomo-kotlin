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
public class DepartmentKtRepository : BiTemporalRepository<DepartmentKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: DepartmentKt): DepartmentKt {
    val obj = Department(Timestamp.from(entity.businessDate))
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Department") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.companyId = entity.companyId
    obj.name = entity.name
    entity.description?.let { obj.description = it }
    entity.parentDepartmentId?.let { obj.parentDepartmentId = it }
    obj.insert()
    return DepartmentKt.fromReladomo(obj)
  }

  override fun findById(id: Long): DepartmentKt? {
    // For bitemporal objects, find active record (businessDate at infinity, processingDate at transaction time)
    val operation = DepartmentFinder.id().eq(id)
        .and(DepartmentFinder.businessDate().equalsInfinity())
        .and(DepartmentFinder.processingDate().equalsEdgePoint())
    val entity = DepartmentFinder.findOne(operation)
    return entity?.let { DepartmentKt.fromReladomo(it) }
  }

  override fun update(entity: DepartmentKt, businessDate: Instant): DepartmentKt {
    // For bitemporal objects, find record with infinity processing date at specified business date
    val operation = DepartmentFinder.id().eq(entity.id!!)
        .and(DepartmentFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(DepartmentFinder.processingDate().equalsInfinity())
    val existingEntity = DepartmentFinder.findOne(operation)
        ?: throw EntityNotFoundException("Department not found with id: ${entity.id}")

    // Update fields - Reladomo handles bitemporal chaining
    existingEntity.setCompanyId(entity.companyId)
    existingEntity.setName(entity.name)
    entity.description?.let { existingEntity.setDescription(it) }
    entity.parentDepartmentId?.let { existingEntity.setParentDepartmentId(it) }

    return DepartmentKt.fromReladomo(existingEntity)
  }

  override fun deleteById(id: Long) {
    deleteByIdAsOf(id, Instant.now())
  }

  override fun findAll(): List<DepartmentKt> {
    // For bitemporal queries, use equalsEdgePoint to get active records
    val operation = DepartmentFinder.businessDate().equalsEdgePoint()
        .and(DepartmentFinder.processingDate().equalsEdgePoint())

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

  override fun findByIdAsOf(
    id: Long,
    businessDate: Instant,
    processingDate: Instant,
  ): DepartmentKt? {
    // Find by primary key as of specific business and processing dates
    // Use operation-based query to handle infinity dates correctly
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = DepartmentFinder.id().eq(id)
        .and(DepartmentFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(if (processingDate.isAfter(infinityThreshold))
        DepartmentFinder.processingDate().equalsInfinity() else
        DepartmentFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = DepartmentFinder.findOne(operation)
    return entity?.let { DepartmentKt.fromReladomo(it) }
  }

  override fun update(entity: DepartmentKt): DepartmentKt = update(entity, Instant.now())

  override fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<DepartmentKt> {
    val operation = DepartmentFinder.businessDate().eq(Timestamp.from(businessDate))
        .and(DepartmentFinder.processingDate().eq(Timestamp.from(processingDate)))

    val orders = DepartmentFinder.findMany(operation)
    return orders.map { DepartmentKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<DepartmentKt> {
    // Get all versions of the entity across time
    // For now, returns current version only. Full temporal history query requires
    // using MithraManager API or database-specific queries.
    val current = findById(id)
    return if (current != null) listOf(current) else emptyList()
  }

  override fun deleteByIdAsOf(id: Long, businessDate: Instant) {
    // For bitemporal objects, find record with infinity processing date at specified business date for termination
    val operation = DepartmentFinder.id().eq(id)
        .and(DepartmentFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(DepartmentFinder.processingDate().equalsInfinity())
    val entity = DepartmentFinder.findOne(operation)
        ?: throw EntityNotFoundException("Department not found with id: $id")
    entity.terminate()
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
