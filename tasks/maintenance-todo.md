# 文書機維護任務

- [x] Task 1：移除 AI、Voice、MCP 與 Quotation 殘留
  - Acceptance：編譯產物、設定、腳本與文件只描述圖片資產功能。
  - Verify：產品邊界測試與 repository-wide 搜尋。
- [x] Task 2：建立圖片資產專屬環境設定契約
  - Acceptance：設定精靈、`.env.example`、Spring 與 Docker 欄位一致。
  - Verify：UnifiedEnvironmentConfigurationTest、AppConfiguration tests。
- [x] Task 3：強化 Cloudflare VPN 連線與診斷
  - Acceptance：protocol 受驗證、預設 http2、Token 不在命令列、提前退出有安全診斷。
  - Verify：CloudflareProcessTest、CloudflareConnectorTest。
- [x] Task 4：改善 Log 可讀性與預設追蹤成本
  - Acceptance：桌面顯示可讀摘要；預設不建立 AOP trace bean，明確啟用仍可追蹤。
  - Verify：Log 與 MethodTraceLogger focused tests、兩次 Maven test 前後量測。
- [x] Task 5：完成 Windows 發佈驗證
  - Acceptance：clean verify、App Image、Setup install/edit/uninstall 通過。
  - Verify：既有 packaging 與 installer scripts。
  - Result：clean verify、App Image、Setup 靜態驗證，以及 install／repair／uninstall／資料保留／reinstall 真實生命週期均已通過。

效能結果：同條件完整測試由平均 15.062 秒降至 9.510 秒，改善約 36.9%。

- [x] Task 6：設定 LINE 連線與請求逾時
  - Acceptance：桌面設定、環境變數與每次 LINE 請求使用一致且可驗證的逾時。
  - Verify：LineStorageServicePushTest、AppConfiguration tests、UnifiedEnvironmentConfigurationTest。
- [x] Task 7：設定客戶歸檔代碼規則與回覆範例
  - Acceptance：歸檔判斷、語法提示及說明共用可驗證格式，範例由格式產生，過長輸入不執行 regex。
  - Verify：CommandServiceArchiveTest、AppConfigurationValidatorTest、UnifiedEnvironmentConfigurationTest。
- [x] Task 8：統一使用 App 內設定並發佈 0.3.0
  - Acceptance：首次設定、主視窗編輯與 Setup 維護模式都使用 App 設定精靈，不存在瀏覽器設定入口。
  - Verify：UnifiedEnvironmentConfigurationTest、clean verify、Windows Setup release workflow 與 installer lifecycle evidence。
