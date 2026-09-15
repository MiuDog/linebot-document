# SPEC：Company Asset Contract

狀態：Approved，2026-09-02 核准。

## User Story

身為公司資產管理者，我希望在不取得原始碼的情況下，透過 LINE 收錄或受保護的匯入流程取得、修正、盤點與移轉公司圖片資產。

## Acceptance Criteria

1. 每套部署綁定唯一 `companyId`，不得在同一資料庫或 Bucket prefix 混入其他公司資料。
2. 公司資產包含已歸檔圖片、原始 MIME／大小／SHA-256、資產編號、公司資料夾代碼、標籤及來源稽核資料。
3. 每個匯入批次都有 manifest，包含 `schemaVersion`、`companyId`、`batchId`、物件 metadata 與 SHA-256。
4. LINE 收圖與管理匯入使用同一套驗證及儲存契約；任何來源都不得繞過 MIME、大小、magic bytes、hash 與路徑驗證。
5. 正式物件不可原地覆寫；修正 metadata 建立稽核版本，替換檔案建立新 object version。
6. OCI 映像、JAR、Git repository 與 Log 不得包含公司圖片、完整 LINE 訊息內容或可下載資產的長效憑證。
7. 管理／同步入口不對公網匿名開放，必須同時受網路邊界與管理憑證保護，並留下不含 Secret 的稽核紀錄。
8. 公司可完整匯出 manifest 與原始物件，且能在另一套同版本部署驗證後重新匯入。

## Out of Scope

- 多租戶共用部署。
- 圖片內容的著作權判定或自動授權。
- 將 LINE Token、資料庫密碼或 Tunnel Token 當作公司資產保存。

## Data Model Changes

- 資產增加 `company_id`、object key、object version、MIME、大小、SHA-256 與狀態。
- 新增 `asset_import_batch`、`asset_metadata_revision` 與 `asset_audit_event`。
- pending image 與正式 asset 都必須可追溯至來源事件，但不得保存不必要的訊息本文。

## API Changes

- 管理 API：建立匯入批次、上傳／註冊物件、驗證、提交、匯出 manifest、修正 metadata 及查詢稽核。
- 既有同步 API 改為相同批次契約，不再直接掃描任意主機路徑後靜默寫入。

## Edge Cases

- manifest 遺失物件、雜湊不符、重複資產編號或未知 schema 時整批拒絕。
- 批次提交中斷時，不得讓部分物件出現在正式查詢結果。
- 同一 LINE webhook 重送不得建立重複物件或資產資料列。

## Dependencies

無。
