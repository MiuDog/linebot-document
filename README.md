# Assets Manager LINE Bot

`@linebot-document@0.2.0`

把 LINE 群組當成圖片資產的收件與取件窗口：群組上傳圖片後，引用圖片並輸入合法資料夾代碼即可直接歸檔；SQLite 保存圖片組與正式檔案索引。

---

## 它做什麼

| 你在群組做的事 | 系統的反應 |
|---|---|
| 傳一張或一組圖 | 下載至待處理區，等待資料夾代碼 |
| 引用圖片，輸入 `ZD12345` 等合法代碼 | 將已抓取的圖片直接歸檔；缺圖時仍保存成功圖片並回報數量 |
| 再次引用已歸檔圖片並輸入代碼 | 允許重複存入，建立新的流水號 |
| 輸入合法代碼但未引用圖片 | 回覆操作錯誤，不會無回應 |
| 輸入 `#查 ZD12345` | 把該代碼的圖片貼回群組 |
| 輸入 `#標籤` | 列出所有編號與各自張數 |
| 輸入 `#說明` | 顯示用法 |
| 標記機器人並輸入 `ping` | 回覆 `pong` 與本次事件的延遲毫秒數 |

多張同時上傳的圖片使用 LINE `imageSet` 資訊分組；webhook 到達順序不影響圖片順序。單張圖片則獨立視為一組。

所有檔案共用 `SYSTEM_ROOT_PATH`；圖片位於其「圖片資產」子目錄，並依「部門代碼／台北日期」分層：

```
system-data\
├─ log\
└─ 圖片資產\
   ├─ assets.db
   ├─ .pending\
   ├─ ZD12345\
   │  ├─ 20260727\
   │  │  ├─ 20260727-01.jpg
   │  │  └─ 20260727-02.jpg
   │  └─ 20260728\
   │     └─ 20260728-01.jpg
   └─ YJ123456\
      └─ 20260728\
         └─ 20260728-01.jpg
```

`.pending` 只保存尚未歸檔的圖片；輸入合法代碼後才建立正式資料夾。每個部門每天從 `01` 獨立計數，超過 `99` 自動擴充為三碼。

---

## Windows App（未簽章，個人使用）

Windows 桌面版會安裝為單一 App，內含 Java Runtime，不需要使用者另裝 JDK／JRE。第一次開啟會顯示繁體中文設定精靈；關閉視窗後仍可留在系統匣背景執行，再次開啟 App 可查看狀態與即時 Log。

所有設定都在 App 內完成：首次安裝會自動顯示設定精靈，日後可從主視窗按「編輯設定」，不需要也不提供瀏覽器管理頁。

**目前版本導向為未簽章的個人使用版。** 功能完整，唯一差別是沒有 Authenticode 簽章，首次執行會出現 Windows SmartScreen 警告（處理方式見下方）。Release 建立在 private repo，只有具備存取權的人看得到，並一律標記為 pre-release。

### 自動發佈到 GitHub Release

推送符合 `v<主版號>.<次版號>.<修訂號>` 的 tag 即自動建置、驗證並建立 GitHub Release，資產只有一份 Setup.exe：

```powershell
powershell.exe -NoProfile -File scripts\release.ps1 -Version 0.2.0
```

腳本會依序完成前置檢查、改寫 `pom.xml` 與 README 版本、提交、本機完整驗證、推分支再推 tag。任何一項前置檢查不過就在改動版本庫之前中止：

| 參數 | 用途 |
| --- | --- |
| `-DryRun` | 只印出將執行的指令，不改任何東西 |
| `-SkipVerify` | 略過本機 `mvnw clean verify`（交給 CI 驗） |
| `-Force` | 允許重新指向遠端已存在的 tag |
| `-Branch` | 發版分支，預設 `main` |

Release 內容取決於是否設定簽章憑證，workflow 會自動判斷：

| 狀態 | 行為 |
| --- | --- |
| 未設定簽章憑證（目前） | 略過商用欄位與 Authenticode 閘門，Release 標記為 **pre-release**，Notes 附 SmartScreen 說明與 SHA-256 |
| 已設定簽章憑證 | 套用商用欄位閘門、簽章並驗證 Authenticode，全部通過才建立正式 Release |

版本號取自 tag，且必須與 `pom.xml` 一致，否則 `build-windows-installer.ps1` 會以「Maven 版本與 Setup 版本不一致」中止。`release.ps1` 會自動保持兩者同步，因此不需要手動改版本再打 tag。

本 repo 目前是 private，因此 Release 只有具備存取權的人看得到。推 tag 時 `dry-run` job 會顯示 skipped，這是正常的：它只在手動 `workflow_dispatch` 時執行。

建立個人使用的 Setup：

```powershell
powershell.exe -NoProfile -File scripts\build-windows-installer.ps1 -Version 0.1.1
```

完整安裝、修復、預設保留資料與明確清除驗收：

```powershell
powershell.exe -NoProfile -File scripts\test-windows-installer.ps1 `
	-InstallerPath dist\LinebotDocument-Setup-0.1.1.exe `
	-ExecuteLifecycle `
	-TestPurge
```

### 安裝未簽章版本

因為安裝檔沒有程式碼簽章，Windows 會在第一次執行時顯示藍色的「Windows 已保護您的電腦」畫面。這是預期行為，不代表檔案有問題：

1. 執行 `LinebotDocument-Setup-<版本>.exe`。
2. 出現 SmartScreen 藍色警告時，點左下角的**「其他資訊」**。
3. 展開後點**「仍要執行」**。
4. 依安裝精靈完成安裝；不需要系統管理員權限，只安裝給目前的 Windows 使用者。
5. 安裝完成後會自動開啟設定精靈，填入 LINE Channel Token／Secret 與資料存放位置即可。

同一份 Setup 之後再執行，會顯示**編輯設定**、**修復／升級**、**移除**三個選項。預設的移除會保留你的設定與資料；只有明確勾選完整清除才會刪除。

若瀏覽器在下載時就攔截，選擇「保留」即可。這個警告會在同一台電腦上出現一次，之後執行不再提示。

> 警告的成因是缺少憑證，不是防毒軟體判定為惡意程式。若要消除警告就必須購買 Windows 程式碼簽章憑證；相關取得方式與對 CI 的影響見 [Windows 發佈 Runbook](docs/release-runbook.md)。

### 日後若要轉為商用發佈

需替換核准 EULA、Publisher 與支援網址，並在 GitHub `commercial-release` Environment 配置簽章憑證。注意自 2023 年 6 月起，公開 CA 簽發的 OV 憑證私鑰必須存放於硬體或雲端金鑰服務，現行 workflow 匯入 PFX 的步驟屆時需改為雲端簽章 API。詳細操作與回復方式見 [Windows 發佈 Runbook](docs/release-runbook.md)。

---

## 快速開始

```bash
cp .env.example .env
```

填入 `LINE_BOT_CHANNEL_TOKEN`、`LINE_BOT_CHANNEL_SECRET`、`NGROK_AUTHTOKEN`、
`SYSTEM_ROOT_PATH` 與 `ASSETS_SYNC_TOKEN`，然後：

```bash
docker compose --profile dev up --build -d
```

資料庫同步預設改由腳本執行。單次同步：

```powershell
.\scripts\sync-assets.ps1
```

在隱藏的背景程序中每 30 秒同步：

```powershell
.\scripts\sync-assets.ps1 -Background
```

打開 http://localhost:4040 取得 ngrok 網址，填回 `.env` 的 `PUBLIC_BASE_URL` 並重啟，最後到 LINE Developers Console 把 Webhook URL 設成 `https://xxxx.ngrok-free.app/callback`。

完整步驟見 [docs/01-bot-deployment.md](docs/01-bot-deployment.md)。

### 確認程式是否正常

先看容器狀態：

```powershell
docker compose ps
```

`linebot` 顯示 `running`、`healthy` 代表服務已就緒。也可以直接檢查健康端點：

```powershell
Invoke-RestMethod http://localhost:8088/actuator/health
```

正常時會看到 `status` 為 `UP`。持續查看應用程式日誌：

```powershell
docker compose logs -f linebot
```

---

## 文件

| 文件 | 什麼時候看 |
|---|---|
| [文件入口](docs/README.md) | 安裝、設定、VPN、Cloudflare 與故障排除 |
| [02 LINE Bot 規則與各階段處理](docs/02-linebot-rules.md) | 動訊息收發的程式碼前；測試或部署卡住 |
| [Windows 發佈 Runbook](docs/release-runbook.md) | 建立、簽署或回復 Setup 發佈 |

---

## 技術組成

| 項目 | 選擇 |
|---|---|
| Java | 25 |
| 框架 | Spring Boot 4.1 |
| 資料庫 | SQLite（單一檔案，與圖片放在一起） |
| 資料存取 | `JdbcClient`（不用 JPA） |
| LINE 整合 | 直接呼叫 Messaging API，未使用官方 SDK |
| 容器 | 多階段建置，執行階段 `eclipse-temurin:25-jre` |

**執行階段刻意不用 Alpine**：musl 沒有 UTF-8 locale，JVM 的 `sun.jnu.encoding` 會退化成 ASCII，中文分類資料夾會全部變成問號。

---

## 開發

```bash
./mvnw test
```

自動測試涵蓋收錄、圖片組歸檔、每日流水號、路徑穿越防護、同步安全與運行日誌，
**不需要真實 LINE 憑證**。

```bash
./mvnw clean package
```

Push 前至少執行 `./mvnw clean verify`，Windows 發佈另依 [Windows 發佈 Runbook](docs/release-runbook.md) 驗證。

---

## 目前狀態與已知限制

| 功能 | 狀態 |
|---|---|
| 圖片收錄、`zd` 編號歸檔、查詢取用 | ✅ 完成 |

**LINE 相簿拿不到**：Messaging API 完全不暴露群組相簿，照片放進相簿也不會產生 webhook 事件。本專案改用「引用回覆打編號」達成等效分類。
