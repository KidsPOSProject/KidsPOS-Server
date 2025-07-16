package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.BarcodeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/item")
class ItemApiController {

    @Autowired
    private lateinit var service: ItemService
    
    @Autowired
    private lateinit var barcodeService: BarcodeService

    @RequestMapping("list", method = [RequestMethod.GET])
    fun getItems(): List<ItemEntity> {
        return service.findAll()
    }

    @RequestMapping(method = [RequestMethod.GET], value = ["{barcode}"])
    fun getItem(@PathVariable barcode: String): ItemEntity? {
        return service.findItem(barcode)
    }
    
    @RequestMapping("barcode-pdf", method = [RequestMethod.GET])
    fun generateBarcodePdf(): ResponseEntity<ByteArray> {
        val items = service.findAll()
        val pdfBytes = barcodeService.generateBarcodePdf(items)
        
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData("attachment", "item-barcodes.pdf")
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes)
    }
}