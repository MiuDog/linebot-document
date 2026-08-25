# Implementation Plan：文書機維護強化

## Architecture Decisions

- 先刪除 AI／Voice／MCP 完整垂直切片，再以產品邊界測試防止回流。
- 以 `AppConfigurationField` 作為桌面設定的唯一產品契約，其他設定介面由測試核對。
- Cloudflare 維持 child process 架構，加入受限 protocol、可清理診斷與較可靠的啟動觀察，不把 Token 放入參數。
- 保留 AOP 全方法追蹤能力但改為明確啟用、DEBUG 輸出；圖片業務事件負責正式環境流程追蹤。
- 將網路逾時與客戶歸檔代碼規則設定化；協定上限與安全規則維持程式內固定值。

## Dependency Order

產品邊界刪除 → 設定契約 → Tunnel process 契約 → Log 摘要 → 追蹤成本 → LINE 韌性 → 歸檔規則 → 打包驗證。

## Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| 移除 Voice 破壞 Webhook 建構 | 先改 controller 行為測試，再移除 service／controller |
| 舊設定檔仍含已刪欄位 | repository 只載入已知 enum，未知欄位安全忽略 |
| VPN 仍攔截 TCP 7844 或 DNS | Log 顯示 protocol 與 cloudflared 安全診斷，文件列出必要出口 |
| 關閉預設方法追蹤降低可見性 | 保留圖片重大事件 INFO、HTTP／外部依賴事件與可切換完整追蹤 |

## Checkpoints

- 產品邊界：focused tests 通過，主程式搜尋不到 AI／Voice／MCP／Quotation。
- 運行品質：Tunnel、Log、追蹤 focused tests 通過，效能重新量測。
- Release：`clean verify`、App Image、Setup 與安裝生命週期通過。
