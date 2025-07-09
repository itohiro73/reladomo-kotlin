package io.github.reladomokotlin.sequence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap

class InMemorySequenceGeneratorTest {
    
    private lateinit var generator: InMemorySequenceGenerator
    
    @BeforeEach
    fun setUp() {
        generator = InMemorySequenceGenerator()
    }
    
    @Test
    fun `should generate sequential IDs starting from default value`() {
        val id1 = generator.getNextId("Order")
        val id2 = generator.getNextId("Order")
        val id3 = generator.getNextId("Order")
        
        assertEquals(1000L, id1)
        assertEquals(1001L, id2)
        assertEquals(1002L, id3)
    }
    
    @Test
    fun `should generate sequential IDs with custom start value`() {
        val customGenerator = InMemorySequenceGenerator(defaultStartValue = 5000L)
        
        val id1 = customGenerator.getNextId("Customer")
        val id2 = customGenerator.getNextId("Customer")
        
        assertEquals(5000L, id1)
        assertEquals(5001L, id2)
    }
    
    @Test
    fun `should generate sequential IDs with custom increment`() {
        val customGenerator = InMemorySequenceGenerator(incrementBy = 10)
        
        val id1 = customGenerator.getNextId("Product")
        val id2 = customGenerator.getNextId("Product")
        val id3 = customGenerator.getNextId("Product")
        
        assertEquals(1000L, id1)
        assertEquals(1010L, id2)
        assertEquals(1020L, id3)
    }
    
    @Test
    fun `should maintain separate sequences for different names`() {
        val orderId1 = generator.getNextId("Order")
        val customerId1 = generator.getNextId("Customer")
        val orderId2 = generator.getNextId("Order")
        val customerId2 = generator.getNextId("Customer")
        
        assertEquals(1000L, orderId1)
        assertEquals(1000L, customerId1)
        assertEquals(1001L, orderId2)
        assertEquals(1001L, customerId2)
    }
    
    @Test
    fun `should generate batch of IDs`() {
        val ids = generator.getNextIds("Order", 5)
        
        assertEquals(5, ids.size)
        assertEquals(listOf(1000L, 1001L, 1002L, 1003L, 1004L), ids)
        
        // Next single ID should continue from where batch ended
        val nextId = generator.getNextId("Order")
        assertEquals(1005L, nextId)
    }
    
    @Test
    fun `should generate batch with custom increment`() {
        val customGenerator = InMemorySequenceGenerator(incrementBy = 5)
        val ids = customGenerator.getNextIds("Product", 3)
        
        assertEquals(listOf(1000L, 1005L, 1010L), ids)
        
        val nextId = customGenerator.getNextId("Product")
        assertEquals(1015L, nextId)
    }
    
    @Test
    fun `should throw exception for invalid batch count`() {
        assertThrows<IllegalArgumentException> {
            generator.getNextIds("Order", 0)
        }
        
        assertThrows<IllegalArgumentException> {
            generator.getNextIds("Order", -1)
        }
    }
    
    @Test
    fun `should reset sequence to specific value`() {
        generator.getNextId("Order") // 1000
        generator.getNextId("Order") // 1001
        
        generator.resetSequence("Order", 2000L)
        
        val nextId = generator.getNextId("Order")
        assertEquals(2000L, nextId)
    }
    
    @Test
    fun `should clear all sequences`() {
        generator.getNextId("Order")
        generator.getNextId("Customer")
        generator.getNextId("Product")
        
        generator.clearAll()
        
        // All sequences should restart from default
        assertEquals(1000L, generator.getNextId("Order"))
        assertEquals(1000L, generator.getNextId("Customer"))
        assertEquals(1000L, generator.getNextId("Product"))
    }
    
    @Test
    fun `should be thread-safe for concurrent ID generation`() {
        val numThreads = 10
        val idsPerThread = 100
        val latch = CountDownLatch(numThreads)
        val executor = Executors.newFixedThreadPool(numThreads)
        val generatedIds = ConcurrentHashMap.newKeySet<Long>()
        
        repeat(numThreads) {
            executor.submit {
                try {
                    repeat(idsPerThread) {
                        val id = generator.getNextId("Order")
                        val wasNew = generatedIds.add(id)
                        assertTrue(wasNew, "Duplicate ID generated: $id")
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        latch.await()
        executor.shutdown()
        
        // Should have generated exactly the expected number of unique IDs
        assertEquals(numThreads * idsPerThread, generatedIds.size)
        
        // All IDs should be in the expected range
        val sortedIds = generatedIds.sorted()
        assertEquals(1000L, sortedIds.first())
        assertEquals(1000L + (numThreads * idsPerThread) - 1, sortedIds.last())
    }
    
    @Test
    fun `should be thread-safe for batch generation`() {
        val numThreads = 5
        val batchSize = 10
        val latch = CountDownLatch(numThreads)
        val executor = Executors.newFixedThreadPool(numThreads)
        val allIds = ConcurrentHashMap.newKeySet<Long>()
        
        repeat(numThreads) {
            executor.submit {
                try {
                    val batch = generator.getNextIds("Customer", batchSize)
                    batch.forEach { id ->
                        val wasNew = allIds.add(id)
                        assertTrue(wasNew, "Duplicate ID in batch: $id")
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        latch.await()
        executor.shutdown()
        
        assertEquals(numThreads * batchSize, allIds.size)
    }
}