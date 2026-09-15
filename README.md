# LINE Bot Document

文書圖片資產機器人目前只提供兩種部署：本地 Docker Compose 與標準 Kubernetes。Windows 桌面 App、安裝程式、內嵌 Tunnel 及 SQLite 正式執行模式均已退役。

## 架構

Document 負責 LINE 圖片歸檔、查詢與資產匯入／匯出；Commercial 負責報價草稿、計價與 XLSX／PDF 產生。Document 不提供報價格式編輯器；兩個專案獨立部署，不會自動共用資料或同步圖片。

- Java 25／Spring Boot 無介面服務，固定容器埠 `8089`。
- PostgreSQL 保存圖片索引、標籤、匯入批次與稽核；Flyway 管理 schema。
- S3 相容物件儲存保存原始圖片與 staging 物件。
- 可攜式 ZIP＋manifest 是公司資產的完整匯入／匯出格式。
- Cloudflare Tunnel 是獨立容器或平台元件，App 不管理其程序或 Token。

## Windows 客戶操作

1. 安裝並開啟 Docker Desktop，等候畫面顯示 Engine running。
2. 雙擊 `linebot.cmd`，選擇「首次設定／修改設定」。
3. 依畫面填入公司代碼、Cloudflare 公開網址與 LINE 憑證；資料庫、物件儲存及管理用 Secret 會自動安全產生。
4. 設定顯示通過後，選擇「啟動服務」。控制台會等待 PostgreSQL、物件儲存與 App 全部健康才顯示完成。
5. 遇到收不到訊息、圖片無法顯示或 Tunnel 異常時，先選擇「連線與環境診斷」。診斷會分開檢查 Docker、設定、本機服務、公開網域 DNS／HTTPS 與 LINE API，並在失敗階段列出原因和解法。

「停止服務」不會刪除資料卷；不要在 Docker Desktop 手動刪除 database-data 或 object-storage-data。需要交付工程人員時，選擇「查看 App 紀錄」，記下問題發生時間與第一個 `ERROR` 的 `requestId`，不要傳送 Token 或 Secret 檔案。

## Docker Compose 部署

命令在本專案根目錄執行。需要 Docker Engine／Docker Desktop（Linux containers）與 Compose v2；Java 與 Maven 在映像內建置，主機不需另外安裝。

### 1. 設定與 Secret

Windows 可先執行 `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/prepare-local-settings.ps1`，保留既有設定、移轉既有憑證並自動產生缺少的內部密碼。再雙擊 `linebot.cmd` →「首次設定／修改設定」，補上 LINE 與 HTTPS／Tunnel 資料。空白 Secret 會提示輸入，工具不會代為申請外部憑證。請勿將 Secret 貼到聊天或提交 Git。

首次設定才複製 `.env.example` 為 `.env`（PowerShell：`Copy-Item .env.example .env`；Linux／macOS：`cp .env.example .env`）。已有 `.env` 時直接編輯。

- 設定 `COMPANY_ID`、`PUBLIC_BASE_URL`、獨立 `OBJECT_STORAGE_BUCKET`；預設主機埠 `APP_PORT=8089`，容器埠固定 `8089`。
- 依 [Secret 清單](secrets/README.md) 建立檔案，包括 Compose 會掛載的 `assets-sync-token`。目前 Compose 設定 `ASSETS_SYNC_ENABLED=false`，不會自動啟用舊同步功能。
- `.env` 不放密碼。Linux 上須讓容器 UID `10001` 可讀掛載的 Secret，並限制其他使用者存取。Windows 可使用 `linebot.cmd` 自動建立內部 Secret。

### 2. 建置與健康檢查

```text
docker compose config --quiet
docker compose up --build --wait
docker compose ps
curl --fail http://127.0.0.1:8089/readyz
```

PowerShell 的 `curl` 若為別名，使用 `curl.exe`。首次啟動需下載映像及 Maven 相依套件。`database`、`object-storage`、`app` 應為 healthy；一次性 `object-storage-init` 正常狀態為 Exited (0)。資料保存在各自的 PostgreSQL／MinIO 命名資料卷，資料庫及 MinIO 不對主機開埠。

### 3. Tunnel 與 Webhook

使用內建 Compose Tunnel 時，建立 `secrets/cloudflared-token`，在 Cloudflare 將 Public Hostname 路由至 `http://app:8089`，執行：

```text
docker compose --profile tunnel up --build --wait
docker compose logs --tail 100 tunnel
```

`TUNNEL_ENABLED=true` 供 Windows 控制台使用；手動 Compose 必須指定 `--profile tunnel`。若 Connector 在主機執行，可路由到 `http://127.0.0.1:8089`；其他容器須依其網路設定目的地。

LINE Webhook 設為 `{PUBLIC_BASE_URL}/callback`，啟用 Webhook 並 Verify。管理 API 僅限受保護網路。最後以測試圖片驗證 LINE 歸檔、查詢與圖片開啟；`readyz` 不代表 LINE 端到端驗收通過。

### 4. 維運

```text
docker compose logs --tail 100 app
docker compose --profile tunnel stop
docker compose --profile tunnel down
```

`down` 保留命名資料卷；**`down -v` 會刪除資料，不可作為一般停止方式。** 更新前備份 PostgreSQL 與 Bucket。原始碼部署重新執行 `up --build --wait`；發布映像則設定 `APP_IMAGE=repository@sha256:...`，執行 `docker compose pull app`、`docker compose up --no-build -d --wait app`。schema 相容性與回復方式見 [部署 Runbook](docs/deployment-runbook.md)。

### Windows 維運入口

1. 建議先以 `linebot.cmd Validate` 執行與客戶控制台相同的完整前置驗證。
2. 只有自動化部署才手動複製 `.env.example` 與建立 [`secrets/README.md`](secrets/README.md) 所列 Secret。
3. 執行 `docker compose up --build --wait`。
4. 確認 `http://127.0.0.1:8089/readyz` 回應成功。
5. 需要 Cloudflare Tunnel 時改執行 `docker compose --profile tunnel up --build --wait`。

文書與商用專案可同時啟動；商用機預設使用 `127.0.0.1:8088`，兩者不得共用資料庫、Bucket 或 Tunnel Token。

## 資產維護

以檔案操作為準：完整圖片資產使用 ZIP＋manifest 匯入／匯出；CSV 適合表格資料，不能替代圖片二進位檔與完整資產 manifest。Commercial 的品項 CSV 與報價 XLSX 範本需在 Commercial 維護。

圖片可由 LINE 正常流程產生，也可透過受保護管理 API 匯入 manifest 批次。每次匯入經 stage、validation、commit 後才可見；完整批次可匯出後在另一套部署重匯入。

資產所有權、來源證明、版本、備份與離場交付請閱讀 [`docs/company-asset-governance.md`](docs/company-asset-governance.md)。部署操作見 [`docs/deployment-runbook.md`](docs/deployment-runbook.md)。

## 開發與驗證

公司本地資產包請放在 Git／Docker 忽略的 `company-assets-local/`，並另外保存公司控制的備份；忽略規則不會移除 Git 歷史與既有舊映像內的檔案。

新版 Docker 的真實 LINE 對話與生成結果截圖尚待錄製及上傳。範例只使用可公開的測試資料，不以模擬對話或舊桌面版截圖代替實機驗收。

```text
mvnw.cmd clean verify
docker compose config --quiet
kubectl kustomize k8s/base
```

正式部署必須使用 CI 產出的映像 digest，不使用 `latest`。Kubernetes 範本位於 [`k8s/base`](k8s/base)。

## 文件

- [`docs/deployment-runbook.md`](docs/deployment-runbook.md)：本地與雲端部署、升級、回復、備份。
- [`docs/company-asset-governance.md`](docs/company-asset-governance.md)：公司圖片資產治理。
- [`docs/migration-runbook.md`](docs/migration-runbook.md)：舊 SQLite／本機圖片切換。
- 根目錄 `SPEC-*.md`：已核准架構契約。
