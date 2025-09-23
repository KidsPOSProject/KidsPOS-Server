package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.service.StaffService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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