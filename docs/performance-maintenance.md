# 效能維護紀錄

## 2026-08-25 方法追蹤常駐

### 基準與證據

- 同機 Document 閒置程序量測 10 秒，總 CPU 時間增加 `15.6 ms`，約單核心 `0.16%`，工作集約 `330.9 MB`、83 個執行緒；閒置 CPU 並未持續偏高。
- 已安裝 Commercial 設定為 `METHOD_TRACING_ENABLED=true`、`LOG_LEVEL_ROOT=INFO`；Document 為 `false`。兩個產品共用相同追蹤機制，因此一併加入效能保護。

### 保留的修正

- 設定 schema 由 1 升為 2；舊設定升級時把永久方法追蹤重設為 `false`。
- FLOW_TRACE 未開 DEBUG 時，成功路徑只呼叫原方法，不解析簽章、不建立 UUID、不操作 MDC、不計時。
- 自動測試驗證 INFO 快速路徑不呼叫 `getSignature()`，DEBUG 整合測試仍產生完整進入與完成事件。

### 未採用的猜測

- 沒有調高 Log 輪詢間隔：逐執行緒量測未顯示 500 ms 增量讀檔為 CPU 熱點，修改它只會降低 Log 即時性。
- 沒有限制 JVM 記憶體：直接壓低 heap 可能增加 GC 與 CPU，缺乏證據時不採用。
