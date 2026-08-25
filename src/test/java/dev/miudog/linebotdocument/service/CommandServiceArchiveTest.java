package dev.miudog.linebotdocument.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandServiceArchiveTest {

	@Mock
	AssetService assetService;

	@Mock
	LineStorageService lineService;


	@Mock
	ImageArchiveService archiveService;

	CommandService commandService;

	// 方法：以正式預設規則建立指令服務。
	@BeforeEach
	void setUp() {
		commandService = new CommandService(
			assetService,
			lineService,
			archiveService,
			defaultArchivePolicy()
		);
	}

	// 方法：驗證客戶可替換歸檔代碼規則，且錯誤提示同步使用設定範例。
	@Test
	void usesConfiguredArchiveCodeAndExample() throws Exception {
		commandService = new CommandService(
			assetService,
			lineService,
			archiveService,
			new AssetArchivePolicy("AC####")
		);
		when(archiveService.archive("M1", "C1", "AC1234"))
			.thenReturn(new ImageArchiveService.ArchiveResult(ImageArchiveService.ArchiveStatus.ARCHIVED, "AC1234", 1, 1, 0, "01", "01"));

		commandService.handleText("AC1234", "M1", "C1", "U1", "R1");
		commandService.handleText("AC12", "M2", "C1", "U1", "R2");

		verify(archiveService).archive("M1", "C1", "AC1234");
		verify(lineService).replyText("R2", "檢測到語法錯誤，請修正後重新執行指令。格式例如：AC1234");
	}

	// 方法：驗證超過安全長度的文字不會進入客戶 regex 或觸發歸檔流程。
	@Test
	void ignoresArchiveCandidatesOverTheSafetyLimit() {
		commandService.handleText("ZD" + "1".repeat(1000), "M1", "C1", "U1", "R1");

		verifyNoInteractions(archiveService, lineService);
	}

	@Test
	void archivesTheQuotedImageSetForEveryAllowedFolderFormat() throws Exception {
		when(archiveService.archive("M1", "C1", "ZD12345"))
			.thenReturn(new ImageArchiveService.ArchiveResult(ImageArchiveService.ArchiveStatus.ARCHIVED, "ZD12345", 3, 3, 0, "01", "03"));
		when(archiveService.archive("M2", "C1", "ZD12345A"))
			.thenReturn(new ImageArchiveService.ArchiveResult(ImageArchiveService.ArchiveStatus.ARCHIVED, "ZD12345A", 2, 2, 0, "08", "09"));
		when(archiveService.archive("M3", "C1", "ZD-JY12345"))
			.thenReturn(new ImageArchiveService.ArchiveResult(ImageArchiveService.ArchiveStatus.ARCHIVED, "ZD-JY12345", 1, 1, 0, "100", "100"));
		when(archiveService.archive("M4", "C1", "YJ123456"))
			.thenReturn(new ImageArchiveService.ArchiveResult(ImageArchiveService.ArchiveStatus.ARCHIVED, "YJ123456", 4, 4, 0, "21", "24"));

		commandService.handleText("ZD12345", "M1", "C1", "U1", "R1");
		commandService.handleText("ZD12345A", "M2", "C1", "U1", "R2");
		commandService.handleText("ZD-JY12345", "M3", "C1", "U1", "R3");
		commandService.handleText("YJ123456", "M4", "C1", "U1", "R4");

		verify(lineService).replyText("R1", "歸檔成功：已將3張圖片存入「ZD12345」，流水號01至03。");
		verify(lineService).replyText("R2", "歸檔成功：已將2張圖片存入「ZD12345A」，流水號08至09。");
		verify(lineService).replyText("R3", "歸檔成功：已將1張圖片存入「ZD-JY12345」，流水號100至100。");
		verify(lineService).replyText("R4", "歸檔成功：已將4張圖片存入「YJ123456」，流水號21至24。");
	}

	@Test
	void ignoresLowercaseArchiveCodes() {
		commandService.handleText("zd12345", "M1", "C1", "U1", "R1");
		commandService.handleText("Zd12345A", "M2", "C1", "U1", "R2");

		verifyNoInteractions(archiveService, lineService);
	}

	@Test
	void reportsSyntaxErrorsForRecognizedPrefixesWithInvalidNumbers() {
		commandService.handleText("ZD1234", "M1", "C1", "U1", "R1");
		commandService.handleText("ZD-JY123456", "M2", "C1", "U1", "R2");
		commandService.handleText("YJ12345", "M3", "C1", "U1", "R3");
		commandService.handleText("ZD20260730", "M4", "C1", "U1", "R4");

		verify(lineService).replyText("R1", "檢測到語法錯誤，請修正後重新執行指令。格式例如：ZD12345");
		verify(lineService).replyText("R2", "檢測到語法錯誤，請修正後重新執行指令。格式例如：ZD12345");
		verify(lineService).replyText("R3", "檢測到語法錯誤，請修正後重新執行指令。格式例如：ZD12345");
		verify(lineService).replyText("R4", "檢測到語法錯誤，請修正後重新執行指令。格式例如：ZD12345");
		verifyNoInteractions(archiveService);
	}

	@Test
	void explainsWhenNoImageInTheSetCouldBeDownloaded() throws Exception {
		when(archiveService.archive("M1", "C1", "ZD12345"))
			.thenReturn(new ImageArchiveService.ArchiveResult(ImageArchiveService.ArchiveStatus.INCOMPLETE_SET, "ZD12345", 0, 3, 0, "", ""));

		commandService.handleText("ZD12345", "M1", "C1", "U1", "reply-token");

		verify(lineService).replyText(
			"reply-token",
			"無法歸檔：LINE 顯示此圖片組共3張，但目前沒有任何圖片下載成功。請重新上傳圖片後再執行指令。"
		);
	}

	@Test
	void reportsMissingCountAfterAvailableImagesWereArchived() throws Exception {
		when(archiveService.archive("M1", "C1", "ZD12345"))
			.thenReturn(new ImageArchiveService.ArchiveResult(ImageArchiveService.ArchiveStatus.ARCHIVED, "ZD12345", 2, 3, 0, "01", "02"));

		commandService.handleText("ZD12345", "M1", "C1", "U1", "reply-token");

		verify(lineService).replyText(
			"reply-token",
			"部分歸檔完成：已將2張圖片存入「ZD12345」，流水號01至02；LINE 顯示此圖片組共3張，其中1張未能下載。"
		);
	}

	@Test
	void repliesWhenAValidCommandDoesNotQuoteAnImage() {
		commandService.handleText(
			"ZD12345",
			null,
			"C1",
			"U1",
			"reply-token"
		);

		verify(lineService).replyText(
			"reply-token",
			"無法歸檔：尚未回覆圖片。請先回覆要儲存的圖片，再輸入資料夾代碼。"
		);
		verifyNoInteractions(archiveService);
	}

	@Test
	void repliesForEveryNonSuccessfulArchiveResult() throws Exception {
		when(archiveService.archive("missing", "C1", "ZD12345"))
			.thenReturn(
				new ImageArchiveService.ArchiveResult(
					ImageArchiveService.ArchiveStatus.NOT_FOUND,
					"ZD12345",
					0,
					0,
					0,
					"",
					""
				)
			);
		when(archiveService.archive("wrong-source", "C1", "ZD12345"))
			.thenReturn(
				new ImageArchiveService.ArchiveResult(
					ImageArchiveService.ArchiveStatus.WRONG_SOURCE,
					"ZD12345",
					0,
					0,
					0,
					"",
					""
				)
			);

		commandService.handleText(
			"ZD12345",
			"missing",
			"C1",
			"U1",
			"R1"
		);
		commandService.handleText(
			"ZD12345",
			"wrong-source",
			"C1",
			"U1",
			"R3"
		);

		verify(lineService).replyText(
			"R1",
			"無法歸檔：找不到被回覆圖片，可能尚未下載完成、已被刪除，或暫存紀錄已清除。請重新上傳後再試。"
		);
		verify(lineService).replyText(
			"R3",
			"無法歸檔：被回覆圖片來自其他群組，不能存入目前群組的資料。"
		);
	}

	@Test
	void repliesWhenArchiveWritingFails() throws Exception {
		doThrow(new IOException("disk failure"))
			.when(archiveService)
			.archive("M1", "C1", "ZD12345");

		commandService.handleText(
			"ZD12345",
			"M1",
			"C1",
			"U1",
			"reply-token"
		);

		verify(lineService).replyText(
			"reply-token",
			"圖片歸檔失敗：無法寫入圖片檔案。本次未完成歸檔，請稍後再試。"
		);
	}

	@Test
	void explainsStoragePermissionFailuresInChinese() throws Exception {
		doThrow(new AccessDeniedException("E:/圖片資產/ZD12345"))
			.when(archiveService)
			.archive("M1", "C1", "ZD12345");

		commandService.handleText("ZD12345", "M1", "C1", "U1", "reply-token");

		verify(lineService).replyText(
			"reply-token",
			"圖片歸檔失敗：圖片資料夾沒有寫入權限。本次未完成歸檔，請通知管理員檢查儲存路徑權限。"
		);
	}

	@Test
	void explainsMissingTemporaryImagesInChinese() throws Exception {
		doThrow(new NoSuchFileException(".pending/missing.jpg"))
			.when(archiveService)
			.archive("M1", "C1", "ZD12345");

		commandService.handleText("ZD12345", "M1", "C1", "U1", "reply-token");

		verify(lineService).replyText(
			"reply-token",
			"圖片歸檔失敗：找不到暫存圖片，圖片可能已被移動或刪除。本次未完成歸檔，請重新上傳後再試。"
		);
	}

	@Test
	void explainsInsufficientStorageSpaceInChinese() throws Exception {
		FileSystemException failure =
			new FileSystemException("E:/圖片資產/ZD12345", null, "No space left on device");
		doThrow(failure)
			.when(archiveService)
			.archive("M1", "C1", "ZD12345");

		commandService.handleText("ZD12345", "M1", "C1", "U1", "reply-token");

		verify(lineService).replyText(
			"reply-token",
			"圖片歸檔失敗：圖片儲存空間不足。本次未完成歸檔，請通知管理員清理或擴充磁碟空間。"
		);
	}

	@Test
	void explainsDatabaseOrStateFailuresWithoutShowingTechnicalDetails() throws Exception {
		doThrow(new IllegalStateException("database constraint details"))
			.when(archiveService)
			.archive("M1", "C1", "ZD12345");

		commandService.handleText("ZD12345", "M1", "C1", "U1", "reply-token");

		verify(lineService).replyText(
			"reply-token",
			"圖片歸檔失敗：資料紀錄發生異常。本次未完成歸檔，請稍後再試；若持續發生請通知管理員。"
		);
	}

	// 方法：建立既有 ZD／ZD-JY／YJ 歸檔規則供各測試共用。
	private AssetArchivePolicy defaultArchivePolicy() {
		return new AssetArchivePolicy("ZD#####,ZD#####@,ZD-JY#####,YJ######");
	}
}
