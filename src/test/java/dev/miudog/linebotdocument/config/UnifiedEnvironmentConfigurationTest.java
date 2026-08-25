package dev.miudog.linebotdocument.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedEnvironmentConfigurationTest {

	@Test
	void derivesEveryFilesystemLocationFromOneSystemRoot() throws IOException {
		String environment = read(".env.example");
		String properties = read("src/main/resources/application.properties");
		String compose = read("docker-compose.yml");
		String dockerfile = read("Dockerfile");

		assertThat(environment)
			.contains("SYSTEM_ROOT_PATH=")
			.doesNotContain("ASSETS_ROOT=")
			.doesNotContain("QUOTATION_ROOT_PATH=")
			.doesNotContain("QUOTATION_OUTPUT_PATH=")
			.doesNotContain("QUOTATION_TEMPLATE_PATH=")
			.doesNotContain("LOG_PATH=");
		assertThat(properties)
			.contains("app.system.root=${SYSTEM_ROOT_PATH:${user.dir}/system-data}")
			.contains("app.storage.root=${app.system.root}/")
			.doesNotContain("app.quotation.")
			.contains("app.observability.log-path=${app.system.root}/log");
		assertThat(compose)
			.contains("${SYSTEM_ROOT_PATH:-./system-data}:/data/system-root")
			.contains("SYSTEM_ROOT_PATH=/data/system-root")
			.doesNotContain("LOCAL_ADMIN_CONTAINER_HOST_ACCESS")
			.contains("127.0.0.1:8088:8088")
			.doesNotContain("ASSETS_ROOT=")
			.doesNotContain("QUOTATION_ROOT_PATH=");
		assertThat(dockerfile)
			.doesNotContain("COPY outputs/excel-templates")
			.contains("VOLUME /data/system-root")
			.doesNotContain("VOLUME /data/assets");
	}

	// 方法：設定只能由桌面 App 開啟，不保留網頁設定按鈕、瀏覽器呼叫或頁面資源。
	@Test
	void usesDesktopAppAsTheOnlyConfigurationSurface() throws IOException {
		String desktopWindow = read("src/main/java/dev/miudog/linebotdocument/desktop/DesktopWindow.java");

		assertThat(desktopWindow)
			.contains("new JButton(\"編輯設定\")")
			.doesNotContain("開啟本機管理頁")
			.doesNotContain("Desktop.getDesktop().browse");
		assertThat(Path.of("src/main/resources/static/admin"))
			.doesNotExist();
		assertThat(Path.of("src/main/resources/templates/admin"))
			.doesNotExist();
	}

	// 方法：Windows 發佈內容只能描述圖片資產功能，不夾帶報價硬體警告、選單或資料表。
	@Test
	void packagesOnlyTheDocumentProductExperience() throws IOException {
		String installer = read("packaging/windows/installer.nsi");
		String packageScript = read("scripts/package-windows-app.ps1");
		String schema = read("src/main/resources/schema.sql");

		assertThat(installer)
			.doesNotContain("Excel.Application")
			.doesNotContain("預設印表機")
			.doesNotContain("報價 PDF");
		assertThat(packageScript).doesNotContain("語音任務機器人");
		assertThat(schema).doesNotContain("admin_audit_log");
		assertThat(Path.of("src/main/resources/line/rich-menu.json")).doesNotExist();
	}

	// 方法：圖片資產產品不得載入 AI、語音、MCP 或報價設定。
	@Test
	void keepsOnlyImageAssetProductSettings() throws IOException {
		String environment = read(".env.example");
		String properties = read("src/main/resources/application.properties");

		assertThat(environment)
			.doesNotContain("AI_")
			.doesNotContain("VOICE_")
			.doesNotContain("MCP_")
			.doesNotContain("QUOTATION_");
		assertThat(properties)
			.doesNotContain("app.ai.")
			.doesNotContain("app.voice.")
			.doesNotContain("app.quotation.");
	}

	@Test
	void loadsChineseDerivedDirectoryNamesWithoutMojibake() throws IOException {
		Properties properties = new Properties();

		// 依照 Java properties 的實際規則載入，防止 UTF-8 中文被誤讀成亂碼。
		try (var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
			properties.load(input);
		}

		assertThat(properties.getProperty("app.storage.root"))
			.isEqualTo("${app.system.root}/圖片資產");
		assertThat(properties.getProperty("app.quotation.output-path"))
			.isNull();
	}

	// 方法：以 UTF-8 讀取受測設定檔。
	private String read(String path) throws IOException {
		return Files.readString(Path.of(path), StandardCharsets.UTF_8);
	}
}
