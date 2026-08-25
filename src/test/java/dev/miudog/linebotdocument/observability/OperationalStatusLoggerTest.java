package dev.miudog.linebotdocument.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class OperationalStatusLoggerTest {

	@Test
	void logsConfigurationReadinessWithoutExposingConfiguredValues(CapturedOutput output) {
		OperationalStatusLogger statusLogger = new OperationalStatusLogger("https://private.example.com");

		statusLogger.logApplicationReady();

		assertThat(output)
			.contains("event=application_ready")
			.contains("requestId=startup")
			.contains("publicBaseUrlConfigured=true")
			.doesNotContain("private.example.com");
	}
}
