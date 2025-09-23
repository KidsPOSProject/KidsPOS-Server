package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.service.StaffService
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import org.springframework.beans.factory.annotation.Autowired
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
    fun getStaff(@PathVariable barcode: String): ResponseEntity<StaffEntity> {
        val staff = service.findStaff(barcode)
            ?: throw ResourceNotFoundException("Staff with barcode $barcode not found")
        return ResponseEntity.ok(staff)
    }

    @GetMapping
    fun getAllStaff(): ResponseEntity<List<StaffEntity>> {
        return ResponseEntity.ok(service.findAll())
    }
}