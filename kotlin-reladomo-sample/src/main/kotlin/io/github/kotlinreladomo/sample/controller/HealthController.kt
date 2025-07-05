package io.github.kotlinreladomo.sample.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {
    
    @GetMapping("/health")
    fun health(): HealthResponse {
        return HealthResponse(
            status = "UP",
            timestamp = Instant.now(),
            service = "kotlin-reladomo-sample",
            version = "0.1.0-SNAPSHOT"
        )
    }
    
    @GetMapping("/")
    fun root(): Map<String, String> {
        return mapOf(
            "message" to "Kotlin Reladomo Sample Application",
            "version" to "0.1.0-SNAPSHOT",
            "docs" to "/api/orders (GET, POST)",
            "health" to "/health"
        )
    }
}

data class HealthResponse(
    val status: String,
    val timestamp: Instant,
    val service: String,
    val version: String
)