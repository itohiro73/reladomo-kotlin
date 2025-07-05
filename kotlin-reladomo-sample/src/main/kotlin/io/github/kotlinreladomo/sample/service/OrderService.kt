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
        val order = OrderKt(
            orderId = idGenerator.incrementAndGet(),
            customerId = request.customerId,
            orderDate = now,
            amount = request.amount,
            status = request.status,
            description = request.description,
            businessDate = now,
            processingDate = now
        )
        
        val savedOrder = orderRepository.save(order)
        return savedOrder.toDto()
    }
    
    fun updateOrder(id: Long, request: CreateOrderRequest): OrderDto {
        val existingOrder = orderRepository.findById(id)
            ?: throw EntityNotFoundException("Order not found with id: $id")
        
        val updatedOrder = existingOrder.copy(
            customerId = request.customerId,
            amount = request.amount,
            status = request.status,
            description = request.description,
            businessDate = Instant.now(),
            processingDate = Instant.now()
        )
        
        val savedOrder = orderRepository.update(updatedOrder)
        return savedOrder.toDto()
    }
    
    fun deleteOrder(id: Long) {
        orderRepository.deleteById(id)
    }
    
    fun findOrdersByCustomer(customerId: Long): List<OrderDto> {
        return orderRepository.findByCustomerId(customerId).map { it.toDto() }
    }
    
    private fun OrderKt.toDto(): OrderDto {
        return OrderDto(
            orderId = this.orderId,
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