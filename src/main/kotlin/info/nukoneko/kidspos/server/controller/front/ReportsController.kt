package info.nukoneko.kidspos.server.controller.front

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/reports")
class ReportsController {
    private val logger = LoggerFactory.getLogger(ReportsController::class.java)

    @GetMapping("/sales")
    fun salesReportPage(): String {
        logger.info("Accessing sales report page")
        return "reports/sales"
    }
}
