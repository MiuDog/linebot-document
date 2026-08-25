package dev.miudog.linebotdocument.desktop.cloudflare;

import java.nio.file.Path;
import java.time.Duration;

/**
 * 定義 connector 唯一可操作的 cloudflared child process 邊界。
 */
public interface CloudflareProcessControl {

	// 方法：以 agent 與 Token 啟動 cloudflared child。
	void start(
		Path agent,
		String tunnelToken,
		CloudflareProtocol protocol
	);

	// 方法：在 timeout 內等待 cloudflared 官方 readiness endpoint 成功。
	boolean awaitReady(Duration timeout);

	// 方法：取得最後一筆已清理的 cloudflared 診斷。
	String diagnostic();

	// 方法：在 timeout 內停止本 App 建立的 child。
	void stop(Duration timeout);

	// 方法：取得目前 cloudflared child 狀態。
	CloudflareStatus status();
}
