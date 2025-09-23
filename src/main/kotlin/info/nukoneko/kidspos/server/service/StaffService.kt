package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.CacheConfig
import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.repository.StaffRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing staff member operations
 *
 * Handles CRUD operations for staff data with caching support for improved
 * performance. Staff members are identified by barcode IDs and this service
 * provides efficient lookup capabilities for POS operations where staff
 * authentication is required.
 *
 * Key responsibilities:
 * - Managing staff member data retrieval
 * - Providing cached access to staff information
 * - Supporting barcode-based staff identification
 * - Maintaining data consistency through proper caching strategies
 *
 * Caching strategy:
 * - All staff data cached for bulk operations
 * - Individual staff lookup cached by barcode ID
 * - Cache warming through findAll() method
 *
 * @constructor Creates StaffService with required repository
 * @param repository Repository for staff data access
 */
@Service
@Transactional
class StaffService(
    private val repository: StaffRepository
) {
    private val logger = LoggerFactory.getLogger(StaffService::class.java)

    @Cacheable(value = [CacheConfig.STAFF_CACHE])
    fun findAll(): List<StaffEntity> {
        logger.debug("Fetching all staff from database")
        return repository.findAll()
    }

    @Cacheable(value = [CacheConfig.STAFF_BY_ID_CACHE], key = "#barcode")
    fun findStaff(barcode: String): StaffEntity? {
        logger.debug("Fetching staff by barcode: {} from database", barcode)
        return repository.findByIdOrNull(barcode)
    }

    fun save(staff: StaffEntity): StaffEntity {
        logger.debug("Saving staff with barcode: {}", staff.barcode)
        return repository.save(staff)
    }

    fun delete(barcode: String) {
        logger.debug("Deleting staff with barcode: {}", barcode)
        repository.deleteById(barcode)
    }

    fun delete(staff: StaffEntity) {
        logger.debug("Deleting staff entity with barcode: {}", staff.barcode)
        repository.delete(staff)
    }
}