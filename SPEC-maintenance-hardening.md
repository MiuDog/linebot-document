# Spec：文書機圖片資產系統維護強化

## Objective

將 `linebot-document` 收斂為供文書機執行的圖片資產儲存與查詢系統。移除報價、AI、語音及 MCP 的程式、設定與文件殘留；改善公司 VPN 環境下的 Cloudflare Tunnel 可診斷性與連線韌性，並降低啟動與執行成本。

## Commands

- 測試：`.\mvnw.cmd --batch-mode --no-transfer-progress test`
- 完整驗證：`.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`
- App Image：`powershell.exe -NoProfile -File scripts\package-windows-app.ps1 -Version 0.2.0`
- Setup：`powershell.exe -NoProfile -File scripts\build-windows-installer.ps1 -Version 0.2.0`

## Product Boundary

- 必要能力：LINE Webhook、圖片暫存與歸檔、標籤查詢、圖片取回、資產同步、SQLite、Windows 桌面宿主。
- 不屬於本產品：報價、Excel／PDF、AI 圖片解析、語音命令、Voice MCP、AI 成本稽核。
- 設定精靈、`.env.example`、Spring properties、Docker Compose 與文件必須遵守同一份環境變數契約。

## Network Requirements

- Cloudflare Tunnel Token 只能透過 child environment 傳遞，不可出現在命令列或 Log。
- 公司 VPN 或防火牆可能阻擋 UDP／QUIC；預設使用 HTTP/2，並提供 `auto`、`http2`、`quic` 三種受驗證選項。
- cloudflared 的診斷輸出必須被有限容量保留、清除敏感資料並寫入 App Log；啟動必須在穩定觀察期內偵測提前退出。
- 圖片對外 URL 使用 Cloudflare 公開 HTTPS 網域，本機 origin 維持 loopback。

## Logging Requirements

- 正式環境預設關閉逐方法追蹤，但保留可由設定精靈啟用的完整流程測試能力。
- 逐方法追蹤使用 DEBUG，錯誤使用 ERROR；圖片歸檔、查詢與同步事件維持 INFO。
- 桌面 Log 顯示時間、等級、元件、事件與重點欄位，不直接顯示整行 JSON。
- Token、Secret、Header、完整請求本文與使用者訊息不得寫入 Log。

## Performance Budget

- 同機 Maven test 暖機基準為 14.662 秒與 15.461 秒，平均 15.062 秒。
- 預設設定的兩次平均測試時間必須較基準改善，且不得以跳過測試換取改善。
- 移除不屬於產品的 Spring Beans、控制器與程式碼；預設不得建立逐方法 AOP 攔截器。

## Testing Strategy

- 邊界測試：主程式不得含 AI、Voice、MCP、Quotation 類別或設定鍵。
- 單元測試：產品設定欄位集合、Tunnel 命令與診斷清理、JSON Log 摘要解析。
- 整合測試：圖片 Webhook 不處理 audio，Spring 預設不建立 MethodTraceLogger。
- 完整驗證：所有 Maven 測試、SBOM、App Image 自包含 Runtime、Setup 安裝生命週期。

## Boundaries

- Always：先寫失敗測試；只記錄允許欄位；外部連線具 timeout；保留 DPAPI 機密儲存。
- Ask first：變更圖片資產資料庫 schema、LINE 對外契約、加入新外部服務。
- Never：提交 `.env`、把 Token 放入程序參數、為效能刪除必要驗證或測試。

## Success Criteria

- document 的編譯產物、設定精靈、範例環境變數與文件不含 AI、Voice、MCP 或 Quotation 功能。
- VPN 受限環境可選 HTTP/2，Tunnel 失敗能從安全且可讀的 Log 找到原因。
- Log 視窗不再顯示原始 JSON，方法追蹤預設不造成 INFO 洪水。
- 前後效能數據、完整測試與 Windows 打包證據均被記錄。

## 可注入功能維護

- LINE API 的連線與單次請求逾時必須分開設定；VPN 半斷線時不得無限等待。
- 客戶的歸檔代碼格式必須可由設定注入；顯示範例由第一組格式自動產生，錯誤回覆與說明同步使用同一規則。
- 自訂歸檔格式使用 `#` 數字、`@` 大寫字母的非技術遮罩，必須限制數量與長度，且匹配前限制輸入長度。
- 新設定缺省時維持既有行為：LINE 連線 10 秒、請求 30 秒，既有 ZD／ZD-JY／YJ 代碼仍全部可用。
- LINE 訊息上限、路徑逃逸防護與 Webhook 簽章屬協定／安全邊界，不開放客戶修改。
