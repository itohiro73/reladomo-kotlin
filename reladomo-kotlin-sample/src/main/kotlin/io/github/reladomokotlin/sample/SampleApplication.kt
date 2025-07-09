package io.github.reladomokotlin.sample

import io.github.reladomokotlin.spring.repository.EnableReladomoRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableReladomoRepositories
class SampleApplication

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}