package io.github.kotlinreladomo.sample.service

import io.github.kotlinreladomo.core.exceptions.EntityNotFoundException
import io.github.kotlinreladomo.sample.dto.CreateOrderRequest
import io.github.kotlinreladomo.sample.dto.OrderDto
import io.github.kotlinreladomo.sample.domain.kotlin.OrderKt
import io.github.kotlinreladomo.sample.domain.kotlin.repository.OrderKtRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

@Service
@Transactional
class OrderService(
    private val orderRepository: OrderKtRepository
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
        val updatedOrder = existingOrder.copy(
            customerId = request.customerId,
            amount = request.amount,
            status = request.status,
            description = request.description,
            businessDate = existingOrder.businessDate, // Keep the same business date
            processingDate = infinityDate
        )
        
        // Update with the existing business date
        val savedOrder = orderRepository.update(updatedOrder, existingOrder.businessDate)
        return savedOrder.toDto()
    }
    
    fun deleteOrder(id: Long) {
        val existingOrder = orderRepository.findById(id)
            ?: throw EntityNotFoundException("Order not found with id: $id")
        // Delete with the existing business date
        orderRepository.deleteById(id, existingOrder.businessDate)
    }
    
    fun findOrdersByCustomer(customerId: Long): List<OrderDto> {
        return orderRepository.findByCustomerId(customerId).map { it.toDto() }
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