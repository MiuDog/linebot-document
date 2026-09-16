# ADR-001：Headless、單公司 PostgreSQL／S3 架構

狀態：Accepted（2026-09-02）

## Context

舊版將 Spring Boot、SQLite、本機圖片、Tunnel 程序與 Windows 桌面控制器綁在同一個 App，不適合雲端滾動更新、資料可攜與多副本冪等。

## Decision

- 只保留 headless Spring Boot server；Windows App、installer、IPC、DPAPI 與內嵌 Tunnel 退役。
- 同一 OCI 映像支援本地 Compose 與標準 Kubernetes。
- 每套部署只服務一個 `companyId`；PostgreSQL 保存索引／標籤／稽核，S3 相容儲存保存圖片。
- 批次圖片以 manifest 經 stage、validation、commit 管理，並提供完整 ZIP 匯出／重匯。
- 容器主機檔案只作暫存；Secret 與 Tunnel 由平台獨立管理。

## Consequences

本地環境需維護 PostgreSQL 與 MinIO，舊 SQLite／圖片需明確遷移。公司須指定資產 owner、來源證明與保存政策。容器映像可被取得 bytecode；若程式不可交付，必須採供應方託管 SaaS 並以契約與完整資料匯出保障公司資產。
