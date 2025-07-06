package io.github.kotlinreladomo.sample.controller

import io.github.kotlinreladomo.sample.service.OrderService
import io.github.kotlinreladomo.sample.dto.OrderDto
import io.github.kotlinreladomo.sample.dto.CreateOrderRequest
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.math.BigDecimal

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {
    
    @GetMapping
    fun getAllOrders(): List<OrderDto> {
        return orderService.findAllOrders()
    }
    
    @GetMapping("/{id}")
    fun getOrder(@PathVariable id: Long): OrderDto {
        return orderService.findOrderById(id)
    }
    
    @GetMapping("/{id}/asof")
    fun getOrderAsOf(
        @PathVariable id: Long,
        @RequestParam businessDate: String,
        @RequestParam(required = false) processingDate: String?
    ): OrderDto {
        val businessInstant = Instant.parse(businessDate)
        val processingInstant = processingDate?.let { Instant.parse(it) } ?: Instant.now()
        
        return orderService.findOrderAsOf(id, businessInstant, processingInstant)
    }
    
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): OrderDto {
        return orderService.createOrder(request)
    }
    
    @PutMapping("/{id}")
    fun updateOrder(
        @PathVariable id: Long,
        @RequestBody request: CreateOrderRequest
    ): OrderDto {
        return orderService.updateOrder(id, request)
    }
    
    @DeleteMapping("/{id}")
    fun deleteOrder(@PathVariable id: Long) {
        orderService.deleteOrder(id)
    }
    
    @GetMapping("/customer/{customerId}")
    fun getOrdersByCustomer(@PathVariable customerId: Long): List<OrderDto> {
        return orderService.findOrdersByCustomer(customerId)
    }
    
    @GetMapping("/status/{status}")
    fun getOrdersByStatus(@PathVariable status: String): List<OrderDto> {
        return orderService.findOrdersByStatus(status)
    }
    
    @GetMapping("/high-value")
    fun getHighValueOrders(@RequestParam minAmount: BigDecimal): List<OrderDto> {
        return orderService.findHighValueOrders(minAmount)
    }
    
    @GetMapping("/customer/{customerId}/exists")
    fun checkCustomerHasOrders(@PathVariable customerId: Long): Map<String, Boolean> {
        return mapOf("exists" to orderService.orderExistsForCustomer(customerId))
    }
    
    @GetMapping("/status/{status}/count")
    fun countOrdersByStatus(@PathVariable status: String): Map<String, Long> {
        return mapOf("count" to orderService.countOrdersByStatus(status))
    }
}