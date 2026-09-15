# SPEC：Portable Storage

狀態：Approved，2026-09-02 核准。

## User Story

身為維護者，我希望雲端與本地 Docker 使用相同的 PostgreSQL 與 S3 相容儲存契約，避免容器重建造成圖片與索引遺失。

## Acceptance Criteria

1. 結構化資產索引、pending 狀態、標籤及稽核使用 PostgreSQL；圖片檔使用 S3 相容物件儲存。
2. App 容器檔案系統僅可保存可重建的暫存檔，程序重啟後不得依賴容器內舊檔案。
3. 本地 Compose 啟動 PostgreSQL 與 S3 相容服務；雲端使用外部連線資訊而不更改應用程式碼。
4. 所有 object key 由程式產生並限制在 `companies/{companyId}/`，不得接受使用者提供完整 key 或 URL。
5. 歸檔使用 staging object、資料庫 transaction 與補償流程；整組圖片成功後才對查詢可見。
6. 提供可重跑的 SQLite＋圖片資料夾遷移工具；先驗證、再複製、核對數量與 SHA-256，成功後才切換服務。
7. 遷移工具不刪除來源資料，並產生不含個資與 Secret 的結果報告。
8. Schema 由版本化 migration 管理，禁止依賴 `spring.sql.init.mode=always` 修改正式資料庫。

## Out of Scope

- 跨公司共享物件去重。
- CDN 作為真實資料來源。
- 自動刪除舊 SQLite 或舊圖片目錄。

## Data Model Changes

- SQLite schema 轉為 PostgreSQL migration，保留資產編號、標籤與事件冪等語意。
- `file_path` 改為 storage scope、object key、version、MIME、大小與 SHA-256，不保存主機相對或絕對路徑。
- 新增 migration ledger，記錄來源識別、目標識別與校驗結果。

## API Changes

- 媒體 API 改由儲存介面串流物件或產生短效簽名 URL。
- 不公開 Bucket 名稱、長效 object URL 或底層憑證。

## Edge Cases

- PostgreSQL transaction rollback 後必須清理由該次操作建立的 staging object。
- S3 timeout 可重試，但不得重複消耗資產流水號。
- 遷移遇到 DB 紀錄缺檔、孤兒檔案或 hash 不符時必須停止切換並列出問題。

## Dependencies

`company-asset-contract`。
