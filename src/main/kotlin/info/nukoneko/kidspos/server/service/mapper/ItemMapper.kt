package info.nukoneko.kidspos.server.service.mapper

import info.nukoneko.kidspos.server.controller.dto.request.CreateItemRequest
import info.nukoneko.kidspos.server.controller.dto.response.ItemResponse
import info.nukoneko.kidspos.server.entity.ItemEntity
import org.springframework.stereotype.Component

/**
 * Data mapper for Item entities and DTOs
 *
 * This mapper handles all data transformations between Item domain objects and their
 * corresponding Data Transfer Objects (DTOs). It provides bidirectional mapping
 * capabilities for the complete item lifecycle in the Kids POS system.
 *
 * ## Mapping Responsibilities:
 * - **Request to Entity**: Transforms CreateItemRequest DTOs into ItemEntity objects
 *   for database persistence, handling ID assignment and validation
 * - **Entity to Response**: Converts ItemEntity objects into ItemResponse DTOs
 *   for API responses and frontend consumption
 * - **Batch Operations**: Provides efficient list-based transformations for
 *   bulk operations and collection responses
 *
 * ## Data Flow Patterns:
 * ```
 * CreateItemRequest -> ItemEntity (for creation/updates)
 * ItemEntity -> ItemResponse (for API responses)
 * List<ItemEntity> -> List<ItemResponse> (for collection endpoints)
 * ```
 *
 * ## Business Context:
 * Items represent products available for sale in the Kids POS system. This mapper
 * ensures consistent data transformation while maintaining the integrity of item
 * attributes like barcode, name, and price across all system layers.
 *
 * @see ItemEntity
 * @see CreateItemRequest
 * @see ItemResponse
 * @since 1.0.0
 */
@Component
class ItemMapper {
    fun toEntity(
        request: CreateItemRequest,
        id: Int,
    ): ItemEntity =
        ItemEntity(
            id = id,
            barcode = request.barcode ?: "",
            name = request.name,
            price = request.price,
        )

    fun toResponse(entity: ItemEntity): ItemResponse =
        ItemResponse(
            id = entity.id,
            name = entity.name,
            barcode = entity.barcode,
            price = entity.price,
        )

    fun toResponseList(entities: List<ItemEntity>): List<ItemResponse> = entities.map { toResponse(it) }
}
