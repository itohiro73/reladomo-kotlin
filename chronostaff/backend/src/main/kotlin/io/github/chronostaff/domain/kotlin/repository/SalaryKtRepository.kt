package io.github.chronostaff.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.chronostaff.domain.Salary
import io.github.chronostaff.domain.SalaryFinder
import io.github.chronostaff.domain.kotlin.SalaryKt
import io.github.chronostaff.domain.kotlin.query.SalaryQueryDsl
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
public class SalaryKtRepository : BiTemporalRepository<SalaryKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: SalaryKt): SalaryKt {
    val obj = Salary(Timestamp.from(entity.businessDate))
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Salary") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.employeeId = entity.employeeId
    obj.amount = entity.amount
    obj.currency = entity.currency
    entity.updatedBy?.let { obj.updatedBy = it }
    obj.insert()
    return SalaryKt.fromReladomo(obj)
  }

  override fun findById(id: Long): SalaryKt? {
    // For bitemporal objects, find active record (businessDate at infinity, processingDate at transaction time)
    val operation = SalaryFinder.id().eq(id)
        .and(SalaryFinder.businessDate().equalsInfinity())
        .and(SalaryFinder.processingDate().equalsEdgePoint())
    val entity = SalaryFinder.findOne(operation)
    return entity?.let { SalaryKt.fromReladomo(it) }
  }

  override fun update(entity: SalaryKt, businessDate: Instant): SalaryKt {
    // For bitemporal objects, find record with infinity processing date at specified business date
    val operation = SalaryFinder.id().eq(entity.id!!)
        .and(SalaryFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(SalaryFinder.processingDate().equalsInfinity())
    val existingEntity = SalaryFinder.findOne(operation)
        ?: throw EntityNotFoundException("Salary not found with id: ${entity.id}")

    // Update fields - Reladomo handles bitemporal chaining
    existingEntity.setEmployeeId(entity.employeeId)
    existingEntity.setAmount(entity.amount)
    existingEntity.setCurrency(entity.currency)
    entity.updatedBy?.let { existingEntity.setUpdatedBy(it) }

    return SalaryKt.fromReladomo(existingEntity)
  }

  override fun deleteById(id: Long) {
    deleteByIdAsOf(id, Instant.now())
  }

  override fun findAll(): List<SalaryKt> {
    // For bitemporal queries, use equalsEdgePoint to get active records
    val operation = SalaryFinder.businessDate().equalsEdgePoint()
        .and(SalaryFinder.processingDate().equalsEdgePoint())

    val orders = SalaryFinder.findMany(operation)
    return orders.map { SalaryKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<SalaryKt> {
    val orders = SalaryFinder.findMany(operation)
    return orders.map { SalaryKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long = SalaryFinder.findMany(operation).size.toLong()

  override fun delete(entity: SalaryKt) {
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
  ): SalaryKt? {
    // Find by primary key as of specific business and processing dates
    // Use operation-based query to handle infinity dates correctly
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = SalaryFinder.id().eq(id)
        .and(SalaryFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(if (processingDate.isAfter(infinityThreshold))
        SalaryFinder.processingDate().equalsInfinity() else
        SalaryFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = SalaryFinder.findOne(operation)
    return entity?.let { SalaryKt.fromReladomo(it) }
  }

  override fun update(entity: SalaryKt): SalaryKt = update(entity, Instant.now())

  override fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<SalaryKt> {
    val operation = SalaryFinder.businessDate().eq(Timestamp.from(businessDate))
        .and(SalaryFinder.processingDate().eq(Timestamp.from(processingDate)))

    val orders = SalaryFinder.findMany(operation)
    return orders.map { SalaryKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<SalaryKt> {
    // Get all versions of the entity across time
    // For now, returns current version only. Full temporal history query requires
    // using MithraManager API or database-specific queries.
    val current = findById(id)
    return if (current != null) listOf(current) else emptyList()
  }

  override fun deleteByIdAsOf(id: Long, businessDate: Instant) {
    // For bitemporal objects, find record with infinity processing date at specified business date for termination
    val operation = SalaryFinder.id().eq(id)
        .and(SalaryFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(SalaryFinder.processingDate().equalsInfinity())
    val entity = SalaryFinder.findOne(operation)
        ?: throw EntityNotFoundException("Salary not found with id: $id")
    entity.terminate()
  }

  public fun find(query: QueryContext.() -> Unit): List<SalaryKt> {
    // Find entities using Query DSL
    // Use SalaryQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = SalaryFinder.findMany(operation)
    return results.map { SalaryKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): SalaryKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = SalaryFinder.findOne(operation)
    return result?.let { SalaryKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return SalaryFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return SalaryFinder.findOne(operation) != null
  }
}
