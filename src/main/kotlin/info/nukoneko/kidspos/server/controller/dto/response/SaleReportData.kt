package info.nukoneko.kidspos.server.controller.dto.response

import java.util.Date

data class SaleReportData(
    val saleId: Int,
    val storeId: Int,
    val storeName: String,
    val staffId: Int,
    val staffName: String,
    val quantity: Int,
    val amount: Int,
    val createdAt: Date,
    val details: List<SaleReportDetailData>,
)

data class SaleReportDetailData(
    val itemId: Int,
    val itemName: String,
    val price: Int,
    val quantity: Int,
    val subtotal: Int,
)

data class SaleReportSummary(
    val totalSales: Int,
    val totalAmount: Int,
    val averageAmount: Double,
    val startDate: Date,
    val endDate: Date,
)
