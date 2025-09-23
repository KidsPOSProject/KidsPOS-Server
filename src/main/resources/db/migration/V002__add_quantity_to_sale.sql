-- Add quantity column to sale table
-- This column stores the total quantity of items in the sale
ALTER TABLE sale
    ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0;