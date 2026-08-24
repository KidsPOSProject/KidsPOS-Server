package info.nukoneko.kidspos.server.controller.dto.response

import java.util.Date

data class SaleSummaryResponse(
    val startDate: Date,
    val endDate: Date,
    val totalSales: Int,
    val totalItemCount: Int,
    val totalAmount: Int,
    val averageAmount: Int,
    val stores: List<StoreSalesSummary>,
    val items: List<ItemSalesSummary>,
    val daily: List<DailySalesSummary>,
)

data class StoreSalesSummary(
    val storeId: Int,
    val storeName: String,
    val salesCount: Int,
    val itemCount: Int,
    val amount: Int,
)

data class ItemSalesSummary(
    val itemId: Int,
    val itemName: String,
    val unitPrice: Int,
    val quantity: Int,
    val amount: Int,
)

data class DailySalesSummary(
    val date: String,
    val salesCount: Int,
    val itemCount: Int,
    val amount: Int,
)
