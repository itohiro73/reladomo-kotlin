package io.github.chronostaff.controller

import com.gs.fw.common.mithra.MithraManagerProvider
import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp
import io.github.chronostaff.domain.EmployeeAssignmentFinder
import io.github.chronostaff.domain.Position
import io.github.chronostaff.domain.PositionFinder
import io.github.chronostaff.dto.PositionDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp

@RestController
@RequestMapping("/api/positions")
class PositionController {

    @GetMapping
    fun getAllPositions(): List<PositionDto> {
        val operation = PositionFinder.businessDate().equalsInfinity()
            .and(PositionFinder.processingDate().equalsInfinity())
        return PositionFinder.findMany(operation)
            .map { position ->
                PositionDto(
                    id = position.id,
                    name = position.name,
                    level = position.level,
                    description = position.description,
                    businessFrom = position.businessDateFrom.toInstant().toString(),
                    businessThru = position.businessDateTo.toInstant().toString(),
                    processingFrom = position.processingDateFrom.toInstant().toString(),
                    processingThru = position.processingDateTo.toInstant().toString()
                )
            }
    }

    @GetMapping("/{id}")
    fun getPosition(@PathVariable id: Long): PositionDto? {
        val operation = PositionFinder.id().eq(id)
            .and(PositionFinder.businessDate().equalsInfinity())
            .and(PositionFinder.processingDate().equalsInfinity())
        val position = PositionFinder.findOne(operation) ?: return null
        return PositionDto(
            id = position.id,
            name = position.name,
            level = position.level,
            description = position.description,
            businessFrom = position.businessDateFrom.toInstant().toString(),
            businessThru = position.businessDateTo.toInstant().toString(),
            processingFrom = position.processingDateFrom.toInstant().toString(),
            processingThru = position.processingDateTo.toInstant().toString()
        )
    }

    @PostMapping
    fun createPosition(@RequestBody dto: PositionDto, @RequestParam companyId: Long): PositionDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val infinityDate = Timestamp.from(DefaultInfinityTimestamp.getDefaultInfinity().toInstant())
            val position = Position(infinityDate, infinityDate)
            position.companyId = companyId
            position.name = dto.name
            position.level = dto.level
            position.description = dto.description
            position.insert()

            PositionDto(
                id = position.id,
                name = position.name,
                level = position.level,
                description = position.description,
                businessFrom = position.businessDateFrom.toInstant().toString(),
                businessThru = position.businessDateTo.toInstant().toString(),
                processingFrom = position.processingDateFrom.toInstant().toString(),
                processingThru = position.processingDateTo.toInstant().toString()
            )
        }
    }

    @PutMapping("/{id}")
    fun updatePosition(@PathVariable id: Long, @RequestBody dto: PositionDto): PositionDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val operation = PositionFinder.id().eq(id)
                .and(PositionFinder.businessDate().equalsInfinity())
                .and(PositionFinder.processingDate().equalsInfinity())
            val position = PositionFinder.findOne(operation)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found")

            position.name = dto.name
            position.level = dto.level
            position.description = dto.description

            PositionDto(
                id = position.id,
                name = position.name,
                level = position.level,
                description = position.description,
                businessFrom = position.businessDateFrom.toInstant().toString(),
                businessThru = position.businessDateTo.toInstant().toString(),
                processingFrom = position.processingDateFrom.toInstant().toString(),
                processingThru = position.processingDateTo.toInstant().toString()
            )
        }
    }

    @DeleteMapping("/{id}")
    fun deletePosition(@PathVariable id: Long) {
        MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val operation = PositionFinder.id().eq(id)
                .and(PositionFinder.businessDate().equalsInfinity())
                .and(PositionFinder.processingDate().equalsInfinity())
            val position = PositionFinder.findOne(operation)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found")

            // Check if position is in use
            val assignmentsWithPosition = EmployeeAssignmentFinder.findMany(
                EmployeeAssignmentFinder.positionId().eq(id)
                    .and(EmployeeAssignmentFinder.businessDate().equalsInfinity())
                    .and(EmployeeAssignmentFinder.processingDate().equalsInfinity())
            )

            if (assignmentsWithPosition.isNotEmpty()) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete position: ${assignmentsWithPosition.size} employees are currently assigned to this position"
                )
            }

            position.terminate()
        }
    }
}
