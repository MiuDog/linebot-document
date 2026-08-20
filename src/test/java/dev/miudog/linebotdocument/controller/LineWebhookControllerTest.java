package dev.miudog.linebotdocument.controller;

import dev.miudog.linebotdocument.service.CommandService;
import dev.miudog.linebotdocument.service.ImageArchiveService;
import dev.miudog.linebotdocument.service.LineStorageService;
import dev.miudog.linebotdocument.service.voice.VoiceCommandService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LineWebhookControllerTest {

	private static final String CHANNEL_SECRET = "test-channel-secret";

	@Mock
	CommandService commandService;

	@Mock
	ImageArchiveService archiveService;

	@Mock
	LineStorageService lineService;

	@Mock
	VoiceCommandService voiceCommandService;

	LineWebhookController controller;

	@BeforeEach
	void setUp() {
		controller = new LineWebhookController(
			commandService,
			archiveService,
			lineService,
			voiceCommandService
		);
		ReflectionTestUtils.setField(controller, "channelSecret", CHANNEL_SECRET);
	}

	// 方法：未設定 channel secret 時一律拒絕，避免未簽章請求進入任何流程。
	@Test
	void rejectsEveryWebhookWhenChannelSecretIsBlank() {
		ReflectionTestUtils.setField(controller, "channelSecret", "");

		var response = controller.handleWebhook("attacker-signature", "{\"events\":[]}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(commandService, never()).handleText(any(), any(), any(), any(), any());
		verifyNoInteractions(archiveService, lineService, voiceCommandService);
	}

	// 方法：簽章不符時拒絕請求，不觸發任何下游服務。
	@Test
	void rejectsWebhookWithAnInvalidSignature() {
		var response = controller.handleWebhook("wrong-signature", "{\"events\":[]}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verifyNoInteractions(archiveService, lineService, voiceCommandService);
	}

	// 方法：群組語音轉交語音任務服務。
	@Test
	void routesGroupAudioToTheVoiceCommandService() throws Exception {
		String payload = """
			{"events":[{
			  "type":"message",
			  "replyToken":"reply-token",
			  "source":{"type":"group","groupId":"C1","userId":"U1"},
			  "message":{"id":"A1","type":"audio","duration":3500}
			}]}
			""";

		var response = controller.handleWebhook(signature(payload), payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(voiceCommandService).handleGroupAudio("A1", "C1", "reply-token");
	}

	// 方法：語音任務僅限群組，一對一語音不得觸發任何處理。
	@Test
	void ignoresDirectAudioBecauseVoiceCommandsAreGroupOnly() throws Exception {
		String payload = """
			{"events":[{
			  "type":"message",
			  "replyToken":"reply-token",
			  "source":{"type":"user","userId":"U1"},
			  "message":{"id":"A1","type":"audio","duration":3500}
			}]}
			""";

		controller.handleWebhook(signature(payload), payload);

		verifyNoInteractions(voiceCommandService);
	}

	// 方法：群組文字訊息轉交指令服務處理。
	@Test
	void routesGroupTextToTheCommandService() throws Exception {
		String payload = """
			{"events":[{
			  "type":"message",
			  "replyToken":"reply-token",
			  "source":{"type":"group","groupId":"C1","userId":"U1"},
			  "message":{"id":"M1","type":"text","text":"#標籤"}
			}]}
			""";

		controller.handleWebhook(signature(payload), payload);

		verify(commandService).handleText(
			eq("#標籤"),
			any(),
			eq("C1"),
			eq("U1"),
			eq("reply-token")
		);
	}

	// 方法：postback 屬於商用機器人的報價按鈕，本產品必須安靜忽略。
	@Test
	void ignoresPostbackBecauseItBelongsToTheCommercialBot() throws Exception {
		String payload = """
			{"events":[{
			  "type":"postback",
			  "replyToken":"reply-token",
			  "source":{"type":"user","userId":"U1"},
			  "postback":{"data":"v=1&d=1"}
			}]}
			""";

		var response = controller.handleWebhook(signature(payload), payload);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verifyNoInteractions(commandService, archiveService, lineService, voiceCommandService);
	}

	// 方法：建立測試用的 LINE 簽章。
	private static String signature(String payload) throws Exception {
		// 外部 API：使用 Java 密碼 API 建立測試用 HMAC。
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(CHANNEL_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

		return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
	}
}
