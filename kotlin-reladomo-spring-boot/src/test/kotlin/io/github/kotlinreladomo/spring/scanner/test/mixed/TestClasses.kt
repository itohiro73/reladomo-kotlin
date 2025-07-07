package io.github.kotlinreladomo.spring.scanner.test.mixed

import io.github.kotlinreladomo.spring.annotation.*

@ReladomoEntity
class ValidEntity {
    @PrimaryKey
    val id: Long = 0
}

// This should not be picked up by scanner
class RegularClass {
    val id: Long = 0
}