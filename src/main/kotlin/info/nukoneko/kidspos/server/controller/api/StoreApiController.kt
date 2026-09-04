package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.StoreService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 店舗APIコントローラー
 *
 * 店舗情報の取得と管理を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/stores")
class StoreApiController(
    private val service: StoreService,
) {
    @GetMapping
    fun getStores(): ResponseEntity<List<StoreEntity>> = ResponseEntity.ok(service.findAll())

    @PostMapping
    fun createStore(
        @Valid @RequestBody store: StoreEntity,
    ): ResponseEntity<StoreEntity> {
        if (store.name.isBlank()) {
            throw IllegalArgumentException("Store name is required")
        }

        // 登録なので id を受け取っても既存を書き換えず、採番に任せる
        val savedStore = service.save(store.copy(id = 0))
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStore)
    }

    @GetMapping("/{id}")
    fun getStore(
        @PathVariable id: Int,
    ): ResponseEntity<StoreEntity> {
        val store =
            service.findStore(id)
                ?: throw ResourceNotFoundException("Store with ID $id not found")
        return ResponseEntity.ok(store)
    }

    @PutMapping("/{id}")
    fun updateStore(
        @PathVariable id: Int,
        @Valid @RequestBody store: StoreEntity,
    ): ResponseEntity<StoreEntity> {
        // Check if store exists
        service.findStore(id)
            ?: throw ResourceNotFoundException("Store with ID $id not found")

        if (store.name.isBlank()) {
            throw IllegalArgumentException("Store name is required")
        }

        val updatedStore = service.save(store.copy(id = id))
        return ResponseEntity.ok(updatedStore)
    }

    @DeleteMapping("/{id}")
    fun deleteStore(
        @PathVariable id: Int,
    ): ResponseEntity<Void> {
        // Check if store exists
        service.findStore(id)
            ?: throw ResourceNotFoundException("Store with ID $id not found")

        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
