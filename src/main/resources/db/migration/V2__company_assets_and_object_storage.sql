-- 公司圖片資產批次、metadata revision、稽核與物件儲存 metadata。
-- Flyway 規則：https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway

CREATE TABLE asset_import_batch (
    id            BIGSERIAL PRIMARY KEY,
    company_id    TEXT NOT NULL,
    batch_id      TEXT NOT NULL,
    schema_version TEXT NOT NULL,
    manifest_hash TEXT NOT NULL,
    status        TEXT NOT NULL CHECK (status IN ('STAGED', 'VALIDATED', 'COMMITTED', 'REJECTED')),
    created_by    TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (CAST(CURRENT_TIMESTAMP AS TEXT)),
    committed_at  TEXT,
    UNIQUE (company_id, batch_id),
    UNIQUE (company_id, manifest_hash)
);

CREATE TABLE asset_import_object (
    id                BIGSERIAL PRIMARY KEY,
    import_batch_id   BIGINT NOT NULL,
    asset_external_id TEXT NOT NULL,
    asset_code        TEXT NOT NULL,
    folder_code       TEXT,
    tags_json         TEXT NOT NULL DEFAULT '[]',
    source_type       TEXT,
    source_id         TEXT,
    source_event_id   TEXT,
    file_name         TEXT NOT NULL,
    object_key        TEXT NOT NULL,
    object_version    TEXT,
    content_type      TEXT NOT NULL,
    file_size         BIGINT NOT NULL CHECK (file_size > 0),
    content_hash      TEXT NOT NULL,
    UNIQUE (import_batch_id, asset_external_id),
    UNIQUE (import_batch_id, object_key),
    FOREIGN KEY (import_batch_id) REFERENCES asset_import_batch (id) ON DELETE CASCADE
);

CREATE TABLE asset_metadata_revision (
    id            BIGSERIAL PRIMARY KEY,
    asset_id      BIGINT NOT NULL,
    revision      INTEGER NOT NULL CHECK (revision > 0),
    folder_code   TEXT,
    tags_json     TEXT NOT NULL DEFAULT '[]',
    actor         TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (CAST(CURRENT_TIMESTAMP AS TEXT)),
    UNIQUE (asset_id, revision),
    FOREIGN KEY (asset_id) REFERENCES asset (id) ON DELETE CASCADE
);

CREATE TABLE asset_audit_event (
    id           BIGSERIAL PRIMARY KEY,
    company_id   TEXT NOT NULL,
    asset_id     BIGINT,
    batch_id     BIGINT,
    action       TEXT NOT NULL,
    actor        TEXT NOT NULL,
    summary_json TEXT NOT NULL DEFAULT '{}',
    created_at   TEXT NOT NULL DEFAULT (CAST(CURRENT_TIMESTAMP AS TEXT)),
    FOREIGN KEY (asset_id) REFERENCES asset (id) ON DELETE SET NULL,
    FOREIGN KEY (batch_id) REFERENCES asset_import_batch (id) ON DELETE SET NULL
);

CREATE TABLE storage_migration_ledger (
    id              BIGSERIAL PRIMARY KEY,
    company_id      TEXT NOT NULL,
    source_type     TEXT NOT NULL,
    source_identity TEXT NOT NULL,
    target_identity TEXT,
    content_hash    TEXT,
    status          TEXT NOT NULL CHECK (status IN ('DISCOVERED', 'COPIED', 'VERIFIED', 'FAILED')),
    error_code      TEXT,
    updated_at      TEXT NOT NULL DEFAULT (CAST(CURRENT_TIMESTAMP AS TEXT)),
    UNIQUE (company_id, source_type, source_identity)
);

ALTER TABLE asset
    ADD COLUMN company_id TEXT NOT NULL DEFAULT 'legacy',
    ADD COLUMN object_key TEXT,
    ADD COLUMN object_version TEXT,
    ADD COLUMN content_hash TEXT,
    ADD COLUMN import_batch_id BIGINT,
    ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'RETIRED')),
    ADD CONSTRAINT fk_asset_import_batch
        FOREIGN KEY (import_batch_id) REFERENCES asset_import_batch (id) ON DELETE RESTRICT;

ALTER TABLE pending_image
    ADD COLUMN staging_object_key TEXT;

CREATE INDEX idx_asset_company_object ON asset (company_id, object_key);
CREATE INDEX idx_asset_import_batch_status ON asset_import_batch (company_id, status, created_at);
