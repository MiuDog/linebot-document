package dev.miudog.linebotdocument.desktop.cloudflare;

import java.util.Locale;

/**
 * 限制 cloudflared 可使用的官方傳輸協定參數。
 */
public enum CloudflareProtocol {

	AUTO("auto"),
	HTTP2("http2"),
	QUIC("quic");

	//#region 欄位

	private final String argument;

	//#endregion

	//#region 建構子

	// 方法：建立可直接傳給 cloudflared 的固定協定參數。
	CloudflareProtocol(String argument) {
		this.argument = argument;
	}

	//#endregion

	//#region 方法

	// 方法：取得已列入允許清單的 cloudflared 協定參數。
	public String argument() {
		return argument;
	}

	// 方法：解析客戶設定，空值採用較適合公司 VPN 的 HTTP/2。
	public static CloudflareProtocol parse(String value) {
		String normalized = value == null || value.isBlank()
			? "http2"
			: value.trim().toLowerCase(Locale.ROOT);

		return switch (normalized) {
			case "auto" -> AUTO;
			case "http2" -> HTTP2;
			case "quic" -> QUIC;
			default -> throw new IllegalArgumentException("不支援的 Cloudflare 傳輸協定");

		};
	}

	//#endregion
}
