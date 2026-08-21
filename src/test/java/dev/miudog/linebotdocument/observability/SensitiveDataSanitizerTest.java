package dev.miudog.linebotdocument.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

	// 驗證安全下載網址不會把 bearer token 寫入 HTTP 日誌。
	@Test
	void redactsDownloadTokenFromPath() {
		String sanitized = SensitiveDataSanitizer.sanitizeRequestPath(
			"/api/storage/download/super-secret-download-token"
		);

		assertThat(sanitized)
			.isEqualTo("/api/storage/download/[REDACTED]")
			.doesNotContain("super-secret-download-token");
	}

	// 驗證圖片分享網址同樣不保存資產分享 token。
	@Test
	void templatesMediaShareToken() {
		String sanitized = SensitiveDataSanitizer.sanitizeRequestPath("/media/raw-share-token");

		assertThat(sanitized)
			.isEqualTo("/media/{shareToken}")
			.doesNotContain("raw-share-token");
	}
}
