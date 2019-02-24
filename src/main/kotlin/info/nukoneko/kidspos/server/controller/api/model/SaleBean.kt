package info.nukoneko.kidspos.server.controller.api.model

data class SaleBean(val store_id: Int, val staff_id: Int, val items: List<ItemBean>, val deposit: Int)