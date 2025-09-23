package info.nukoneko.kidspos.server.service.mapper

import info.nukoneko.kidspos.server.controller.dto.response.StoreResponse
import info.nukoneko.kidspos.server.entity.StoreEntity
import org.springframework.stereotype.Component

/**
 * Data mapper for Store entities and DTOs
 *
 * This mapper provides unidirectional data transformation from Store domain objects
 * to their corresponding Data Transfer Objects for API responses. It handles the
 * conversion of store information while managing data compatibility between
 * entity and response structures.
 *
 * ## Mapping Responsibilities:
 * - **Entity to Response**: Transforms StoreEntity objects into StoreResponse DTOs
 *   for API consumption and frontend display
 * - **Barcode Generation**: Dynamically generates display barcodes from store IDs
 *   using zero-padded formatting (4-digit format)
 * - **Data Compatibility**: Handles missing fields gracefully by providing null
 *   values for unavailable attributes (kana, timestamps)
 * - **Batch Operations**: Supports efficient list transformations for multiple stores
 *
 * ## Data Flow Patterns:
 * ```
 * StoreEntity -> StoreResponse (for API responses)
 * List<StoreEntity> -> List<StoreResponse> (for collection endpoints)
 * ```
 *
 * ## Implementation Notes:
 * - **Barcode Generation**: Store barcodes are generated as zero-padded 4-digit
 *   strings from the store ID (e.g., ID 1 becomes "0001")
 * - **Missing Fields**: Some response fields (kana, createdAt, updatedAt) are
 *   set to null as they're not available in the current entity structure
 * - **Read-Only Operations**: This mapper is optimized for read operations only,
 *   no reverse mapping is provided
 *
 * ## Business Context:
 * Stores represent physical locations or branches in the Kids POS system.
 * This mapper ensures consistent store data presentation while maintaining
 * backward compatibility with response contracts that expect additional fields.
 *
 * @see StoreEntity
 * @see StoreResponse
 * @since 1.0.0
 */
@Component
class StoreMapper {

    fun toResponse(entity: StoreEntity): StoreResponse {
        return StoreResponse(
            id = entity.id,
            name = entity.name,
            barcode = entity.id.toString().padStart(4, '0'), // Generate barcode from ID
            kana = null, // Not available in current entity
            createdAt = null, // Not available in current entity
            updatedAt = null  // Not available in current entity
        )
    }

    fun toResponseList(entities: List<StoreEntity>): List<StoreResponse> {
        return entities.map { toResponse(it) }
    }
}