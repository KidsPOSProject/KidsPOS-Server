package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.domain.exception.ItemNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for parsing and converting item data
 * Extracted from SaleApiController to improve separation of concerns
 */
@Service
class ItemParsingService(
    private val itemService: ItemService,
) {
    private val logger = LoggerFactory.getLogger(ItemParsingService::class.java)

    /**
     * Parse item IDs string and convert to ItemBean list
     */
    fun parseItemsFromIds(itemIds: String): List<ItemBean> {
        if (itemIds.isBlank()) {
            throw IllegalArgumentException("Item IDs cannot be empty")
        }

        val ids =
            itemIds
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        if (ids.isEmpty()) {
            throw IllegalArgumentException("No valid item IDs found")
        }

        logger.debug("Parsing {} item IDs: {}", ids.size, ids)

        return ids.mapNotNull { idStr ->
            try {
                val itemId = idStr.toInt()
                val itemEntity = itemService.findItem(itemId)

                if (itemEntity == null) {
                    logger.warn("Item not found for ID: {}", itemId)
                    throw ItemNotFoundException(id = itemId)
                }

                ItemBean(
                    id = itemEntity.id,
                    barcode = itemEntity.barcode,
                    name = itemEntity.name,
                    price = itemEntity.price,
                )
            } catch (e: NumberFormatException) {
                logger.error("Invalid item ID format: {}", idStr)
                throw IllegalArgumentException("Invalid item ID format: $idStr")
            }
        }
    }

    /**
     * Convert single item ID to ItemBean
     */
    fun parseItemFromId(itemId: Int): ItemBean {
        val itemEntity =
            itemService.findItem(itemId)
                ?: throw ItemNotFoundException(id = itemId)

        return ItemBean(
            id = itemEntity.id,
            barcode = itemEntity.barcode,
            name = itemEntity.name,
            price = itemEntity.price,
        )
    }

    /**
     * Parse items from barcode string
     */
    fun parseItemsFromBarcodes(barcodes: String): List<ItemBean> {
        if (barcodes.isBlank()) {
            throw IllegalArgumentException("Barcodes cannot be empty")
        }

        val barcodeList =
            barcodes
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        logger.debug("Parsing {} barcodes", barcodeList.size)

        return barcodeList.map { barcode ->
            val itemEntity =
                itemService.findItem(barcode)
                    ?: throw ItemNotFoundException(barcode = barcode)

            ItemBean(
                id = itemEntity.id,
                barcode = itemEntity.barcode,
                name = itemEntity.name,
                price = itemEntity.price,
            )
        }
    }

    /**
     * Validate item IDs format
     */
    fun validateItemIdsFormat(itemIds: String): Boolean {
        if (itemIds.isBlank()) return false

        val ids = itemIds.split(",").map { it.trim() }
        return ids.all { id ->
            id.isNotEmpty() && id.all { char -> char.isDigit() }
        }
    }

    /**
     * Count items from ID string
     */
    fun countItemsFromIds(itemIds: String): Int {
        if (itemIds.isBlank()) return 0

        return itemIds
            .split(",")
            .map { it.trim() }
            .count { it.isNotEmpty() }
    }

    /**
     * Get unique item count from ID string
     */
    fun getUniqueItemCount(itemIds: String): Int {
        if (itemIds.isBlank()) return 0

        return itemIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
            .size
    }
}
