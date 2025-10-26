package io.github.chronostaff.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.chronostaff.domain.Company
import io.github.chronostaff.domain.CompanyFinder
import io.github.chronostaff.domain.kotlin.CompanyKt
import io.github.chronostaff.domain.kotlin.query.CompanyQueryDsl
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
public class CompanyKtRepository : UniTemporalRepository<CompanyKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: CompanyKt): CompanyKt {
    val obj = Company()
    val id = entity.id?.takeIf { it != 0L } ?: sequenceGenerator?.getNextId("Company") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.id = id
    obj.name = entity.name
    obj.insert()
    return CompanyKt.fromReladomo(obj)
  }

  override fun findById(id: Long): CompanyKt? {
    val entity = CompanyFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
    return entity?.let { CompanyKt.fromReladomo(it) }
  }

  override fun update(entity: CompanyKt): CompanyKt {
    val existingOrder = CompanyFinder.findByPrimaryKey(entity.id!!,  Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: ${entity.id}")

    // Update attributes
    existingOrder.setName(entity.name)

    return CompanyKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = CompanyFinder.findByPrimaryKey(id, Timestamp.from(Instant.now()))
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<CompanyKt> {
    // For unitemporal queries, use equalsInfinity to get current records (PROCESSING_THRU = infinity)
    val operation = CompanyFinder.processingDate().equalsInfinity()
    val orders = CompanyFinder.findMany(operation)
    return orders.map { CompanyKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<CompanyKt> {
    val orders = CompanyFinder.findMany(operation)
    return orders.map { CompanyKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long = CompanyFinder.findMany(operation).size.toLong()

  override fun delete(entity: CompanyKt) {
    entity.id?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  override fun findByIdAsOf(id: Long, processingDate: Instant): CompanyKt? {
    // Find by primary key as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = CompanyFinder.id().eq(id)
        .and(if (processingDate.isAfter(infinityThreshold))
        CompanyFinder.processingDate().equalsInfinity() else
        CompanyFinder.processingDate().eq(Timestamp.from(processingDate)))
    val entity = CompanyFinder.findOne(operation)
    return entity?.let { CompanyKt.fromReladomo(it) }
  }

  override fun findAllAsOf(processingDate: Instant): List<CompanyKt> {
    // Find all entities as of specific processing date
    val infinityThreshold = Instant.parse("9999-01-01T00:00:00Z")
    val operation = if (processingDate.isAfter(infinityThreshold)) {
        CompanyFinder.processingDate().equalsInfinity()
    } else {
        CompanyFinder.processingDate().eq(Timestamp.from(processingDate))
    }
    val entities = CompanyFinder.findMany(operation)
    return entities.map { CompanyKt.fromReladomo(it) }
  }

  override fun getHistory(id: Long): List<CompanyKt> {
    // Get all versions of the entity across processing time
    // Use equalsEdgePoint() to retrieve ALL historical records
    // Returns history sorted by processing date (oldest first)
    val operation = CompanyFinder.id().eq(id)
        .and(CompanyFinder.processingDate().equalsEdgePoint())
    val entities = CompanyFinder.findMany(operation)
    return entities.map { CompanyKt.fromReladomo(it) }.sortedBy { it.processingDate }
  }

  public fun find(query: QueryContext.() -> Unit): List<CompanyKt> {
    // Find entities using Query DSL
    // Use CompanyQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = CompanyFinder.findMany(operation)
    return results.map { CompanyKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): CompanyKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = CompanyFinder.findOne(operation)
    return result?.let { CompanyKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return CompanyFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return CompanyFinder.findOne(operation) != null
  }
}
