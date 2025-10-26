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
public class PositionKtRepository : BaseRepository<PositionKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: PositionKt): PositionKt {
    val obj = Position()
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
    val entity = PositionFinder.findByPrimaryKey(id)
    return entity?.let { PositionKt.fromReladomo(it) }
  }

  override fun update(entity: PositionKt): PositionKt {
    val existingOrder = PositionFinder.findByPrimaryKey(entity.id!!)
        ?: throw EntityNotFoundException("Order not found with id: ${entity.id}")

    // Update attributes
    existingOrder.setCompanyId(entity.companyId)
    existingOrder.setName(entity.name)
    existingOrder.setLevel(entity.level)
    entity.description?.let { existingOrder.setDescription(it) }

    return PositionKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = PositionFinder.findByPrimaryKey(id)
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<PositionKt> {
    val orders = PositionFinder.findMany(PositionFinder.all())
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
