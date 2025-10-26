package io.github.chronostaff.controller

import com.gs.fw.common.mithra.MithraManagerProvider
import io.github.chronostaff.domain.EmployeeAssignmentFinder
import io.github.chronostaff.domain.Position
import io.github.chronostaff.domain.PositionFinder
import io.github.chronostaff.dto.PositionDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

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

    @PostMapping
    fun createPosition(@RequestBody dto: PositionDto): PositionDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val position = Position()
            position.name = dto.name
            position.level = dto.level
            position.description = dto.description
            position.insert()

            PositionDto(
                id = position.id,
                name = position.name,
                level = position.level,
                description = position.description
            )
        }
    }

    @PutMapping("/{id}")
    fun updatePosition(@PathVariable id: Long, @RequestBody dto: PositionDto): PositionDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val position = PositionFinder.findByPrimaryKey(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found")

            position.name = dto.name
            position.level = dto.level
            position.description = dto.description

            PositionDto(
                id = position.id,
                name = position.name,
                level = position.level,
                description = position.description
            )
        }
    }

    @DeleteMapping("/{id}")
    fun deletePosition(@PathVariable id: Long) {
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val position = PositionFinder.findByPrimaryKey(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found")

            // Check if position is in use
            val assignmentsWithPosition = EmployeeAssignmentFinder.findMany(
                EmployeeAssignmentFinder.positionId().eq(id)
                    .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
            )

            if (assignmentsWithPosition.isNotEmpty()) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete position: ${assignmentsWithPosition.size} employees are currently assigned to this position"
                )
            }

            position.delete()
        }
    }
}
