package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.request.CreateSaleRequest
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.controller.dto.response.SaleResponse
import info.nukoneko.kidspos.server.service.ItemParsingService
import info.nukoneko.kidspos.server.service.ReceiptService
import info.nukoneko.kidspos.server.service.SaleProcessingService
import info.nukoneko.kidspos.server.service.SaleResult
import info.nukoneko.kidspos.server.service.mapper.SaleMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

/**
 * 販売APIコントローラー
 *
 * 販売取引の作成と管理を行うREST APIエンドポイントを提供
 * 責務は専門化されたサービスに委譲
 */
@RestController
@RequestMapping("/api/sales")
@Validated
class SaleApiController(
    private val saleProcessingService: SaleProcessingService,
    private val itemParsingService: ItemParsingService,
    private val receiptService: ReceiptService,
    private val saleMapper: SaleMapper,
) {
    private val logger = LoggerFactory.getLogger(SaleApiController::class.java)

    @PostMapping
    fun createSale(
        @Valid @RequestBody request: CreateSaleRequest,
    ): ResponseEntity<Map<String, Any>> {
        logger.info("Creating sale for store: {}", request.storeId)

        return try {
            // Parse items from item IDs
            val items = itemParsingService.parseItemsFromIds(request.itemIds)

            // Process the sale
            val saleBean =
                SaleBean(
                    storeId = request.storeId,
                    itemIds = request.itemIds,
                    deposit = request.deposit,
                )
            when (val result = saleProcessingService.processSaleWithValidation(saleBean, items)) {
                is SaleResult.Success -> {
                    // Print receipt asynchronously (non-blocking)
                    CompletableFuture.runAsync {
                        try {
                            receiptService.printReceipt(
                                request.storeId,
                                items,
                                request.deposit,
                            )
                        } catch (e: Exception) {
                            logger.error("Failed to print receipt asynchronously", e)
                        }
                    }

                    val sale = result.sale
                    val response =
                        mapOf(
                            "id" to sale.id,
                            "amount" to sale.amount,
                            "quantity" to sale.quantity,
                            "deposit" to request.deposit,
                            "change" to (request.deposit - sale.amount),
                            "storeId" to sale.storeId,
                        )
                    logger.info("Sale created successfully: ID={}", sale.id)
                    ResponseEntity.status(201).body(response)
                }

                is SaleResult.Error -> {
                    logger.error("Sale creation failed: {}", result.message)
                    ResponseEntity.badRequest().body(mapOf("error" to result.message))
                }

                is SaleResult.ValidationError -> {
                    logger.warn("Sale validation failed: {}", result.message)
                    ResponseEntity.badRequest().body(mapOf("error" to result.message))
                }

                is SaleResult.ProcessingError -> {
                    logger.error("Sale processing failed: {}", result.message)
                    ResponseEntity.status(500).body(mapOf("error" to result.message))
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing sale", e)
            ResponseEntity.status(500).body(mapOf("error" to "Internal server error"))
        }
    }

    @PostMapping("/create")
    fun createSaleOld(
        @Valid @ModelAttribute saleBean: SaleBean,
    ): ResponseEntity<SaleResponse> {
        logger.info("Creating sale for store: {}", saleBean.storeId)

        return try {
            // Parse items from item IDs
            val items = itemParsingService.parseItemsFromIds(saleBean.itemIds)

            // Process the sale
            when (val result = saleProcessingService.processSaleWithValidation(saleBean, items)) {
                is SaleResult.Success -> {
                    // Print receipt
                    receiptService.printReceipt(
                        saleBean.storeId,
                        items,
                        saleBean.deposit,
                    )

                    val response = saleMapper.toResponse(result.sale)
                    logger.info("Sale created successfully: ID={}", result.sale.id)
                    ResponseEntity.ok(response)
                }

                is SaleResult.ValidationError -> {
                    logger.warn("Sale validation failed: {}", result.message)
                    ResponseEntity.badRequest().build()
                }

                is SaleResult.ProcessingError -> {
                    logger.error("Sale processing failed: {}", result.message)
                    ResponseEntity.internalServerError().build()
                }

                is SaleResult.Error -> {
                    logger.error("Sale creation failed: {}", result.message)
                    ResponseEntity.badRequest().build()
                }
            }
        } catch (e: Exception) {
            logger.error("Unexpected error creating sale", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/{id}")
    fun getSale(
        @PathVariable id: Int,
    ): ResponseEntity<SaleResponse> {
        logger.info("Fetching sale with ID: {}", id)

        return try {
            val sale = saleProcessingService.findSaleById(id)
            if (sale != null) {
                val response = saleMapper.toResponse(sale)
                ResponseEntity.ok(response)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            logger.error("Error fetching sale with ID: {}", id, e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping
    fun getAllSales(): ResponseEntity<List<SaleResponse>> {
        logger.info("Fetching all sales")

        return try {
            val sales = saleProcessingService.findAllSales()
            val responses = saleMapper.toResponseList(sales)
            ResponseEntity.ok(responses)
        } catch (e: Exception) {
            logger.error("Error fetching all sales", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/validate-printer/{storeId}")
    fun validatePrinter(
        @PathVariable storeId: Int,
    ): ResponseEntity<Map<String, Boolean>> {
        logger.info("Validating printer configuration for store: {}", storeId)

        val isValid = receiptService.validatePrinterConfiguration(storeId)
        return ResponseEntity.ok(mapOf("printerConfigured" to isValid))
    }
}
