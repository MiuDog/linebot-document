# Document 舊 SQLite／本機圖片遷移

遷移器會把舊 SQLite 可對應資料表複製到已由 Flyway 建好的 PostgreSQL，並把 asset 與 pending 圖片複製到 S3 相容儲存。來源資料庫及檔案永遠不刪除；預設只做 dry-run。

## 前置條件

1. 停止舊 App，建立 SQLite 與圖片根目錄快照；遷移只讀快照。
2. 啟動空白目標 PostgreSQL／Bucket，先讓正式 App 執行 Flyway，再停止 App。
3. 準備目標 DB／S3／company 設定與 Secret，Bucket 啟用 versioning。
4. `MIGRATION_ASSET_ROOT` 必須是舊 `asset.file_path` 與 `pending_image.staging_path` 的共同根目錄。

## Dry-run

以打包後的同版本 JAR 設定：

```text
SPRING_MAIN_WEB_APPLICATION_TYPE=none
MIGRATION_ENABLED=true
MIGRATION_EXECUTE=false
MIGRATION_LEGACY_DATABASE=<SQLite 快照絕對路徑>
MIGRATION_ASSET_ROOT=<舊圖片根目錄>
MIGRATION_REPORT_PATH=<安全的報告路徑>
DATABASE_URL=<目標 PostgreSQL JDBC URL>
DATABASE_USERNAME=<目標帳號>
COMPANY_ID=<公司 ID>
OBJECT_STORAGE_ENDPOINT=<S3 或 MinIO endpoint>
OBJECT_STORAGE_BUCKET=<目標 Bucket>
```

Secret 由 `/run/secrets` 或平台 Secret 注入。報告只含表筆數、發現／複製檔案數及錯誤代碼，不含圖片路徑、聊天來源、Token 或個資。先修正所有缺檔、絕對／逃逸路徑與 schema 問題，直到 dry-run 為 `SUCCEEDED`。

## 正式複製與重跑

1. 舊服務保持停止，確認快照未變。
2. 將 `MIGRATION_EXECUTE=true`，其餘設定不得變更。
3. 遷移器複製資料表，檔案使用內容雜湊 key；S3 metadata 的 hash／size 驗證成功後才更新 DB。
4. `storage_migration_ledger` 保存來源識別、目標 key、hash 與狀態；相同來源與 hash 重跑會跳過。
5. 失敗時 rollback DB 並補償本次目標物件；不刪來源。

## 切換 Gate

- 報告成功，目標表筆數不少於來源；ledger `VERIFIED` 數等於檔案數。
- 抽樣下載物件、查詢標籤與完整 ZIP 匯出後，SHA-256 與來源一致。
- 在 LINE sandbox 驗證收圖、pending、整組歸檔、搜尋與媒體下載。
- 保存舊快照到核准保存期屆滿；切換後回復舊服務前，必須先處理新系統新增資料，不能假設 SQLite 已同步。
