-- Database indexes for performance optimization
-- Created for KidsPOS Server query optimization

-- Index on item barcode for fast lookups
CREATE INDEX IF NOT EXISTS idx_item_barcode ON item (barcode);

-- Index on item price for range queries
CREATE INDEX IF NOT EXISTS idx_item_price ON item (price);

-- Index on sale store_id for filtering
CREATE INDEX IF NOT EXISTS idx_sale_store_id ON sale (store_id);

-- Index on sale created_at for date range queries
CREATE INDEX IF NOT EXISTS idx_sale_created_at ON sale (created_at);

-- Composite index for sale_detail lookups
CREATE INDEX IF NOT EXISTS idx_sale_detail_sale_id ON sale_detail (sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_detail_item_id ON sale_detail (item_id);


-- Index on setting key for configuration lookups
CREATE INDEX IF NOT EXISTS idx_setting_key ON setting (key);
