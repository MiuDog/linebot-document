# Changelog

## [0.3.0] - 2026-08-26

### Added

- 新增獨立背景 service launcher、Windows 登入自動啟動與桌面控制器探測。
- 新增只綁定 loopback 的 nonce 認證控制通道，可查詢狀態、重新載入設定與安全停止。
- 新增跨程序 service 鎖，避免登入自動啟動與手動開啟 App 同時建立兩組 Tunnel。

### Changed

- 桌面 App 改為純控制器，不再持有 Spring、ngrok 或 cloudflared；關閉視窗不影響背景服務。
- 編輯設定後由背景 service 重新讀取 DPAPI 設定，Installer 升級與解除安裝會依序停止及恢復服務。

### Security

- 控制 nonce 以 DPAPI 保護後原子發布，不寫入 Log，命令與回應都限制為固定列舉。
- IPC 僅接受 loopback、限制 payload 與 timeout，並以固定時間比較 nonce。

## [0.2.3] - 2026-08-25

### Added

- Cloudflare 設定新增 App 專用 Tunnel ID，啟動前會驗證 Token 是否屬於該 Tunnel。
- 主畫面顯示目前電腦、Tunnel ID 與 Connector ID，方便辨識錯誤電腦上的副本連線。

### Security

- 同一台 Windows 電腦使用跨程序租約，阻止 Commercial 與 Document 共用同一條 Tunnel。
- 設定精靈加入獨立 Tunnel 與安全移機提示，避免複製 Token 時意外建立 Connector 副本。

### Performance

- 舊版設定若殘留永久方法追蹤，升級時會自動關閉；完整追蹤仍可在受控 DEBUG 測試中明確啟用。
- INFO 正式模式的方法追蹤改走快速路徑，成功呼叫不再解析方法簽章、建立背景 UUID、操作 MDC 或計算耗時。

## [0.2.2] - 2026-08-25

### Fixed

- 系統匣右鍵選單改用可套用繁體中文字型的 Swing 元件，修正 Windows 顯示方塊字。
- 主視窗與工作列提供 16～256 px 多尺寸品牌圖示，並讓安裝器捷徑明確引用 App 執行檔圖示。

## [0.2.1] - 2026-08-25

### Changed

- document 主視窗、系統匣、Windows Launcher、Setup 與解除安裝程式統一使用新版藍色品牌圖示。
- 桌面執行期使用最佳化 256×256 圖示，Windows 封裝使用 16～256 px 多尺寸 ICO。

### Added

- 主視窗新增「測試連線」，可對指定公開或內網網域分階段檢查 DNS、TCP、TLS 與 HTTP。
- 連線報告同時驗證本機 health endpoint 與 LINE Bot API Token，顯示步驟耗時並產生不含機密的關聯 Log。

## [0.2.0] - 2026-08-25

### Added

- LINE 連線與單次請求逾時可由 App 設定，降低公司 VPN 半斷線時長時間等待的情況。
- 圖片歸檔代碼支援 `#` 數字與 `@` 大寫字母遮罩，錯誤提示會自動產生合法範例。

### Changed

- 首次安裝與日後修改都統一使用 App 內繁體中文設定精靈，不再開啟瀏覽器管理頁。
- Cloudflare 預設使用較適合受限 VPN 的 HTTP/2，並改善啟動診斷與健康檢查資源使用。
- Log 視窗改為中文可讀摘要，完整方法追蹤改為明確啟用，降低正常運行成本。

### Removed

- 移除文書機不使用的 AI、語音、MCP、報價功能、設定槽位與相關文件。
- 移除主視窗的本機管理頁按鈕及瀏覽器設定路徑。

### Security

- 保留 `/admin` 本機存取防護，並取消 Docker 對管理頁私有橋接來源的額外放行。
- 客戶歸檔格式不接受自訂 Regex，並限制遮罩數量、格式長度與待比對輸入長度。
