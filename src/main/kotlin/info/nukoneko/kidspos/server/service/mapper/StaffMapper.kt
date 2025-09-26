package info.nukoneko.kidspos.server.service.mapper

import info.nukoneko.kidspos.server.controller.dto.response.StaffResponse
import info.nukoneko.kidspos.server.entity.StaffEntity
import org.springframework.stereotype.Component

/**
 * Data mapper for Staff entities and DTOs
 *
 * This mapper handles unidirectional data transformation from Staff domain objects
 * to their corresponding Data Transfer Objects for API responses. It manages
 * the conversion of staff information while handling data structure differences
 * and providing default values for missing attributes.
 *
 * ## Mapping Responsibilities:
 * - **Entity to Response**: Transforms StaffEntity objects into StaffResponse DTOs
 *   for API consumption and frontend display
 * - **ID Normalization**: Uses barcode as the primary identifier in both ID and
 *   barcode fields of the response to maintain consistency
 * - **Default Value Assignment**: Provides sensible defaults for missing data
 *   such as default store assignments and null timestamps
 * - **Batch Operations**: Supports efficient list transformations for staff collections
 *
 * ## Data Flow Patterns:
 * ```
 * StaffEntity -> StaffResponse (for API responses)
 * List<StaffEntity> -> List<StaffResponse> (for collection endpoints)
 * ```
 *
 * ## Implementation Specifics:
 * - **Barcode as ID**: The StaffEntity uses barcode as the primary identifier,
 *   which is mapped to both the ID and barcode fields in the response
 * - **Default Store Assignment**: Staff members are assigned to store ID 1 by default
 *   since store relationships are not maintained in the current entity structure
 * - **Missing Relationships**: Store name lookup is not performed, leaving storeName null
 * - **Timestamp Compatibility**: CreatedAt and updatedAt fields are set to null
 *   as they're not available in the current entity
 *
 * ## Business Context:
 * Staff members represent employees who can operate the Kids POS system.
 * This mapper ensures consistent staff data presentation while maintaining
 * compatibility with response contracts that expect store associations and
 * audit timestamps, even when this data is not currently tracked.
 *
 * @see StaffEntity
 * @see StaffResponse
 * @since 1.0.0
 */
@Component
class StaffMapper {
    fun toResponse(entity: StaffEntity): StaffResponse =
        StaffResponse(
            id = entity.barcode, // barcode is the ID in StaffEntity
            name = entity.name,
            barcode = entity.barcode,
            storeId = 1, // Default store ID since not available in entity
            storeName = null, // Not available without additional lookup
            createdAt = null, // Not available in current entity
            updatedAt = null, // Not available in current entity
        )

    fun toResponseList(entities: List<StaffEntity>): List<StaffResponse> = entities.map { toResponse(it) }
}
