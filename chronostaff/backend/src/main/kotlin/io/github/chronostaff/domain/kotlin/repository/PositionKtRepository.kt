package io.github.chronostaff.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.chronostaff.domain.Position
import io.github.chronostaff.domain.PositionFinder
import io.github.chronostaff.domain.kotlin.PositionKt
import io.github.chronostaff.domain.kotlin.query.PositionQueryDsl
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
public class PositionKtRepository : BiTemporalRepository<PositionKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: PositionKt): PositionKt {
    val obj = Position(Timestamp.from(entity.businessDate))
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Position") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.companyId = entity.companyId
    obj.name = entity.name
    obj.level = entity.level
    entity.description?.let { obj.description = it }
    obj.insert()
    return PositionKt.fromReladomo(obj)
  }

  override fun findById(id: Long): PositionKt? {
    // For bitemporal objects, find active record (businessDate at infinity, processingDate at transaction time)
    val operation = PositionFinder.id().eq(id)
        .and(PositionFinder.businessDate().equalsInfinity())
        .and(PositionFinder.processingDate().equalsEdgePoint())
    val entity = PositionFinder.findOne(operation)
    return entity?.let { PositionKt.fromReladomo(it) }
  }

  override fun update(entity: PositionKt, businessDate: Instant): PositionKt {
    // For bitemporal objects, find record with infinity processing date at specified business date
    val operation = PositionFinder.id().eq(entity.id!!)
        .and(PositionFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(PositionFinder.processingDate().equalsInfinity())
    val existingEntity = PositionFinder.findOne(operation)
        ?: throw EntityNotFoundException("Position not found with id: ${entity.id}")

    // Update fields - Reladomo handles bitemporal chaining
    existingEntity.setCompanyId(entity.companyId)
    existingEntity.setName(entity.name)
    existingEntity.setLevel(entity.level)
    entity.description?.let { existingEntity.setDescription(it) }

    return PositionKt.fromReladomo(existingEntity)
  }

  override fun deleteById(id: Long) {
    deleteByIdAsOf(id, Instant.now())
  }

  override fun findAll(): List<PositionKt> {
    // For bitemporal queries, use equalsEdgePoint to get active records
    val operation = PositionFinder.businessDate().equalsEdgePoint()
        .and(PositionFinder.processingDate().equalsEdgePoint())

    val orders = PositionFinder.findMany(operation)
    return orders.map { PositionKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<PositionKt> {
    val orders = PositionFinder.findMany(operation)
    return orders.map { PositionKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      PositionFinder.findMany(operation).size.toLong()

  override fun delete(entity: PositionKt) {
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
  ): PositionKt? {
    // Find by primary key as of specific business and processing dates
    // Use operation-based query to handle infinity dates correctly
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = PositionFinder.id().eq(id)
        .and(PositionFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(if (processingDate.isAfter(infinityThreshold))
        PositionFinder.processingDate().equalsInfinity() else
        PositionFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = PositionFinder.findOne(operation)
    return entity?.let { PositionKt.fromReladomo(it) }
  }

  override fun update(entity: PositionKt): PositionKt = update(entity, Instant.now())

  override fun findAllAsOf(businessDate: Instant, processingDate: Instant): List<PositionKt> {
    val operation = PositionFinder.businessDate().eq(Timestamp.from(businessDate))
        .and(PositionFinder.processingDate().eq(Timestamp.from(processingDate)))

    val orders = PositionFinder.findMany(operation)
    return orders.map { PositionKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<PositionKt> {
    // Get all versions of the entity across time
    // For now, returns current version only. Full temporal history query requires
    // using MithraManager API or database-specific queries.
    val current = findById(id)
    return if (current != null) listOf(current) else emptyList()
  }

  override fun deleteByIdAsOf(id: Long, businessDate: Instant) {
    // For bitemporal objects, find record with infinity processing date at specified business date for termination
    val operation = PositionFinder.id().eq(id)
        .and(PositionFinder.businessDate().eq(Timestamp.from(businessDate)))
        .and(PositionFinder.processingDate().equalsInfinity())
    val entity = PositionFinder.findOne(operation)
        ?: throw EntityNotFoundException("Position not found with id: $id")
    entity.terminate()
  }

  public fun find(query: QueryContext.() -> Unit): List<PositionKt> {
    // Find entities using Query DSL
    // Use PositionQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = PositionFinder.findMany(operation)
    return results.map { PositionKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): PositionKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = PositionFinder.findOne(operation)
    return result?.let { PositionKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return PositionFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return PositionFinder.findOne(operation) != null
  }
}
