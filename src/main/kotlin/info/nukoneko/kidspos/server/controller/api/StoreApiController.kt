package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * 店舗APIコントローラー
 *
 * 店舗情報の取得と管理を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/store")
class StoreApiController {
    @Autowired
    private lateinit var service: StoreService

    @RequestMapping("list", method = [RequestMethod.GET])
    fun getStores(): List<StoreEntity> {
        return service.findAll()
    }
}