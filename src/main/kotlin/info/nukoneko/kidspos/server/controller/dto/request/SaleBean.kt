package info.nukoneko.kidspos.server.controller.dto.request

data class SaleBean(
    val storeId: Int,
    val staffBarcode: String,
    val deposit: Int,
    val itemIds: String
)
