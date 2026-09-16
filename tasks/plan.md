# 實作計畫：文書機雲端與本地 Docker 雙部署

狀態：Active
核准規格：`CAPABILITY-MAP-cloud-local-deployment.md` 與對應七份 `SPEC-*.md`
舊計畫：已封存至 `tasks/archive/plan-legacy-windows-2026-09-02.md`

## 1. 目標

把文書機器人改為單一 headless Spring Boot／OCI 服務；本地以 Docker Compose 執行，雲端以 Kubernetes 部署。資產索引改用 PostgreSQL，圖片改用 S3 相容物件儲存，公司可透過同一 manifest 契約匯入／匯出與維護資產，並完整移除桌面 App 交付鏈。

## 2. 依賴圖

```text
D1 基線
├─ D2 Headless 啟動 ─> D3 Desktop／Tunnel 移除 ─> D4 外部設定與 Secret
├─ D5 PostgreSQL migration ─> D6 Repository SQL 遷移 ─> D7 舊資料遷移工具
├─ D8 S3 儲存介面 ─> D9 公司資產批次／匯出 ─> D10 收圖／歸檔／查詢遷移
├─ D11 本地 Docker
├─ D12 Kubernetes 雲端部署
├─ D13 CI／供應鏈
└─ D14 文件與完成性驗證
```

## 3. 分階段工作

### Phase D-A：安全移除桌面邊界

- D1：記錄既有測試、打包與 Docker 設定基線。
- D2：主程式改為唯一 `SpringApplication.run`，加入 graceful shutdown 與 probes。
- D3：移除 desktop packages／tests、JNA、DPAPI、內建 ngrok／cloudflared、Windows packaging／scripts／workflow。
- D4：以 type-safe properties、環境變數與 config tree 建立非桌面設定／Secret 契約。

Checkpoint：非 Windows 環境完成 Maven test／package；repository 無可執行 desktop mode。

### Phase D-B：可攜式資料與資產層

- D5：導入 Flyway 與 PostgreSQL schema，建立批次、metadata revision、audit 與 migration ledger。
- D6：把 SQLite-specific repository SQL、日期、布林、upsert、流水號與冪等語意改成 PostgreSQL。
- D7：建立 SQLite＋圖片資料夾唯讀遷移命令，支援 dry-run、重跑與 SHA-256 報告。
- D8：建立 object storage port 與 S3 實作，統一 key、stream、metadata、staging、promote、delete compensation。
- D9：建立 manifest、批次 stage／validate／commit、metadata revision、完整匯出與重新匯入。
- D10：LINE 收圖、pending、多圖原子歸檔、查詢、媒體及 reconciliation 改用共同儲存契約。

Checkpoint：PostgreSQL 整合測試、S3 測試、批次原子可見性、舊資料 dry-run／migration 全部通過。

### Phase D-C：部署與營運

- D11：強化 Dockerfile，建立 App＋PostgreSQL＋MinIO＋初始化＋選用 cloudflared 的本地 Compose；主機埠為 `8089`。
- D12：建立 Kubernetes Deployment、Service、ConfigMap／Secret 範本、probes、resource limits、PDB 與 rollback 說明。
- D13：更新 CI，驗證 Maven、migration、Compose config、Kubernetes schema、SBOM、映像掃描與 digest。
- D14：完成 ADR、部署、資產 SOP、備份／還原、遷移、升級、回復、監控與排錯文件；逐條稽核規格。

## 4. 風險與緩解

| 風險 | 緩解 |
|---|---|
| 多圖歸檔跨 DB／S3 中斷 | 整批 staging、DB commit 後 promote、失敗補償與 reconciliation |
| SQLite 到 PostgreSQL 造成資產編號／冪等差異 | 真實 PostgreSQL 並行 integration tests |
| 外部匯入引入惡意或錯誤檔案 | MIME、magic bytes、大小、hash、key policy 與批次原子提交 |
| 舊資料遷移破壞來源 | 工具唯讀來源、不自動刪除、dry-run 與 hash／count gate |

## 5. 驗證命令

- `./mvnw.cmd test`
- `./mvnw.cmd -DskipTests package`
- `docker compose config`
- `docker compose up --build --wait`
- PostgreSQL／S3 integration tests
- SQLite migration dry-run 與重跑測試
- OCI contents／Secret／公司資產掃描
- Kubernetes manifests server-side 或 schema 驗證
