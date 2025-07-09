package io.github.reladomokotlin.sample.domain.kotlin.repository

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.attribute.TimestampAttribute
import com.gs.fw.common.mithra.finder.Operation
import io.github.reladomokotlin.core.BaseRepository
import io.github.reladomokotlin.core.BiTemporalEntity
import io.github.reladomokotlin.core.BiTemporalRepository
import io.github.reladomokotlin.core.ReladomoFinder
import io.github.reladomokotlin.core.ReladomoObject
import io.github.reladomokotlin.core.exceptions.EntityNotFoundException
import io.github.reladomokotlin.query.QueryContext
import io.github.reladomokotlin.query.query
import io.github.reladomokotlin.sample.domain.Customer
import io.github.reladomokotlin.sample.domain.CustomerFinder
import io.github.reladomokotlin.sample.domain.kotlin.CustomerKt
import io.github.reladomokotlin.sample.domain.kotlin.query.CustomerQueryDsl
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
public class CustomerKtRepository : BaseRepository<CustomerKt, Long> {
  @Autowired(required = false)
  private var sequenceGenerator: SequenceGenerator? = null

  override fun save(entity: CustomerKt): CustomerKt {
    val obj = Customer()
    val customerId = entity.customerId?.takeIf { it != 0L } ?:
        sequenceGenerator?.getNextId("Customer") ?: throw
        IllegalStateException("No ID provided and sequence generator not available")
    obj.customerId = customerId
    obj.name = entity.name
    obj.email = entity.email
    entity.phone?.let { obj.phone = it }
    entity.address?.let { obj.address = it }
    obj.createdDate = Timestamp.from(entity.createdDate)
    obj.insert()
    return CustomerKt.fromReladomo(obj)
  }

  override fun findById(id: Long): CustomerKt? {
    val order = CustomerFinder.findByPrimaryKey(id)
    return order?.let { CustomerKt.fromReladomo(it) }
  }

  override fun update(entity: CustomerKt): CustomerKt {
    val existingOrder = CustomerFinder.findByPrimaryKey(entity.customerId!!)
        ?: throw EntityNotFoundException("Order not found with id: ${entity.customerId}")

    // Update attributes
    existingOrder.setName(entity.name)
    existingOrder.setEmail(entity.email)
    entity.phone?.let { existingOrder.setPhone(it) }
    entity.address?.let { existingOrder.setAddress(it) }
    existingOrder.setCreatedDate(Timestamp.from(entity.createdDate))

    return CustomerKt.fromReladomo(existingOrder)
  }

  override fun deleteById(id: Long) {
    val order = CustomerFinder.findByPrimaryKey(id)
        ?: throw EntityNotFoundException("Order not found with id: $id")
    order.delete()
  }

  override fun findAll(): List<CustomerKt> {
    val orders = CustomerFinder.findMany(CustomerFinder.all())
    return orders.map { CustomerKt.fromReladomo(it) }
  }

  override fun findBy(operation: Operation): List<CustomerKt> {
    val orders = CustomerFinder.findMany(operation)
    return orders.map { CustomerKt.fromReladomo(it) }
  }

  override fun countBy(operation: Operation): Long =
      CustomerFinder.findMany(operation).size.toLong()

  override fun delete(entity: CustomerKt) {
    entity.customerId?.let { deleteById(it) }
  }

  override fun deleteAll() {
    val allOrders = findAll()
    allOrders.forEach { delete(it) }
  }

  override fun count(): Long = findAll().size.toLong()

  public fun find(query: QueryContext.() -> Unit): List<CustomerKt> {
    // Find entities using Query DSL
    // Use CustomerQueryDsl extensions to access attribute properties
    val operation = io.github.reladomokotlin.query.query(query)
    val results = CustomerFinder.findMany(operation)
    return results.map { CustomerKt.fromReladomo(it) }
  }

  public fun findOne(query: QueryContext.() -> Unit): CustomerKt? {
    // Find a single entity using Query DSL
    val operation = io.github.reladomokotlin.query.query(query)
    val result = CustomerFinder.findOne(operation)
    return result?.let { CustomerKt.fromReladomo(it) }
  }

  public fun count(query: QueryContext.() -> Unit): Int {
    // Count entities matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return CustomerFinder.findMany(operation).size
  }

  public fun exists(query: QueryContext.() -> Unit): Boolean {
    // Check if any entity exists matching Query DSL criteria
    val operation = io.github.reladomokotlin.query.query(query)
    return CustomerFinder.findOne(operation) != null
  }
}
