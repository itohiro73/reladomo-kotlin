package io.github.reladomokotlin.sample.controller

import io.github.reladomokotlin.sample.dto.CreateOrderRequest
import io.github.reladomokotlin.sample.config.TestReladomoConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import com.fasterxml.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestReladomoConfiguration::class)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=none",
    "reladomo.kotlin.connection-manager-config-file=test-reladomo-runtime-config.xml",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:schema.sql"
])
@ActiveProfiles("test")
class OrderControllerTest {
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Autowired
    lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `should get all orders`() {
        mockMvc.get("/api/orders")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$[0].orderId") { exists() }
                jsonPath("$[0].customerId") { exists() }
                jsonPath("$[0].status") { exists() }
            }
    }
    
    @Test
    fun `should get order by id`() {
        mockMvc.get("/api/orders/1")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.orderId") { value(1) }
                jsonPath("$.customerId") { value(100) }
                jsonPath("$.status") { value("PENDING") }
            }
    }
    
    @Test
    fun `should return 404 for non-existent order`() {
        mockMvc.get("/api/orders/999")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("NOT_FOUND") }
            }
    }
    
    @Test
    fun `should create new order`() {
        val request = CreateOrderRequest(
            customerId = 300L,
            amount = BigDecimal("299.99"),
            status = "PENDING",
            description = "Test order"
        )
        
        mockMvc.post("/api/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.customerId") { value(300) }
            jsonPath("$.amount") { value(299.99) }
            jsonPath("$.status") { value("PENDING") }
        }
    }
    
    @Test
    fun `should update existing order`() {
        val request = CreateOrderRequest(
            customerId = 100L,
            amount = BigDecimal("1999.99"),
            status = "PROCESSING",
            description = "Updated order"
        )
        
        mockMvc.put("/api/orders/1") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.orderId") { value(1) }
            jsonPath("$.amount") { value(1999.99) }
            jsonPath("$.status") { value("PROCESSING") }
        }
    }
    
    @Test
    fun `should delete order`() {
        mockMvc.delete("/api/orders/3")
            .andExpect {
                status { isOk() }
            }
        
        // Verify it's deleted
        mockMvc.get("/api/orders/3")
            .andExpect {
                status { isNotFound() }
            }
    }
    
    @Test
    fun `should get orders by customer`() {
        mockMvc.get("/api/orders/customer/100")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].customerId") { value(100) }
                jsonPath("$[1].customerId") { value(100) }
            }
    }
}