-- Fix APK versions table ID to work with manual ID generation
-- Drop existing table and recreate without AUTOINCREMENT

-- Create backup table
CREATE TABLE apk_versions_backup AS SELECT * FROM apk_versions;

-- Drop existing table
DROP TABLE apk_versions;

-- Recreate table without AUTOINCREMENT
CREATE TABLE apk_versions (
    id INTEGER PRIMARY KEY,
    version VARCHAR(50) NOT NULL UNIQUE,
    version_code INTEGER NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    release_notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    uploaded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Restore data
INSERT INTO apk_versions SELECT * FROM apk_versions_backup;

-- Drop backup table
DROP TABLE apk_versions_backup;

-- Recreate indexes
CREATE INDEX idx_apk_versions_version_code ON apk_versions(version_code);
CREATE INDEX idx_apk_versions_is_active ON apk_versions(is_active);
CREATE INDEX idx_apk_versions_uploaded_at ON apk_versions(uploaded_at);