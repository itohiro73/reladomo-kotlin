package io.github.chronostaff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChronoStaffApplication

fun main(args: Array<String>) {
    runApplication<ChronoStaffApplication>(*args)
}
