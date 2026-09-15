# Document 部署與維運 Runbook

## 本地 Docker

### 首次啟動

1. 安裝 Docker Engine／Docker Desktop 與 Compose v2。
2. 複製 `.env.example` 為 `.env`，設定公司 ID、公開 HTTPS 網址與 Bucket；`.env` 不放密碼。
3. 依 `secrets/README.md` 建立 Secret 檔案，限制只有部署帳號可讀。
4. 執行 `docker compose config --quiet`。
5. 執行 `docker compose up --build --wait`。
6. 確認 database、object-storage、app 均健康，且 `http://127.0.0.1:8089/readyz` 成功。
7. 匯入一個測試批次，驗證上傳、標籤查詢、圖片下載及完整 ZIP 匯出。
8. 將 LINE Webhook 設為 `{PUBLIC_BASE_URL}/callback` 並按 Verify。

Compose 不暴露 PostgreSQL 或 MinIO 埠；App 只綁 loopback。需要 Tunnel 時使用 `docker compose --profile tunnel up --build --wait`。Document 與 Commercial 必須使用不同的 Compose project、埠、資料庫、Bucket 與 Tunnel Token。

### 備份、還原與資產匯出

- PostgreSQL：每日 `pg_dump --format=custom`，備份加密後移至不同故障域；每季執行完整 `pg_restore`。
- MinIO／S3：啟用 versioning 與跨儲存複寫；包含 `companies/{companyId}/` 的所有版本及 metadata。
- 業務可攜副本：定期以 `/api/admin/asset-imports/{batchId}/export` 保存完整 ZIP＋manifest，不以此取代資料庫與 Bucket 備份。
- 還原後抽查資料列數、物件數、SHA-256、標籤與 LINE 查詢；只有驗證成功才更新流量。

### 升級與回復

1. 檢查 release migration 相容性並完成 PostgreSQL／Bucket 備份。
2. 在 staging 重匯入代表性批次，驗證 Flyway、查詢、匯出與 LINE sandbox。
3. 將 `APP_IMAGE` 改成不可變版本／digest，執行 `docker compose up -d --wait app`。
4. 觀察 readiness、5xx、LINE、DB、S3 與匯入失敗率。
5. 只可回復到與目前 schema 相容的舊映像；不得執行自動 downgrade。

## Kubernetes／雲端

1. 由公司或受託管理帳號建立 PostgreSQL、S3 Bucket、KMS、DNS／TLS 與監控。
2. 依 `k8s/secret.example.yaml` 建立 `linebot-document-secrets`；雲端優先使用 workload identity。
3. 修改 ConfigMap，並將 Deployment 映像替換成 CI 產出的 `repository@sha256:...`。
4. 先執行 `kubectl kustomize k8s/base`，再於 staging namespace 套用。
5. Ingress 只公開 LINE callback 與媒體路徑；`/api/admin/**` 僅允許 VPN／零信任閘道並要求 `X-Admin-Token`，不要公開 `/admin`。
6. rollout 與 readiness 成功後完成批次匯入／匯出與 LINE 端到端測試，再切 DNS。

Deployment 已設定非 root、唯讀根檔案系統、tmp emptyDir、資源限制、滾動更新及 PDB。多 replica 的 webhook 冪等、資產 external ID、批次 ID 與來源事件唯一性必須由 PostgreSQL 約束維持。

## 監控與事故

- Liveness：`/livez`；readiness：`/readyz`。
- 必看：HTTP 5xx、LINE 401／429／timeout、DB pool、Flyway、S3 timeout、匯入批次失敗、hash mismatch、tmpfs 與 JVM heap。
- SIGTERM 至少保留 40 秒 graceful shutdown。
- Secret 洩漏先輪替憑證再重啟，不在工單或 Log 貼出 Secret。
- DB／S3 故障時停止切流，不以空資料庫強行啟動。

## 客戶診斷判讀

雙擊 `linebot.cmd` 並選擇「連線與環境診斷」。請由第一個「失敗」項目開始處理；後續失敗可能只是連鎖結果。

| 階段 | 代表意義 | 常見原因與處理 |
| --- | --- | --- |
| Docker 引擎 | 本機容器環境可用 | 開啟 Docker Desktop，等候 Engine running；仍失敗時重新啟動 Docker Desktop。 |
| 設定完整性 | `.env` 與 Secret 可安全啟動 | 回到「首次設定／修改設定」；不要把 Token 貼到工單或 Log。 |
| Docker Compose 設定 | 部署檔可正確展開 | 確認專案檔案未移動、`.env` 每行只有一個 `KEY=VALUE`。 |
| 本機服務 | App、PostgreSQL 與物件儲存已就緒 | 先選擇「啟動服務」，再從 App 紀錄第一個 `ERROR` 與 `requestId` 查起。 |
| Tunnel 容器 | 本產品管理的 Connector 正在執行 | 只有設定為由本產品啟動 Tunnel 時才檢查；Document 與 Commercial 各用獨立 Token、hostname 與 Compose project。 |
| Cloudflare 邊緣連線 | Connector 可穿過 VPN／防火牆 | 請網管允許 TCP 7844 與 `*.argotunnel.com`；公司 VPN 環境保留 `CLOUDFLARED_PROTOCOL=http2`。 |
| 公開網域 DNS | hostname 已指向 Cloudflare | 檢查 Cloudflare DNS 與 Tunnel Public Hostname 拼字；DNS 未通過時 HTTPS 會自動略過。 |
| 公開網域 HTTPS | LINE 可由外網接觸 App | 404 表示路由到錯誤服務、502 表示 Cloudflare 到 App 不通、403 通常是 Access 規則阻擋 Webhook。 |
| LINE API | Token 有效且主機可連 LINE | 401 重新發行 Token；403 檢查 Messaging API Channel 權限；429 等候後再測；網路錯誤請檢查 VPN、Proxy 與 TLS 解密。 |

內建 Tunnel 的 Public Hostname 服務 URL 為 `http://app:8089`。若公司統一管理外部 Connector，請依其所在位置路由到本機 `127.0.0.1:8089`，並保持 `TUNNEL_ENABLED=false`；此時控制台只略過內建 Connector，仍會驗證公開 DNS 與 HTTPS。

## 發布證據

每次發布保存 commit、測試、SBOM、掃描、映像 digest、Flyway 版本、rollout、抽樣 hash、驗收者與回復決策。
