package info.nukoneko.kidspos.receipt

import info.nukoneko.kidspos.server.entity.ItemEntity
import java.util.*

data class ReceiptDetail(
        val items: List<ItemEntity>,
        val storeName: String?,
        val staffName: String?,
        val deposit: Int,
        val transactionId: String?,
        val createdAt: Date)