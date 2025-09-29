-- APKバージョン管理テーブル
CREATE TABLE IF NOT EXISTS apk_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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

-- インデックスの作成
CREATE INDEX idx_apk_versions_version_code ON apk_versions(version_code);
CREATE INDEX idx_apk_versions_is_active ON apk_versions(is_active);
CREATE INDEX idx_apk_versions_uploaded_at ON apk_versions(uploaded_at);
