package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.service.StaffService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/staff")
class StaffApiController {
    @Autowired
    private lateinit var service: StaffService

    @RequestMapping(method = [RequestMethod.GET], value = ["{barcode}"])
    fun getStaff(@PathVariable barcode: String): StaffEntity? {
        return service.findStaff(barcode)
    }
}