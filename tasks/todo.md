# 任務清單：文書機雲端與本地 Docker 雙部署

## Phase D-A：Headless runtime

- [x] D1：保存基線測試、package、目前 schema 與資料路徑證據。
- [x] D2：主程式只保留 Spring Boot server 啟動。
- [x] D2：加入 liveness、readiness 與 graceful shutdown 設定／測試。
- [x] D3：刪除 production／test desktop packages 與 desktop resource。
- [x] D3：移除 JNA、DPAPI、IPC、ngrok、cloudflared runtime 相依。
- [x] D3：刪除 Windows packaging、installer scripts 與 release-windows workflow。
- [ ] D4：建立 type-safe runtime、database、object storage、company、admin 設定。
- [ ] D4：支援 `/run/secrets` config tree，必要 Secret 缺漏時安全失敗。
- [ ] Checkpoint D-A：Maven test／package 通過且無可執行 desktop mode。

## Phase D-B：資料與公司資產

- [ ] D5：加入 PostgreSQL driver、Flyway 與第一版 PostgreSQL migration。
- [ ] D5：建立 import batch／metadata revision／audit／migration ledger schema。
- [ ] D6：遷移所有 repository 的 SQLite-specific SQL。
- [ ] D6：資產流水號、webhook 冪等與 transaction 測試通過。
- [ ] D7：建立 SQLite＋圖片 migration dry-run。
- [ ] D7：完成不刪來源的 copy、hash／count gate、重跑與報告。
- [ ] D8：建立 ObjectStorage port、S3 實作與安全 object key policy。
- [ ] D8：完成 staging、promote、stream、metadata、delete compensation 與整合測試。
- [ ] D9：完成 asset manifest parse／schema／hash／MIME 驗證。
- [ ] D9：完成批次 stage、validate、commit、metadata revision、audit。
- [ ] D9：完成資產 manifest＋原始物件完整匯出及重新匯入。
- [ ] D9：管理入口具備管理憑證及私有網路限制。
- [ ] D10：LINE 收圖、pending、多圖歸檔、搜尋、媒體與 reconciliation 改用 object storage。
- [ ] D10：webhook 重送及批次中斷不得建立部分可見或重複資產。
- [ ] Checkpoint D-B：PostgreSQL、S3、批次與 migration 驗證通過。

## Phase D-C：部署與文件

- [ ] D11：Dockerfile 使用非 root、固定基底與唯讀 root 相容目錄。
- [ ] D11：Compose 提供 App、PostgreSQL、MinIO、初始化與選用 cloudflared。
- [ ] D11：文書機固定主機 `127.0.0.1:8089`，不使用 `container_name`。
- [ ] D11：Secret file、volume、health、restart 與資料持久化測試通過。
- [ ] D12：建立 Kubernetes Deployment／Service／ConfigMap／Secret 範本／PDB。
- [ ] D12：probes、resources、securityContext、rolling update 與 digest 規則完整。
- [ ] D13：CI 驗證 Maven、migration、Compose、Kubernetes、SBOM、scan、digest。
- [ ] D14：ADR 說明桌面退役、單公司部署、PostgreSQL 與 S3。
- [ ] D14：本地 Docker 與 Kubernetes 部署文件完成。
- [ ] D14：資產收錄／匯入／驗證／提交／修正／匯出／重匯 SOP 完成。
- [ ] D14：備份、還原、遷移、升級、rollback、監控與排錯文件完成。
- [ ] D14：文件索引及所有舊桌面說明已更新或標記歷史。
- [ ] 完整 `mvn test`、package、Docker、migration、資產與部署驗證通過。
- [ ] 對七份核准 SPEC 逐條建立直接證據，無未完成要求。
