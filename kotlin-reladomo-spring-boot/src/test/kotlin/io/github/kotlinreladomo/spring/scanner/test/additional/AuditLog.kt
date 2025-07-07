package io.github.kotlinreladomo.spring.scanner.test.additional

import io.github.kotlinreladomo.spring.annotation.*
import io.github.kotlinreladomo.spring.config.ReladomoKotlinProperties.CacheType
import java.time.Instant

@ReladomoEntity(
    tableName = "AUDIT_LOGS",
    connectionManager = "auditDB",
    cacheType = CacheType.NONE
)
class AuditLog {
    @PrimaryKey
    val id: Long = 0
    
    @Column(name = "ACTION")
    val action: String = ""
    
    @Column(name = "TIMESTAMP")
    val timestamp: Instant = Instant.now()
}