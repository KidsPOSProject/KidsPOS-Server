package info.nukoneko.kidspos.server.service.mapper

import info.nukoneko.kidspos.server.controller.dto.response.SaleItemResponse
import info.nukoneko.kidspos.server.controller.dto.response.SaleResponse
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.SalePersistenceService
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Data mapper for Sale entities and DTOs with relationship resolution
 *
 * Builds sale responses by resolving the related store, items and line items.
 * Lookups go through the services so the mapper stays out of the data access
 * layer, and every relation is fetched once per call rather than per line item.
 *
 * @param storeService Service for store lookup
 * @param itemService Service for item lookup
 * @param salePersistenceService Service for sale detail lookup
 */
@Component
class SaleMapper(
    private val storeService: StoreService,
    private val itemService: ItemService,
    private val salePersistenceService: SalePersistenceService,
) {
    fun toResponse(entity: SaleEntity): SaleResponse = toResponseList(listOf(entity)).first()

    fun toResponseList(entities: List<SaleEntity>): List<SaleResponse> {
        if (entities.isEmpty()) {
            return emptyList()
        }

        val detailsBySaleId =
            salePersistenceService
                .findSaleDetails(entities.map { it.id })
                .groupBy { it.saleId }
        val storeNames = storeService.findAll().associate { it.id to it.name }
        val items = itemService.findAll().associateBy { it.id }

        return entities.map { entity ->
            val itemResponses =
                detailsBySaleId[entity.id].orEmpty().map { detail ->
                    val item = items[detail.itemId]
                    SaleItemResponse(
                        itemId = detail.itemId,
                        itemName = item?.name ?: "Unknown",
                        barcode = item?.barcode ?: "",
                        quantity = detail.quantity,
                        unitPrice = detail.price,
                        subtotal = detail.price * detail.quantity,
                    )
                }

            SaleResponse(
                id = entity.id,
                storeId = entity.storeId,
                storeName = storeNames[entity.storeId] ?: "Unknown Store",
                totalAmount = entity.amount,
                deposit = entity.deposit,
                change = entity.deposit - entity.amount,
                saleTime = OffsetDateTime.ofInstant(entity.createdAt.toInstant(), ZoneId.systemDefault()),
                items = itemResponses,
            )
        }
    }
}
