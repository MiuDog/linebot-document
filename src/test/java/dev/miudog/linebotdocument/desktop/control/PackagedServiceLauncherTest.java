package dev.miudog.linebotdocument.desktop.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 驗證背景 service launcher 只能解析桌面執行檔同目錄下的固定檔名。
 */
class PackagedServiceLauncherTest {

	// 方法：將固定 service 檔名解析為桌面執行檔旁的絕對路徑。
	@Test
	void shouldResolveServiceBesideDesktopExecutable() {
		Path desktopExecutable = Path.of("C:/Apps/LinebotDocument/LinebotDocument.exe");

		Path serviceExecutable = PackagedServiceLauncher.resolveServiceExecutable(
			desktopExecutable,
			"LinebotDocumentService.exe"
		);

		assertThat(serviceExecutable).isEqualTo(
			Path.of("C:/Apps/LinebotDocument/LinebotDocumentService.exe").toAbsolutePath().normalize()
		);
	}

	// 方法：拒絕含路徑分隔符的 service 名稱，避免 launcher 逸出安裝目錄。
	@Test
	void shouldRejectServiceNameContainingPathSegments() {
		assertThatThrownBy(() -> PackagedServiceLauncher.resolveServiceExecutable(
			Path.of("C:/Apps/LinebotDocument/LinebotDocument.exe"),
			"../malicious.exe"
		)).isInstanceOf(IllegalArgumentException.class);
	}
}
