-- インデックスの作成
CREATE INDEX IF NOT EXISTS idx_item_barcode ON item(barcode);
CREATE INDEX IF NOT EXISTS idx_staff_barcode ON staff(barcode);
CREATE INDEX IF NOT EXISTS idx_sale_store_id ON sale(storeId);
CREATE INDEX IF NOT EXISTS idx_sale_staff_id ON sale(staffId);
CREATE INDEX IF NOT EXISTS idx_sale_created_at ON sale(createdAt);
CREATE INDEX IF NOT EXISTS idx_sale_detail_sale_id ON sale_detail(saleId);
CREATE INDEX IF NOT EXISTS idx_sale_detail_item_id ON sale_detail(itemId);

-- テーブル最適化
VACUUM;
ANALYZE;
