package info.nukoneko.kidspos.server.util

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * SQLiteデータベースの最適化を行うユーティリティクラス
 */
@Component
class DatabaseOptimizer @Autowired constructor(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(DatabaseOptimizer::class.java)

    /**
     * アプリケーション起動時にインデックスを作成
     */
    @PostConstruct
    fun createIndices() {
        logger.info("データベースインデックスを作成しています...")
        try {
            // 商品テーブルのバーコードにインデックスを作成
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_item_barcode ON item(barcode)")
            
            // スタッフテーブルのバーコードにインデックスを作成
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_staff_barcode ON staff(barcode)")
            
            // 売上テーブルの頻繁に検索されるカラムにインデックスを作成
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sale_store_id ON sale(storeId)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sale_staff_id ON sale(staffId)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sale_created_at ON sale(createdAt)")
            
            // 売上詳細テーブルの外部キーにインデックスを作成
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sale_detail_sale_id ON sale_detail(saleId)")
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sale_detail_item_id ON sale_detail(itemId)")
            
            logger.info("データベースインデックスの作成が完了しました")
        } catch (e: Exception) {
            logger.error("データベースインデックスの作成中にエラーが発生しました", e)
        }
    }

    /**
     * 毎日深夜3時にデータベースの最適化を実行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    fun optimizeDatabase() {
        logger.info("データベースの最適化を開始します...")
        try {
            // ANALYZE: 統計情報の更新
            jdbcTemplate.execute("ANALYZE")
            
            // VACUUM: 未使用領域の回収、データベースファイルの縮小
            jdbcTemplate.execute("VACUUM")
            
            logger.info("データベースの最適化が完了しました")
        } catch (e: Exception) {
            logger.error("データベース最適化中にエラーが発生しました", e)
        }
    }
}
