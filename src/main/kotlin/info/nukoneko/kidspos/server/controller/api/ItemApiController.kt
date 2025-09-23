package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.request.CreateItemRequest
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.response.ItemResponse
import info.nukoneko.kidspos.server.domain.exception.InvalidBarcodeException
import info.nukoneko.kidspos.server.domain.exception.ItemNotFoundException
import info.nukoneko.kidspos.server.service.BarcodeService
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.ValidationService
import info.nukoneko.kidspos.server.service.mapper.ItemMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * 商品APIコントローラー
 *
 * 商品情報のCRUD操作REST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/item")
@Validated
@Tag(name = "Items", description = "Product item management operations")
class ItemApiController {
    @Autowired
    private lateinit var itemService: ItemService

    @Autowired
    private lateinit var itemMapper: ItemMapper

    @Autowired
    private lateinit var validationService: ValidationService

    @Autowired
    private lateinit var barcodeService: BarcodeService

    private val logger = LoggerFactory.getLogger(ItemApiController::class.java)

    @GetMapping
    @Operation(summary = "Get all items", description = "Retrieve a list of all product items")
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved items",
        content = [Content(array = ArraySchema(schema = Schema(implementation = ItemResponse::class)))]
    )
    fun findAll(): ResponseEntity<List<ItemResponse>> {
        logger.info("Fetching all items")
        val items = itemService.findAll()
        return ResponseEntity.ok(itemMapper.toResponseList(items))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID", description = "Retrieve a specific item by its ID")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Item found",
                content = [Content(schema = Schema(implementation = ItemResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Item not found")
        ]
    )
    fun findById(
        @Parameter(description = "Item ID", required = true)
        @PathVariable id: Int
    ): ResponseEntity<ItemResponse> {
        logger.info("Fetching item with ID: {}", id)
        val item = itemService.findItem(id)
            ?: throw ItemNotFoundException(id = id)
        return ResponseEntity.ok(itemMapper.toResponse(item))
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Get item by barcode", description = "Retrieve a specific item by its barcode")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Item found",
                content = [Content(schema = Schema(implementation = ItemResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid barcode format"),
            ApiResponse(responseCode = "404", description = "Item not found")
        ]
    )
    fun findByBarcode(
        @Parameter(description = "Item barcode (4+ digits)", required = true, example = "1234567890")
        @PathVariable barcode: String
    ): ResponseEntity<ItemResponse> {
        logger.info("Fetching item with barcode: {}", barcode)

        // Validate barcode format
        if (!barcode.matches(Regex("^[0-9]{4,}$"))) {
            throw InvalidBarcodeException(barcode)
        }

        val item = itemService.findItem(barcode)
            ?: throw ItemNotFoundException(barcode = barcode)
        return ResponseEntity.ok(itemMapper.toResponse(item))
    }

    @PostMapping
    fun create(@Valid @RequestBody request: CreateItemRequest): ResponseEntity<ItemResponse> {
        logger.info("Creating new item with barcode: {}", request.barcode)

        // Validate barcode uniqueness
        validationService.validateBarcodeUnique(request.barcode)
        validationService.validatePriceRange(request.price)

        // Convert to legacy ItemBean for compatibility
        val itemBean = ItemBean(
            barcode = request.barcode,
            name = request.name,
            price = request.price
        )

        val savedItem = itemService.save(itemBean)
        logger.info("Item created successfully with ID: {}", savedItem.id)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(itemMapper.toResponse(savedItem))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Int,
        @Valid @RequestBody request: CreateItemRequest
    ): ResponseEntity<ItemResponse> {
        logger.info("Updating item with ID: {}", id)

        // Check if item exists
        itemService.findItem(id)
            ?: throw ItemNotFoundException(id = id)

        // Validate barcode uniqueness (exclude current item)
        validationService.validateBarcodeUnique(request.barcode, id)
        validationService.validatePriceRange(request.price)

        // Update the item
        val itemBean = ItemBean(
            id = id,
            barcode = request.barcode,
            name = request.name,
            price = request.price
        )

        val updatedItem = itemService.save(itemBean)
        logger.info("Item updated successfully with ID: {}", updatedItem.id)

        return ResponseEntity.ok(itemMapper.toResponse(updatedItem))
    }

    @PatchMapping("/{id}")
    fun partialUpdate(
        @PathVariable id: Int,
        @RequestBody updates: Map<String, Any>
    ): ResponseEntity<ItemResponse> {
        logger.info("Partially updating item with ID: {}", id)

        // Check if item exists
        val existingItem = itemService.findItem(id)
            ?: throw ItemNotFoundException(id = id)

        // Apply updates
        val barcode = updates["barcode"]?.toString() ?: existingItem.barcode
        val name = updates["name"]?.toString() ?: existingItem.name
        val price = updates["price"]?.toString()?.toIntOrNull() ?: existingItem.price

        // Validate if barcode changed
        if (barcode != existingItem.barcode) {
            validationService.validateBarcodeUnique(barcode, id)
        }
        validationService.validatePriceRange(price)

        // Update the item
        val itemBean = ItemBean(
            id = id,
            barcode = barcode,
            name = name,
            price = price
        )

        val updatedItem = itemService.save(itemBean)
        logger.info("Item partially updated successfully with ID: {}", updatedItem.id)

        return ResponseEntity.ok(itemMapper.toResponse(updatedItem))
    }

    @GetMapping("/barcode-pdf", produces = ["application/pdf"])
    @Operation(
        summary = "Generate barcode PDF",
        description = "Generate a PDF document containing barcodes for all items"
    )
    @ApiResponse(
        responseCode = "200",
        description = "PDF generated successfully",
        content = [Content(mediaType = "application/pdf")]
    )
    fun generateBarcodePdf(): ResponseEntity<ByteArray> {
        logger.info("Generating barcode PDF for all items")
        
        val items = itemService.findAll()
        val pdfBytes = barcodeService.generateBarcodePdf(items)
        
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            setContentDispositionFormData("inline", "barcodes.pdf")
        }
        
        logger.info("Barcode PDF generated successfully with {} items", items.size)
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Int): ResponseEntity<Void> {
        logger.info("Deleting item with ID: {}", id)

        // Check if item exists
        validationService.validateItemExists(id)

        // Note: Delete functionality needs to be implemented in service layer
        logger.warn("Delete functionality not yet implemented for item ID: {}", id)

        return ResponseEntity.noContent().build()
    }
}