package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.domain.exception.DuplicateResourceException
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.service.StaffService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * ユーザーAPIコントローラー
 *
 * ユーザー（スタッフ）情報の取得と管理を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/users")
class UserApiController {
    @Autowired
    private lateinit var staffService: StaffService

    @GetMapping
    fun getUsers(): ResponseEntity<List<StaffEntity>> = ResponseEntity.ok(staffService.findAll())

    @GetMapping("/{barcode}")
    fun getUser(
        @PathVariable barcode: String,
    ): ResponseEntity<StaffEntity> {
        val user =
            staffService.findStaff(barcode)
                ?: throw ResourceNotFoundException("User with barcode $barcode not found")
        return ResponseEntity.ok(user)
    }

    @PostMapping
    fun createUser(
        @Valid @RequestBody user: StaffEntity,
    ): ResponseEntity<StaffEntity> {
        // Validate required fields
        if (user.barcode.isBlank()) {
            throw IllegalArgumentException("Barcode is required")
        }
        if (user.name.isBlank()) {
            throw IllegalArgumentException("Name is required")
        }

        // Check for duplicate barcode
        if (staffService.findStaff(user.barcode) != null) {
            throw DuplicateResourceException("User with barcode ${user.barcode} already exists")
        }

        val savedUser = staffService.save(user)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser)
    }

    @PutMapping("/{barcode}")
    fun updateUser(
        @PathVariable barcode: String,
        @Valid @RequestBody user: StaffEntity,
    ): ResponseEntity<StaffEntity> {
        // Check if user exists
        staffService.findStaff(barcode)
            ?: throw ResourceNotFoundException("User with barcode $barcode not found")

        // Validate required fields
        if (user.name.isBlank()) {
            throw IllegalArgumentException("Name is required")
        }

        val updatedUser = staffService.save(user.copy(barcode = barcode))
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/{barcode}")
    fun deleteUser(
        @PathVariable barcode: String,
    ): ResponseEntity<Void> {
        // Check if user exists
        staffService.findStaff(barcode)
            ?: throw ResourceNotFoundException("User with barcode $barcode not found")

        staffService.delete(barcode)
        return ResponseEntity.noContent().build()
    }
}
