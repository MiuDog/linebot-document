# 公司圖片資產取得與維護制度（Document）

## 先決策：程式與資產分離，不等於映像不可逆向

圖片不放進 Git 或 OCI 映像，可避免公司資料跟著程式發布；但若把 Java 容器映像交付到客戶控制的主機或 Kubernetes，客戶可取得 bytecode，技術上仍可能反編譯。若「程式碼不可交付」是不可妥協條件，應採供應商管理的 SaaS／Kubernetes 帳號，只向公司提供 API、管理介面、稽核與完整資料匯出。公司自管雲端或本地 Docker 則要用授權契約、存取控制與必要混淆保護 IP，不能只依靠容器格式。

## 所有權與責任

| 項目 | 建議所有人 | 維護責任 |
|---|---|---|
| 原始圖片、資產代碼、標籤、來源紀錄 | 公司 | 公司確認著作權、營業秘密與個資處理依據 |
| 匯入批次、稽核、衍生索引與備份 | 公司 | 公司決定保存期；維運方執行備份與還原 |
| LINE Channel、網域、TLS 與 Tunnel 身分 | 公司 | 公司保管主帳號；維運方只取得最小權限 |
| 應用程式原始碼與通用演算法 | 軟體供應方（依契約） | 供應方維護並交付映像 digest／SBOM |
| PostgreSQL／Bucket | 優先由公司持有 | 執行帳號只存取該公司物件前綴 |

契約至少應約定資料所有權、用途、保存期、備份、資安事件通報、離場格式、刪除證明、RTO／RPO，以及不得以圖片訓練模型或轉作他用。

## 圖片如何取得

### LINE 正常流程

公司授權的群組／使用者上傳圖片時，系統保留 LINE 事件識別、來源類型、來源 ID、上傳者、接收時間與檔案完整性資料。圖片先進 staging；只有完成分類／確認後才成為正式資產。不要把聊天平台當成唯一母檔，重要原圖仍應由公司文件管理系統保存。

### 批次匯入

舊系統、掃描或外部提供的圖片以 manifest 批次匯入。每個物件至少要有公司內穩定的 `assetExternalId`、代碼、標籤、來源證明、MIME、size 與 SHA-256：

```json
{
  "schemaVersion": "1",
  "companyId": "company-id",
  "batchId": "2026-09-warehouse-a",
  "objects": [
    {
      "assetExternalId": "asset-000001",
      "assetCode": "ZD12345",
      "folderCode": "ZD12345",
      "tags": ["外牆", "完工"],
      "sourceType": "legacy-import",
      "sourceId": "warehouse-a",
      "sourceEventId": "row-000001",
      "fileName": "asset-000001.jpg",
      "contentType": "image/jpeg",
      "size": 12345,
      "sha256": "64-lowercase-hex"
    }
  ]
}
```

## 匯入與匯出生命週期

管理 API 同時受網路邊界與 `X-Admin-Token` 保護，並以 `X-Admin-Actor` 留下操作者代號：

1. `POST /api/admin/asset-imports/stages`：multipart 上傳 `manifest` 與全部 `files`。
2. `POST /api/admin/asset-imports/{batchId}/validation`：核對 company、ID、MIME、大小、magic header 與 SHA-256，提升為正式物件。
3. `POST /api/admin/asset-imports/{batchId}/commit`：在單一資料庫交易中建立資產、標籤、metadata revision 與稽核。
4. `GET /api/admin/asset-imports`：列出批次及狀態。
5. `GET /api/admin/asset-imports/{batchId}/export`：匯出完整 ZIP＋manifest，可在另一套部署重新匯入。

相同 `batchId`、`assetExternalId` 或來源事件重送必須保持冪等；任何缺檔或 hash 不符都停止整批 commit，不允許部分可見。

## 日常維護

- 標籤或代碼變更要新增 metadata revision，不覆寫來源證明。
- 保留需求單、提供者、核准者、manifest hash、匯入結果及抽樣驗收證據。
- 每季做 PostgreSQL＋Bucket 還原演練；定期抽樣比對資料庫 hash 與物件 hash。
- Production App 只取得 `companies/{companyId}/` 前綴的最小權限；備份與執行帳號分離。
- 敏感圖片設定保存期、法規封存與刪除核准；刪除要涵蓋物件版本、備份到期及稽核證據。

## 雲端服務與離場

建議由公司帳號持有 PostgreSQL、Bucket、KMS key、網域與 LINE Channel；供應方只在隔離工作負載中使用短效身分。若供應方完全不應看到明文圖片，需在公司控制的環境解密，這可能代表公司必須控制執行環境並取得映像；「程式完全不交付」與「供應方完全看不到資料」無法同時只靠技術達成，必須明確選擇信任邊界。

離場時公司取得 PostgreSQL 邏輯備份、完整 ZIP 匯出、物件清單／SHA-256 與稽核；在隔離環境完成重匯入驗證後撤銷服務帳號，並依契約取得供應方端刪除證明。
