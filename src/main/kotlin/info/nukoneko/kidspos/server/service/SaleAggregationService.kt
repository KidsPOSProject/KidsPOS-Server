package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.DailySalesSummary
import info.nukoneko.kidspos.server.controller.dto.response.ItemSalesSummary
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportDetailData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportSummary
import info.nukoneko.kidspos.server.controller.dto.response.SaleSummaryResponse
import info.nukoneko.kidspos.server.controller.dto.response.StoreSalesSummary
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 売上の集計を一箇所に集める。
 *
 * PDF・Excel・画面表示のいずれも同じ集計結果を使うことで、出力ごとに数字がずれるのを防ぐ。
 */
@Service
@Transactional(readOnly = true)
class SaleAggregationService(
    private val saleRepository: SaleRepository,
    private val saleDetailRepository: SaleDetailRepository,
    private val itemRepository: ItemRepository,
    private val storeRepository: StoreRepository,
) {
    data class Aggregation(
        val startDate: Date,
        val endDate: Date,
        val sales: List<SaleReportData>,
        val summary: SaleReportSummary,
    )

    /**
     * 当日実績と前日実績の比較。
     *
     * @property changeRatio 前日比。前日に実績が無いときは比較できないため null。
     */
    data class DailyComparison(
        val date: Date,
        val totalAmount: Int,
        val salesCount: Int,
        val averageAmount: Int,
        val previousAmount: Int,
        val previousSalesCount: Int,
        val changeRatio: Double?,
    )

    data class SaleDetailView(
        val sale: SaleEntity,
        val storeName: String,
        val details: List<SaleReportDetailData>,
    ) {
        val detailAmount: Int get() = details.sumOf { it.subtotal }
    }

    fun aggregate(
        startDate: Date,
        endDate: Date,
        storeId: Int? = null,
    ): Aggregation {
        val from = ReportPeriod.startOfDay(startDate)
        val to = ReportPeriod.endOfDay(endDate)

        val sales =
            saleRepository
                .findByDateRange(from, to)
                .filter { storeId == null || it.storeId == storeId }

        val details = loadDetails(sales.map { it.id })
        val itemNames = loadItemNames(details.values.flatten().map { it.itemId })
        val storeNames = loadStoreNames(sales.map { it.storeId })

        val reportData =
            sales.map { sale ->
                SaleReportData(
                    saleId = sale.id,
                    storeId = sale.storeId,
                    storeName = storeNames[sale.storeId] ?: UNKNOWN_STORE,
                    quantity = sale.quantity,
                    amount = sale.amount,
                    createdAt = sale.createdAt,
                    details =
                        details[sale.id].orEmpty().map { detail ->
                            SaleReportDetailData(
                                itemId = detail.itemId,
                                itemName = itemNames[detail.itemId] ?: UNKNOWN_ITEM,
                                price = detail.price,
                                quantity = detail.quantity,
                                subtotal = detail.price * detail.quantity,
                            )
                        },
                )
            }

        return Aggregation(
            startDate = from,
            endDate = to,
            sales = reportData,
            summary = summarize(reportData, from, to),
        )
    }

    fun summaryResponse(
        startDate: Date,
        endDate: Date,
        storeId: Int? = null,
    ): SaleSummaryResponse {
        val aggregation = aggregate(startDate, endDate, storeId)
        val summary = aggregation.summary

        return SaleSummaryResponse(
            startDate = aggregation.startDate,
            endDate = aggregation.endDate,
            totalSales = summary.totalSales,
            totalItemCount = summary.totalItemCount,
            totalAmount = summary.totalAmount,
            averageAmount = summary.averageAmount.toInt(),
            stores = byStore(aggregation.sales),
            items = byItem(aggregation.sales),
            daily = byDay(aggregation.sales),
        )
    }

    fun dailyComparison(date: Date): DailyComparison {
        val today = aggregate(date, date).summary
        val previousDate = ReportPeriod.previousDay(date)
        val previous = aggregate(previousDate, previousDate).summary

        return DailyComparison(
            date = ReportPeriod.startOfDay(date),
            totalAmount = today.totalAmount,
            salesCount = today.totalSales,
            averageAmount = today.averageAmount.toInt(),
            previousAmount = previous.totalAmount,
            previousSalesCount = previous.totalSales,
            changeRatio =
                if (previous.totalAmount > 0) {
                    (today.totalAmount - previous.totalAmount).toDouble() / previous.totalAmount
                } else {
                    null
                },
        )
    }

    fun findSaleDetail(saleId: Int): SaleDetailView? {
        val sale = saleRepository.findById(saleId).orElse(null) ?: return null
        val details = saleDetailRepository.findBySaleIdIn(listOf(saleId))
        val itemNames = loadItemNames(details.map { it.itemId })
        val storeNames = loadStoreNames(listOf(sale.storeId))

        return SaleDetailView(
            sale = sale,
            storeName = storeNames[sale.storeId] ?: UNKNOWN_STORE,
            details =
                details.map { detail ->
                    SaleReportDetailData(
                        itemId = detail.itemId,
                        itemName = itemNames[detail.itemId] ?: UNKNOWN_ITEM,
                        price = detail.price,
                        quantity = detail.quantity,
                        subtotal = detail.price * detail.quantity,
                    )
                },
        )
    }

    private fun summarize(
        reportData: List<SaleReportData>,
        startDate: Date,
        endDate: Date,
    ): SaleReportSummary {
        val totalSales = reportData.size
        val totalAmount = reportData.sumOf { it.amount }

        return SaleReportSummary(
            totalSales = totalSales,
            totalItemCount = reportData.sumOf { sale -> sale.details.sumOf { it.quantity } },
            totalAmount = totalAmount,
            averageAmount = if (totalSales > 0) totalAmount.toDouble() / totalSales else 0.0,
            startDate = startDate,
            endDate = endDate,
        )
    }

    private fun byStore(sales: List<SaleReportData>): List<StoreSalesSummary> =
        sales
            .groupBy { it.storeId }
            .map { (storeId, group) ->
                StoreSalesSummary(
                    storeId = storeId,
                    storeName = group.first().storeName,
                    salesCount = group.size,
                    itemCount = group.sumOf { sale -> sale.details.sumOf { it.quantity } },
                    amount = group.sumOf { it.amount },
                )
            }.sortedByDescending { it.amount }

    private fun byItem(sales: List<SaleReportData>): List<ItemSalesSummary> =
        sales
            .flatMap { it.details }
            .groupBy { it.itemId }
            .map { (itemId, group) ->
                ItemSalesSummary(
                    itemId = itemId,
                    itemName = group.first().itemName,
                    unitPrice = group.first().price,
                    quantity = group.sumOf { it.quantity },
                    amount = group.sumOf { it.subtotal },
                )
            }.sortedByDescending { it.amount }

    private fun byDay(sales: List<SaleReportData>): List<DailySalesSummary> {
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
        return sales
            .groupBy { dayFormat.format(it.createdAt) }
            .map { (date, group) ->
                DailySalesSummary(
                    date = date,
                    salesCount = group.size,
                    itemCount = group.sumOf { sale -> sale.details.sumOf { it.quantity } },
                    amount = group.sumOf { it.amount },
                )
            }.sortedBy { it.date }
    }

    private fun loadDetails(saleIds: List<Int>) =
        if (saleIds.isEmpty()) {
            emptyMap()
        } else {
            saleDetailRepository.findBySaleIdIn(saleIds).groupBy { it.saleId }
        }

    private fun loadItemNames(itemIds: List<Int>) =
        if (itemIds.isEmpty()) {
            emptyMap()
        } else {
            itemRepository.findAllById(itemIds.distinct()).associate { it.id to it.name }
        }

    private fun loadStoreNames(storeIds: List<Int>) =
        if (storeIds.isEmpty()) {
            emptyMap()
        } else {
            storeRepository.findAllById(storeIds.distinct()).associate { it.id to it.name }
        }

    private companion object {
        const val UNKNOWN_STORE = "不明な店舗"
        const val UNKNOWN_ITEM = "不明な商品"
    }
}
