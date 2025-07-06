package io.github.kotlinreladomo.sample

import io.github.kotlinreladomo.spring.repository.EnableReladomoRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableReladomoRepositories
class SampleApplication

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}