# Linebot Document 文件

本專案只提供圖片資產收錄、編號歸檔、查詢取用與可選的資產同步，不包含報價、AI、語音或 MCP 功能。

## 客戶安裝

1. 執行唯一一份 `LinebotDocument-Setup-<版本>.exe`。
2. 第一次開啟時設定 LINE Channel Token、Channel Secret 與資料根目錄。
3. 日後從 App 主視窗按「編輯設定」修改內容；本產品不使用瀏覽器管理頁。
4. 依公司網路選擇 Cloudflare Tunnel、ngrok 或自行提供的公開 HTTPS 網址。
5. 將顯示的 `/callback` 網址填入 LINE Developers Console。
6. 關閉主視窗後 App 會留在系統匣；再次開啟可查看狀態與已整理的即時 Log。

再次執行同一份 Setup 可編輯設定、修復／升級或移除。一般移除會保留客戶設定與圖片資料，只有明確選擇完整清除才會移除資料。

## 公司 VPN 與 Cloudflare Tunnel

公司 VPN 常封鎖 UDP，因此預設 `CLOUDFLARE_PROTOCOL=http2`，使用 TCP 7844。只有網路管理員確認 UDP 7844 可通行時才改為 `quic`；希望 cloudflared 自動選擇時可設為 `auto`。

App 只有在 cloudflared 的本機 `/ready` 回傳成功後才顯示 Tunnel 已啟動。若逾時，請在 App Log 查看已遮罩的 cloudflared 診斷，並請網路管理員確認：

- DNS 可以解析 Cloudflare Tunnel 端點。
- TCP 7844 可由客戶電腦向外連線。
- VPN、Proxy 或端點防護沒有攔截 `cloudflared.exe`。
- Tunnel Token、公開網域與 LINE Callback 指向同一條 Tunnel。

## Log 與流程追蹤

桌面畫面會把 JSON Log 整理為 `[等級] [元件] 事件內容`，並可依等級與文字搜尋。Token、API Key、Authorization 與常見敏感欄位會再次遮罩。

`METHOD_TRACING_ENABLED` 預設為 `false`，避免每個方法的進出 Log 拖慢正式環境。需要重現完整流程時才暫時改為 `true`，測試完成後關閉。

## 開發與發佈

```powershell
.\mvnw.cmd clean verify
powershell.exe -NoProfile -File scripts\build-windows-installer.ps1 -Version <版本>
```

發佈與簽章細節見 [Windows 發佈 Runbook](release-runbook.md)，第三方授權見 [Third-party notices](third-party-notices.md)。
