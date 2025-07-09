package io.github.reladomokotlin.spring.scanner.test.mixed

import io.github.reladomokotlin.spring.annotation.*

@ReladomoEntity
class ValidEntity {
    @PrimaryKey
    val id: Long = 0
}

// This should not be picked up by scanner
class RegularClass {
    val id: Long = 0
}