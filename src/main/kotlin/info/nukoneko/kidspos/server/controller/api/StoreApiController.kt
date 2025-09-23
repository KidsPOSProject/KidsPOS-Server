package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.StoreService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
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
class StoreApiController {
    @Autowired
    private lateinit var service: StoreService

    @GetMapping
    fun getStores(): ResponseEntity<List<StoreEntity>> {
        return ResponseEntity.ok(service.findAll())
    }

    @PostMapping
    fun createStore(@Valid @RequestBody store: StoreEntity): ResponseEntity<StoreEntity> {
        // Validate required fields
        if (store.name.isBlank()) {
            throw IllegalArgumentException("Store name is required")
        }
        if (store.printerUri.isBlank()) {
            throw IllegalArgumentException("Printer URI is required")
        }

        val savedStore = service.save(store)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStore)
    }

    @GetMapping("/{id}")
    fun getStore(@PathVariable id: Int): ResponseEntity<StoreEntity> {
        val store = service.findStore(id)
            ?: throw ResourceNotFoundException("Store with ID $id not found")
        return ResponseEntity.ok(store)
    }

    @PutMapping("/{id}")
    fun updateStore(@PathVariable id: Int, @Valid @RequestBody store: StoreEntity): ResponseEntity<StoreEntity> {
        // Check if store exists
        service.findStore(id)
            ?: throw ResourceNotFoundException("Store with ID $id not found")

        // Validate required fields
        if (store.name.isBlank()) {
            throw IllegalArgumentException("Store name is required")
        }
        if (store.printerUri.isBlank()) {
            throw IllegalArgumentException("Printer URI is required")
        }

        val updatedStore = service.save(store.copy(id = id))
        return ResponseEntity.ok(updatedStore)
    }

    @DeleteMapping("/{id}")
    fun deleteStore(@PathVariable id: Int): ResponseEntity<Void> {
        // Check if store exists
        service.findStore(id)
            ?: throw ResourceNotFoundException("Store with ID $id not found")

        // Note: Delete functionality needs to be implemented in service layer
        return ResponseEntity.noContent().build()
    }
}