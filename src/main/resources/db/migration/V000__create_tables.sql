-- Create tables for KidsPOS Server
-- This migration creates the initial database schema

-- Store table
CREATE TABLE IF NOT EXISTS store
(
    id
                INTEGER
        PRIMARY
            KEY
        AUTOINCREMENT,
    name
                VARCHAR(255) NOT NULL,
    printer_uri VARCHAR(255)
);


-- Item table
CREATE TABLE IF NOT EXISTS item
(
    id
            INTEGER
        PRIMARY
            KEY
        AUTOINCREMENT,
    barcode
            VARCHAR(255) NOT NULL,
    name    VARCHAR(255) NOT NULL,
    price   INTEGER      NOT NULL,
    enabled BOOLEAN DEFAULT 1
);

-- Sale table
CREATE TABLE IF NOT EXISTS sale
(
    id
        INTEGER
        PRIMARY
            KEY
        AUTOINCREMENT,
    store_id
        INTEGER,
    amount
        INTEGER
        NOT
            NULL,
    deposit
        INTEGER
        NOT
            NULL,
    change_amount
        INTEGER
        NOT
            NULL,
    created_at
        TIMESTAMP
        DEFAULT
            CURRENT_TIMESTAMP,
    FOREIGN
        KEY
        (
         store_id
            ) REFERENCES store
        (
         id
            )
);

-- Sale detail table
CREATE TABLE IF NOT EXISTS sale_detail
(
    id
             INTEGER
        PRIMARY
            KEY
        AUTOINCREMENT,
    sale_id
             INTEGER
                     NOT
                         NULL,
    item_id
             INTEGER,
    item_name
             VARCHAR(255),
    price    INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY
        (
         sale_id
            ) REFERENCES sale
        (
         id
            ),
    FOREIGN KEY
        (
         item_id
            ) REFERENCES item
        (
         id
            )
);

-- Setting table
CREATE TABLE IF NOT EXISTS setting
(
    id
               INTEGER
        PRIMARY
            KEY
        AUTOINCREMENT,
    key
               VARCHAR(255) NOT NULL UNIQUE,
    value      TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default settings
INSERT
    OR IGNORE
INTO setting (key, value)
VALUES ('system.version', '1.0.0');
INSERT
    OR IGNORE
INTO setting (key, value)
VALUES ('receipt.shop_name', 'KidsPOS Shop');
INSERT
    OR IGNORE
INTO setting (key, value)
VALUES ('receipt.footer_message', 'Thank you for your purchase!');
