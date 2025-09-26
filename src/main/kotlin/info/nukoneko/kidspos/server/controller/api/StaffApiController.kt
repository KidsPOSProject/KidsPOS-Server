package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.request.CreateStaffRequest
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.service.StaffService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * スタッフAPIコントローラー
 *
 * スタッフ情報の取得と管理を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/staff")
class StaffApiController {
    @Autowired
    private lateinit var service: StaffService

    @GetMapping("/{barcode}")
    fun getStaff(
        @PathVariable barcode: String,
    ): ResponseEntity<StaffEntity> {
        val staff =
            service.findStaff(barcode)
                ?: throw ResourceNotFoundException("Staff with barcode $barcode not found")
        return ResponseEntity.ok(staff)
    }

    @GetMapping
    fun getAllStaff(): ResponseEntity<List<StaffEntity>> = ResponseEntity.ok(service.findAll())

    @PostMapping
    fun createStaff(
        @Valid @RequestBody request: CreateStaffRequest,
    ): ResponseEntity<StaffEntity> {
        val staff =
            StaffEntity(
                barcode = request.barcode,
                name = request.name,
            )
        val savedStaff = service.save(staff)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStaff)
    }

    @PutMapping("/{barcode}")
    fun updateStaff(
        @PathVariable barcode: String,
        @Valid @RequestBody request: CreateStaffRequest,
    ): ResponseEntity<StaffEntity> {
        val existingStaff =
            service.findStaff(barcode)
                ?: throw ResourceNotFoundException("Staff with barcode $barcode not found")

        val updatedStaff = existingStaff.copy(name = request.name)
        val savedStaff = service.save(updatedStaff)
        return ResponseEntity.ok(savedStaff)
    }

    @DeleteMapping("/{barcode}")
    fun deleteStaff(
        @PathVariable barcode: String,
    ): ResponseEntity<Void> {
        val existingStaff =
            service.findStaff(barcode)
                ?: throw ResourceNotFoundException("Staff with barcode $barcode not found")

        service.delete(existingStaff)
        return ResponseEntity.noContent().build()
    }
}
