package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.service.ItemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/item")
class ItemApiController {

    @Autowired
    private lateinit var service: ItemService

    @RequestMapping(method = [RequestMethod.GET], value = ["{barcode}"])
    fun getItem(@PathVariable barcode: String): ItemEntity {
        return service.findItem(barcode)
    }
}