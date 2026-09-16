-- 資產索引：資料庫只存 metadata 與「指向」檔案的相對路徑，圖片本體留在磁碟
CREATE TABLE IF NOT EXISTS asset (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id    TEXT    NOT NULL UNIQUE,
    share_token   TEXT    NOT NULL UNIQUE,
    source_type   TEXT,
    source_id     TEXT,
    uploader_id   TEXT,
    file_path     TEXT    NOT NULL,
    content_type  TEXT,
    file_size     INTEGER,
    created_at    TEXT    NOT NULL
);

-- 檔案身分資料：用檔案系統識別碼優先追蹤 Explorer 改名／移動，雜湊作為跨平台備援
CREATE TABLE IF NOT EXISTS asset_file_identity (
    asset_id       INTEGER PRIMARY KEY,
    file_key       TEXT,
    content_hash   TEXT    NOT NULL,
    file_size      INTEGER NOT NULL,
    last_modified  INTEGER NOT NULL,
    updated_at     TEXT    NOT NULL,
    FOREIGN KEY (asset_id) REFERENCES asset (id) ON DELETE CASCADE
);

-- 暫時離開資產根目錄的圖片只標記遺失，保留身分資料以便移回後復原。
DROP TABLE IF EXISTS asset_file_missing;

CREATE TABLE IF NOT EXISTS tag (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS asset_tag (
    asset_id INTEGER NOT NULL,
    tag_id   INTEGER NOT NULL,
    PRIMARY KEY (asset_id, tag_id),
    FOREIGN KEY (asset_id) REFERENCES asset (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_asset_source ON asset (source_id);
CREATE INDEX IF NOT EXISTS idx_asset_tag_tag ON asset_tag (tag_id);

-- 尚未確認歸檔的 LINE 圖片。圖片本體暫存在 .pending，確認後才建立 asset。
CREATE TABLE IF NOT EXISTS pending_image (
    message_id    TEXT PRIMARY KEY,
    image_set_id  TEXT    NOT NULL,
    image_index   INTEGER NOT NULL CHECK (image_index > 0),
    image_total   INTEGER NOT NULL CHECK (image_total > 0),
    source_type   TEXT,
    source_id     TEXT    NOT NULL,
    uploader_id   TEXT,
    staging_path  TEXT    NOT NULL UNIQUE,
    content_type  TEXT,
    file_size     INTEGER NOT NULL,
    received_at   TEXT    NOT NULL,
    UNIQUE (source_id, image_set_id, image_index)
);

CREATE INDEX IF NOT EXISTS idx_pending_image_set
    ON pending_image (source_id, image_set_id, image_index);

-- 每張 LINE 圖片的抓取結果，用於完整回報成功、重複與失敗。
CREATE TABLE IF NOT EXISTS pending_image_fetch (
    source_id     TEXT    NOT NULL,
    image_set_id  TEXT    NOT NULL,
    image_index   INTEGER NOT NULL CHECK (image_index > 0),
    image_total   INTEGER NOT NULL CHECK (image_total > 0),
    message_id    TEXT    NOT NULL,
    status        TEXT    NOT NULL CHECK (status IN ('FETCHED', 'DUPLICATE', 'FAILED')),
    updated_at    TEXT    NOT NULL,
    PRIMARY KEY (source_id, image_set_id, image_index)
);

CREATE INDEX IF NOT EXISTS idx_pending_image_fetch_message
    ON pending_image_fetch (message_id);

CREATE TABLE IF NOT EXISTS pending_archive_confirmation (
    source_id     TEXT NOT NULL,
    requester_id  TEXT NOT NULL,
    image_set_id  TEXT NOT NULL,
    archive_date  TEXT NOT NULL,
    requested_at  TEXT NOT NULL,
    PRIMARY KEY (source_id, requester_id)
);
