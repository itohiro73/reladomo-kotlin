package io.github.chronostaff.controller

import io.github.chronostaff.domain.PositionFinder
import io.github.chronostaff.dto.PositionDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/positions")
class PositionController {

    @GetMapping
    fun getAllPositions(): List<PositionDto> {
        return PositionFinder.findMany(PositionFinder.all())
            .map { position ->
                PositionDto(
                    id = position.id,
                    name = position.name,
                    level = position.level,
                    description = position.description
                )
            }
    }

    @GetMapping("/{id}")
    fun getPosition(@PathVariable id: Long): PositionDto? {
        val position = PositionFinder.findByPrimaryKey(id) ?: return null
        return PositionDto(
            id = position.id,
            name = position.name,
            level = position.level,
            description = position.description
        )
    }
}
