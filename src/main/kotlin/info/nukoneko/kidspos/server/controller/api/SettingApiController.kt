package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.api.model.SaleBean
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.service.SaleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/setting")
class SettingApiController {
    @Autowired
    private lateinit var service: SaleService

    @RequestMapping(method = [RequestMethod.POST])
    fun createSale(@Validated @RequestBody sale: SaleBean): SaleEntity {
        return service.save(sale)
    }
}