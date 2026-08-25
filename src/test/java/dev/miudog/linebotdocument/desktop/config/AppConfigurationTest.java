package dev.miudog.linebotdocument.desktop.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 驗證桌面設定的欄位分類、預設路徑與不可變操作。
 */
class AppConfigurationTest {

	// 方法：驗證預設設定與資料都放在目前使用者的 Local AppData。
	@Test
	void shouldUseLocalAppDataForDefaultDirectories() {
		Path localAppData = Path.of("C:/Users/test/AppData/Local");
		AppConfiguration configuration = AppConfiguration.defaults(localAppData);

		assertThat(AppConfiguration.configurationRoot(localAppData))
			.isEqualTo(localAppData.resolve("LinebotDocument/config"));
		assertThat(configuration.value(AppConfigurationField.SYSTEM_ROOT_PATH))
			.isEqualTo(localAppData.resolve("LinebotDocument/data").toString());
	}

	// 方法：驗證圖片資產系統的機密欄位具有單一且完整的分類。
	@Test
	void shouldClassifyEverySpecifiedSecretField() {
		Set<String> secretEnvironmentKeys = AppConfigurationField.secretFields().stream()
			.map(AppConfigurationField::environmentKey)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());

		assertThat(secretEnvironmentKeys).containsExactlyInAnyOrder(
			"LINE_BOT_CHANNEL_TOKEN",
			"LINE_BOT_CHANNEL_SECRET",
			"ASSETS_SYNC_TOKEN",
			"NGROK_AUTHTOKEN",
			"CLOUDFLARE_TUNNEL_TOKEN"
		);
	}

	// 方法：文書機設定不得顯示 AI、語音、MCP 或報價槽位。
	@Test
	void shouldContainOnlyImageAssetProductGroups() {
		assertThat(AppConfigurationField.values())
			.extracting(AppConfigurationField::environmentKey)
			.noneMatch(key -> key.startsWith("AI_")
				|| key.startsWith("VOICE_")
				|| key.startsWith("QUOTATION_"));
		assertThat(AppConfigurationField.Group.values())
			.extracting(Enum::name)
			.doesNotContain("AI", "VOICE", "QUOTATION");
	}

	// 方法：驗證修改設定會建立新物件，不會改變既有設定快照。
	@Test
	void shouldCreateANewSnapshotWhenChangingAValue() {
		AppConfiguration original = AppConfiguration.defaults(Path.of(System.getProperty("java.io.tmpdir")));
		AppConfiguration changed = original.withValue(
			AppConfigurationField.LINE_BOT_CHANNEL_TOKEN,
			"test-token"
		);

		assertThat(original.value(AppConfigurationField.LINE_BOT_CHANNEL_TOKEN)).isEmpty();
		assertThat(changed.value(AppConfigurationField.LINE_BOT_CHANNEL_TOKEN)).isEqualTo("test-token");
	}
}
