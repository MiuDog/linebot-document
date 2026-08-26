package dev.miudog.linebotdocument.desktop.config;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定義桌面設定欄位的環境變數、Spring 屬性、群組與安全分類。
 */
public enum AppConfigurationField {

	SERVER_PORT("SERVER_PORT", "server.port", "本機服務埠", Group.SYSTEM, false, false, "8088", Format.PORT),
	SYSTEM_ROOT_PATH("SYSTEM_ROOT_PATH", "app.system.root", "資料根目錄", Group.SYSTEM, false, true, "", Format.ABSOLUTE_PATH),
	PUBLIC_BASE_URL("PUBLIC_BASE_URL", "app.public-base-url", "公開 HTTPS 網址", Group.SYSTEM, false, false, "", Format.HTTPS_URL),
	QUERY_MAX_RESULTS("QUERY_MAX_RESULTS", "app.query.max-results", "單次圖片上限", Group.SYSTEM, false, false, "4", Format.POSITIVE_INTEGER),
	ASSET_ARCHIVE_CODE_FORMATS("ASSET_ARCHIVE_CODE_FORMATS", "app.archive.code-formats", "歸檔代碼格式（#數字／@大寫字母）", Group.SYSTEM, false, false, "ZD#####,ZD#####@,ZD-JY#####,YJ######", Format.ARCHIVE_CODE_FORMATS),
	LINE_BOT_CHANNEL_TOKEN("LINE_BOT_CHANNEL_TOKEN", "line.bot.channel-token", "LINE Channel Token", Group.LINE, true, true, "", Format.NONE),
	LINE_BOT_CHANNEL_SECRET("LINE_BOT_CHANNEL_SECRET", "line.bot.channel-secret", "LINE Channel Secret", Group.LINE, true, true, "", Format.NONE),
	LINE_CONNECT_TIMEOUT_SECONDS("LINE_CONNECT_TIMEOUT_SECONDS", "app.line.connect-timeout-seconds", "LINE 連線逾時秒數", Group.LINE, false, false, "10", Format.POSITIVE_INTEGER),
	LINE_REQUEST_TIMEOUT_SECONDS("LINE_REQUEST_TIMEOUT_SECONDS", "app.line.request-timeout-seconds", "LINE 請求逾時秒數", Group.LINE, false, false, "30", Format.POSITIVE_INTEGER),
	ASSETS_SYNC_ENABLED("ASSETS_SYNC_ENABLED", "app.storage.sync-enabled", "啟用資產同步", Group.LINE, false, false, "false", Format.BOOLEAN),
	ASSETS_SYNC_INTERVAL_MS("ASSETS_SYNC_INTERVAL_MS", "app.storage.sync-interval-ms", "資產同步週期", Group.LINE, false, false, "30000", Format.POSITIVE_INTEGER),
	ASSETS_SYNC_TOKEN("ASSETS_SYNC_TOKEN", "app.storage.sync-token", "資產同步 Token", Group.LINE, true, false, "", Format.NONE),
	METHOD_TRACING_ENABLED("METHOD_TRACING_ENABLED", "app.observability.method-tracing-enabled", "方法追蹤（僅測試，會增加 CPU）", Group.LOG, false, false, "false", Format.BOOLEAN),
	LOG_LEVEL_ROOT("LOG_LEVEL_ROOT", "logging.level.root", "Log Level", Group.LOG, false, false, "INFO", Format.LOG_LEVEL),
	LOG_MAX_FILE_SIZE("LOG_MAX_FILE_SIZE", "LOG_MAX_FILE_SIZE", "Log 單檔大小", Group.LOG, false, false, "20MB", Format.DATA_SIZE),
	LOG_MAX_HISTORY("LOG_MAX_HISTORY", "LOG_MAX_HISTORY", "Log 保留天數", Group.LOG, false, false, "30", Format.POSITIVE_INTEGER),
	LOG_TOTAL_SIZE_CAP("LOG_TOTAL_SIZE_CAP", "LOG_TOTAL_SIZE_CAP", "Log 總容量", Group.LOG, false, false, "2GB", Format.DATA_SIZE),
	RESOURCE_LOG_INTERVAL_MS("RESOURCE_LOG_INTERVAL_MS", "app.observability.resource-interval-ms", "資源記錄週期", Group.LOG, false, false, "60000", Format.POSITIVE_INTEGER),
	NGROK_ENABLED("NGROK_ENABLED", "app.desktop.ngrok.enabled", "使用 ngrok", Group.NGROK, false, false, "false", Format.BOOLEAN),
	NGROK_AGENT_PATH("NGROK_AGENT_PATH", "app.desktop.ngrok.agent-path", "ngrok 執行檔", Group.NGROK, false, false, "", Format.ABSOLUTE_PATH),
	NGROK_AUTHTOKEN("NGROK_AUTHTOKEN", "app.desktop.ngrok.authtoken", "ngrok Authtoken", Group.NGROK, true, false, "", Format.NONE),
	CLOUDFLARE_ENABLED("CLOUDFLARE_ENABLED", "app.desktop.cloudflare.enabled", "使用 Cloudflare", Group.CLOUDFLARE, false, false, "false", Format.BOOLEAN),
	CLOUDFLARE_AGENT_PATH("CLOUDFLARE_AGENT_PATH", "app.desktop.cloudflare.agent-path", "cloudflared 執行檔", Group.CLOUDFLARE, false, false, "", Format.ABSOLUTE_PATH),
	CLOUDFLARE_TUNNEL_ID("CLOUDFLARE_TUNNEL_ID", "app.desktop.cloudflare.tunnel-id", "此 App 專用 Tunnel ID", Group.CLOUDFLARE, false, false, "", Format.UUID),
	CLOUDFLARE_TUNNEL_TOKEN("CLOUDFLARE_TUNNEL_TOKEN", "app.desktop.cloudflare.tunnel-token", "Cloudflare Tunnel Token", Group.CLOUDFLARE, true, false, "", Format.NONE),
	CLOUDFLARE_PROTOCOL("CLOUDFLARE_PROTOCOL", "app.desktop.cloudflare.protocol", "Cloudflare 傳輸協定", Group.CLOUDFLARE, false, false, "http2", Format.CLOUDFLARE_PROTOCOL);

	//#region 欄位

	private final String environmentKey;
	private final String propertyKey;
	private final String label;
	private final Group group;
	private final boolean secret;
	private final boolean required;
	private final String defaultValue;
	private final Format format;

	//#endregion

	//#region 建構子

	// 方法：建立單一設定欄位的完整中繼資料。
	AppConfigurationField(
		String environmentKey,
		String propertyKey,
		String label,
		Group group,
		boolean secret,
		boolean required,
		String defaultValue,
		Format format
	) {
		this.environmentKey = environmentKey;
		this.propertyKey = propertyKey;
		this.label = label;
		this.group = group;
		this.secret = secret;
		this.required = required;
		this.defaultValue = defaultValue;
		this.format = format;
	}

	//#endregion

	//#region 方法

	// 方法：取得對應的環境變數名稱。
	public String environmentKey() {
		return environmentKey;
	}

	// 方法：取得啟動 Spring 時使用的屬性名稱。
	public String propertyKey() {
		return propertyKey;
	}

	// 方法：取得設定精靈顯示的中文名稱。
	public String label() {
		return label;
	}

	// 方法：取得設定精靈的欄位群組。
	public Group group() {
		return group;
	}

	// 方法：判斷欄位是否必須加密保存。
	public boolean secret() {
		return secret;
	}

	// 方法：判斷欄位是否為啟動必要值。
	public boolean required() {
		return required;
	}

	// 方法：取得欄位的安全預設值。
	public String defaultValue() {
		return defaultValue;
	}

	// 方法：取得欄位格式規則。
	public Format format() {
		return format;
	}

	// 方法：取得所有需要加密保存的欄位。
	public static Set<AppConfigurationField> secretFields() {
		return Arrays.stream(values())
			.filter(AppConfigurationField::secret)
			.collect(Collectors.toUnmodifiableSet());
	}

	// 方法：依環境變數名稱尋找已知設定欄位，讓新版程式安全忽略未知欄位。
	public static Optional<AppConfigurationField> fromEnvironmentKey(String environmentKey) {
		return Arrays.stream(values())
			.filter(field -> field.environmentKey.equals(environmentKey))
			.findFirst();
	}

	//#endregion

	/**
	 * 設定精靈的頁面群組。
	 */
	public enum Group {
		SYSTEM,
		LINE,
		LOG,
		NGROK,
		CLOUDFLARE
	}

	/**
	 * 設定值的格式驗證種類。
	 */
	public enum Format {
		NONE,
		BOOLEAN,
		PORT,
		POSITIVE_INTEGER,
		NON_NEGATIVE_INTEGER,
		NON_NEGATIVE_DECIMAL,
		HTTP_URL,
		HTTPS_URL,
		ABSOLUTE_PATH,
		LOG_LEVEL,
		DATA_SIZE,
		UUID,
		ARCHIVE_CODE_FORMATS,
		CLOUDFLARE_PROTOCOL
	}
}
