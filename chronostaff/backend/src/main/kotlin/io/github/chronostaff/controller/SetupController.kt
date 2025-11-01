package io.github.chronostaff.controller

import com.gs.fw.common.mithra.MithraManagerProvider
import io.github.chronostaff.domain.Company
import io.github.chronostaff.domain.Department
import io.github.chronostaff.domain.Position
import io.github.chronostaff.dto.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Instant

@RestController
@RequestMapping("/api/setup")
@CrossOrigin(origins = ["http://localhost:5173"])
class SetupController {

    /**
     * Initial setup wizard endpoint
     * Creates positions and departments for a new organization
     */
    @PostMapping
    fun setupOrganization(@RequestBody request: SetupRequestDto): SetupResponseDto {
        return MithraManagerProvider.getMithraManager().executeTransactionalCommand { _ ->
            val createdPositions = mutableListOf<PositionDto>()
            val createdDepartments = mutableListOf<DepartmentDto>()

            try {
                val infinityDate = Timestamp.from(Instant.parse("9999-12-01T23:59:00Z"))

                // Step 1: Create Company (unitemporal)
                val company = Company(infinityDate)
                company.setName(request.companyName)
                company.insert()

                val companyId = company.id

                // Step 2: Create positions (bitemporal, per company)
                request.positions.forEach { positionDto ->
                    val position = Position(infinityDate, infinityDate)
                    position.companyId = companyId  // Link to company
                    position.name = positionDto.name
                    position.level = positionDto.level
                    position.description = positionDto.description
                    position.insert()

                    createdPositions.add(toPositionDto(position))
                }

                // Step 3: Create departments (bitemporal, per company)
                request.departments.forEach { deptDto ->
                    val department = Department(infinityDate, infinityDate)
                    department.companyId = companyId  // Link to company
                    department.name = deptDto.name
                    department.description = deptDto.description
                    department.insert()

                    createdDepartments.add(toDepartmentDto(department))
                }

                SetupResponseDto(
                    companyId = companyId,
                    companyName = request.companyName,
                    positions = createdPositions,
                    departments = createdDepartments
                )
            } catch (e: Exception) {
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to setup organization: ${e.message}",
                    e
                )
            }
        }
    }

    private fun toPositionDto(position: Position): PositionDto {
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

    private fun toDepartmentDto(department: Department): DepartmentDto {
        return DepartmentDto(
            id = department.id,
            name = department.name,
            description = department.description,
            parentDepartmentId = department.parentDepartmentId,
            businessFrom = department.businessDateFrom.toInstant().toString(),
            businessThru = department.businessDateTo.toInstant().toString(),
            processingFrom = department.processingDateFrom.toInstant().toString(),
            processingThru = department.processingDateTo.toInstant().toString()
        )
    }
}
