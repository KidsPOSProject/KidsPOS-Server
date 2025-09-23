package info.nukoneko.kidspos.server.service.mapper

import info.nukoneko.kidspos.server.controller.dto.request.CreateSaleRequest
import info.nukoneko.kidspos.server.controller.dto.response.SaleResponse
import info.nukoneko.kidspos.server.controller.dto.response.SaleItemResponse
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.repository.StoreRepository
import info.nukoneko.kidspos.server.repository.StaffRepository
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Data mapper for Sale entities and DTOs with complex relationship resolution
 *
 * This mapper handles the most complex data transformations in the Kids POS system,
 * managing sales data conversion while resolving multiple entity relationships
 * and performing real-time data aggregation. It orchestrates data retrieval
 * from multiple repositories to build comprehensive sale responses.
 *
 * ## Mapping Responsibilities:
 * - **Entity to Response**: Transforms SaleEntity objects into detailed SaleResponse DTOs
 *   with full relationship resolution and calculated fields
 * - **Relationship Resolution**: Dynamically resolves and populates related data from:
 *   - Store information via StoreRepository
 *   - Staff details via StaffRepository
 *   - Item details via ItemRepository
 *   - Sale line items via SaleDetailRepository
 * - **Financial Calculations**: Computes derived values like change amounts and subtotals
 * - **Temporal Conversion**: Handles timestamp conversions from database format to LocalDateTime
 * - **Batch Operations**: Provides efficient list transformations with relationship resolution
 *
 * ## Data Flow Patterns:
 * ```
 * SaleEntity -> SaleResponse (with full relationship data)
 * ├─ Store lookup -> StoreName population
 * ├─ Staff lookup -> StaffName population
 * ├─ SaleDetail lookup -> Item list construction
 * └─ Item lookup per detail -> Complete item information
 *
 * List<SaleEntity> -> List<SaleResponse> (with relationships)
 * ```
 *
 * ## Complex Transformation Logic:
 * - **Multi-Repository Coordination**: Performs coordinated lookups across four repositories
 *   to build complete sale representations with all related data
 * - **Graceful Degradation**: Handles missing related entities by providing fallback values
 *   ("Unknown Store", "Unknown Staff", "Unknown" item names)
 * - **Financial Integrity**: Calculates change amounts using deposit minus total logic
 * - **Timezone Handling**: Converts UTC timestamps to system timezone for display
 * - **Performance Considerations**: Each sale response requires multiple database queries,
 *   making this mapper resource-intensive for large datasets
 *
 * ## Business Context:
 * Sales represent completed transactions in the Kids POS system, encompassing
 * the store location, staff member, items purchased, quantities, and payment details.
 * This mapper provides the most comprehensive view of sales data by aggregating
 * information from across the entire system architecture.
 *
 * ## Repository Dependencies:
 * This mapper requires injected repositories for relationship resolution:
 * - StoreRepository: Store name and details
 * - StaffRepository: Staff member information
 * - ItemRepository: Product details and pricing
 * - SaleDetailRepository: Individual line items per sale
 *
 * @param storeRepository Repository for store data lookup
 * @param staffRepository Repository for staff data lookup
 * @param itemRepository Repository for item/product data lookup
 * @param saleDetailRepository Repository for sale line item lookup
 *
 * @see SaleEntity
 * @see SaleResponse
 * @see SaleItemResponse
 * @since 1.0.0
 */
@Component
class SaleMapper(
    private val storeRepository: StoreRepository,
    private val staffRepository: StaffRepository,
    private val itemRepository: ItemRepository,
    private val saleDetailRepository: SaleDetailRepository
) {

    fun toResponse(entity: SaleEntity): SaleResponse {
        val store = storeRepository.findById(entity.storeId).orElse(null)
        val staff = staffRepository.findById(entity.staffId.toString()).orElse(null)
        val saleDetails = saleDetailRepository.findBySaleId(entity.id)

        val items = saleDetails.map { detail ->
            val item = itemRepository.findById(detail.itemId).orElse(null)
            SaleItemResponse(
                itemId = detail.itemId,
                itemName = item?.name ?: "Unknown",
                barcode = item?.barcode ?: "",
                quantity = detail.quantity,
                unitPrice = detail.price,
                subtotal = detail.price * detail.quantity
            )
        }

        return SaleResponse(
            id = entity.id,
            storeId = entity.storeId,
            storeName = store?.name ?: "Unknown Store",
            staffId = entity.staffId.toString(),
            staffName = staff?.name ?: "Unknown Staff",
            totalAmount = entity.amount,
            deposit = entity.deposit,
            change = entity.deposit - entity.amount,
            saleTime = LocalDateTime.ofInstant(entity.createdAt.toInstant(), ZoneId.systemDefault()),
            items = items
        )
    }

    fun toResponseList(entities: List<SaleEntity>): List<SaleResponse> {
        return entities.map { toResponse(it) }
    }
}