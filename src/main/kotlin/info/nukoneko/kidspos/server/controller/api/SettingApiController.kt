package info.nukoneko.kidspos.server.controller.api

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/setting")
class SettingApiController {
    @RequestMapping("status", method = [RequestMethod.GET])
    fun getStatus(): StatusBean {
        return StatusBean("OK")
    }

    class StatusBean(val status: String)
}