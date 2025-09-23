package info.nukoneko.kidspos.server.controller.dto.request

data class ItemBean(
    val id: Int? = null,
    val barcode: String,
    val name: String,
    val price: Int
)