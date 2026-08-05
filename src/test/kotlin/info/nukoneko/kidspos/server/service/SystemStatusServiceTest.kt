package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.entity.StoreEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SystemStatusServiceTest {
    private val storeService: StoreService = mock()
    private val printerConnectionChecker: PrinterConnectionChecker = mock()
    private val appProperties =
        AppProperties(
            apiVersion = "2.3.4",
            receipt =
                AppProperties.ReceiptProperties(
                    printer =
                        AppProperties.ReceiptProperties.PrinterProperties(
                            port = 9100,
                        ),
                ),
        )

    private val service =
        SystemStatusService(
            storeService = storeService,
            appProperties = appProperties,
            printerConnectionChecker = printerConnectionChecker,
        )

    @Test
    fun `returns API version and reachable printer status`() {
        whenever(storeService.findAll()).thenReturn(
            listOf(
                StoreEntity(
                    id = 1,
                    name = "Main store",
                    printerUri = "192.168.1.50",
                ),
            ),
        )
        whenever(
            printerConnectionChecker.isReachable(
                host = "192.168.1.50",
                port = 9100,
            ),
        ).thenReturn(true)

        val result = service.getStatus()

        assertEquals("ok", result.status)
        assertEquals("2.3.4", result.apiVersion)
        assertTrue(result.printer.configured)
        assertTrue(result.printer.reachable)
        assertEquals(1, result.printer.total)
        assertEquals(1, result.printer.reachableCount)
    }

    @Test
    fun `returns not configured when no printer host exists`() {
        whenever(storeService.findAll()).thenReturn(emptyList())

        val result = service.getStatus()

        assertFalse(result.printer.configured)
        assertFalse(result.printer.reachable)
        assertEquals(0, result.printer.total)
        assertEquals(0, result.printer.reachableCount)
    }
}
