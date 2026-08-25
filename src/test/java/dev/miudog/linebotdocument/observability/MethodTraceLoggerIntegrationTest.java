package dev.miudog.linebotdocument.observability;

import dev.miudog.linebotdocument.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
	"app.observability.method-tracing-enabled=true",
	"logging.level.FLOW_TRACE=DEBUG"
})
@ExtendWith(OutputCaptureExtension.class)
class MethodTraceLoggerIntegrationTest {

	@Autowired
	FileStorageService fileStorageService;

	@Test
	void tracesPublicMethodsOnSpringManagedApplicationClasses(CapturedOutput output) {
		fileStorageService.root();

		assertThat(output)
			.contains("event=method_entered")
			.contains("event=method_completed")
			.contains("class=FileStorageService")
			.contains("method=root")
			.contains("requestId=background-");
	}
}
