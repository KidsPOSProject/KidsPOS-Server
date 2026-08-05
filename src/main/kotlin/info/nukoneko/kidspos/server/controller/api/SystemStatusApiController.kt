package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.response.SystemStatusResponse
import info.nukoneko.kidspos.server.service.SystemStatusService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/status")
class SystemStatusApiController(
    private val systemStatusService: SystemStatusService,
) {
    @GetMapping
    fun getStatus(): ResponseEntity<SystemStatusResponse> =
        ResponseEntity.ok(systemStatusService.getStatus())
}
