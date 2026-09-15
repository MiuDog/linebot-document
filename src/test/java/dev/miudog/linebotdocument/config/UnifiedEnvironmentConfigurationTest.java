package dev.miudog.linebotdocument.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedEnvironmentConfigurationTest {

	// 方法：部署只將 PostgreSQL 與物件儲存視為持久狀態，不再掛載桌面資料目錄。
	@Test
	void keepsPersistentStateOutsideTheApplicationImage() throws IOException {
		String environment = read(".env.example");
		String properties = read("src/main/resources/application.properties");
		String compose = read("docker-compose.yml");
		String dockerfile = read("Dockerfile");

		assertThat(environment)
			.contains("COMPANY_ID=")
			.contains("OBJECT_STORAGE_BUCKET=")
			.contains("SECRETS_DIR=")
			.doesNotContain("DATABASE_PASSWORD=")
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
			.contains("SYSTEM_ROOT_PATH: /tmp/linebot")
			.contains("database-data:/var/lib/postgresql/data")
			.contains("object-storage-data:/data")
			.contains("LOCAL_ADMIN_CONTAINER_HOST_ACCESS: \"true\"")
			.contains("127.0.0.1:${APP_PORT:-8089}:8089")
			.doesNotContain("./system-data")
			.doesNotContain("ASSETS_ROOT=")
			.doesNotContain("QUOTATION_ROOT_PATH=");
		assertThat(dockerfile)
			.doesNotContain("COPY outputs/excel-templates")
			.doesNotContain("VOLUME ");
	}

	// 方法：正式入口只能啟動 Spring Boot，不保留任何桌面或服務監督模式。
	@Test
	void usesHeadlessSpringBootAsTheOnlyRuntime() throws IOException {
		String application = read("src/main/java/dev/miudog/linebotdocument/LinebotDocumentApplication.java");
		String properties = read("src/main/resources/application.properties");

		assertThat(application)
			.contains("SpringApplication.run(LinebotDocumentApplication.class, args)")
			.doesNotContain("DesktopApplication")
			.doesNotContain("ServiceApplication")
			.doesNotContain("ApplicationRuntimeMode");
		assertThat(Path.of("src/main/java/dev/miudog/linebotdocument/desktop")).doesNotExist();
		assertThat(properties)
			.contains("management.endpoint.health.probes.enabled=true")
			.contains("management.endpoint.health.probes.add-additional-paths=true")
			.contains("server.shutdown=graceful")
			.contains("spring.lifecycle.timeout-per-shutdown-phase=${SHUTDOWN_TIMEOUT:30s}");
	}

	// 方法：停止產生 Windows 安裝程式，並保留圖片資產產品邊界。
	@Test
	void retiresWindowsPackagingAndKeepsDocumentProductBoundary() throws IOException {
		String schema = read("src/main/resources/db/migration/V1__baseline.sql");

		assertThat(Path.of("packaging/windows")).doesNotExist();
		assertThat(Path.of(".github/workflows/release-windows.yml")).doesNotExist();
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
