package io.github.reladomokotlin.sample.service

import io.github.reladomokotlin.core.exceptions.EntityNotFoundException
import io.github.reladomokotlin.sample.dto.CreateOrderRequest
import io.github.reladomokotlin.sample.dto.OrderDto
import io.github.reladomokotlin.sample.domain.kotlin.OrderKt
import io.github.reladomokotlin.sample.repository.OrderSpringDataRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderSpringDataRepository
) {
    
    // Simple ID generator for demo purposes
    private val idGenerator = AtomicLong(1000)
    
    fun findAllOrders(): List<OrderDto> {
        return orderRepository.findAll().map { it.toDto() }
    }
    
    fun findOrderById(id: Long): OrderDto {
        val order = orderRepository.findById(id)
            ?: throw EntityNotFoundException("Order not found with id: $id")
        return order.toDto()
    }
    
    fun findOrderAsOf(id: Long, businessDate: Instant, processingDate: Instant): OrderDto {
        val order = orderRepository.findByIdAsOf(id, businessDate, processingDate)
            ?: throw EntityNotFoundException("Order not found with id: $id at specified time")
        return order.toDto()
    }
    
    fun createOrder(request: CreateOrderRequest): OrderDto {
        val now = Instant.now()
        // For bitemporal objects, processing date should be set to infinity for new records
        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        val order = OrderKt(
            orderId = idGenerator.incrementAndGet(),
            customerId = request.customerId,
            orderDate = now,
            amount = request.amount,
            status = request.status,
            description = request.description,
            businessDate = now,
            processingDate = infinityDate
        )
        
        val savedOrder = orderRepository.save(order)
        return savedOrder.toDto()
    }
    
    fun updateOrder(id: Long, request: CreateOrderRequest): OrderDto {
        val existingOrder = orderRepository.findById(id)
            ?: throw EntityNotFoundException("Order not found with id: $id")

        val infinityDate = Instant.parse("9999-12-01T23:59:00Z")
        val now = Instant.now()
        val updatedOrder = existingOrder.copy(
            customerId = request.customerId,
            amount = request.amount,
            status = request.status,
            description = request.description,
            businessDate = now, // Use current time for business date
            processingDate = infinityDate
        )

        // Update with current business date
        val savedOrder = orderRepository.update(updatedOrder, now)
        return savedOrder.toDto()
    }
    
    fun deleteOrder(id: Long) {
        val existingOrder = orderRepository.findById(id)
            ?: throw EntityNotFoundException("Order not found with id: $id")
        // Delete with current business date
        orderRepository.deleteByIdAsOf(id, Instant.now())
    }
    
    fun findOrdersByCustomer(customerId: Long): List<OrderDto> {
        return orderRepository.findByCustomerId(customerId).map { it.toDto() }
    }
    
    fun findOrdersByStatus(status: String): List<OrderDto> {
        return orderRepository.findByStatus(status).map { it.toDto() }
    }
    
    fun findHighValueOrders(minAmount: java.math.BigDecimal): List<OrderDto> {
        return orderRepository.findByAmountGreaterThan(minAmount).map { it.toDto() }
    }
    
    fun orderExistsForCustomer(customerId: Long): Boolean {
        return orderRepository.existsByCustomerId(customerId)
    }
    
    fun countOrdersByStatus(status: String): Long {
        return orderRepository.countByStatus(status)
    }
    
    private fun OrderKt.toDto(): OrderDto {
        return OrderDto(
            orderId = this.orderId ?: throw IllegalStateException("Order ID cannot be null"),
            customerId = this.customerId,
            orderDate = this.orderDate,
            amount = this.amount,
            status = this.status,
            description = this.description,
            businessDate = this.businessDate,
            processingDate = this.processingDate
        )
    }
}