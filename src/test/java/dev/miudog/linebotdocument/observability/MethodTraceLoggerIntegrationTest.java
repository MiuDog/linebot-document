package dev.miudog.linebotdocument.observability;

import dev.miudog.linebotdocument.service.ai.AiExtractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class MethodTraceLoggerIntegrationTest {

	@Autowired
	AiExtractionService aiExtractionService;

	@Test
	void tracesPublicMethodsOnSpringManagedApplicationClasses(CapturedOutput output) {
		aiExtractionService.isConfigured();

		assertThat(output)
			.contains("event=method_entered")
			.contains("event=method_completed")
			.contains("class=AiExtractionService")
			.contains("method=isConfigured")
			.contains("requestId=background-");
	}
}
