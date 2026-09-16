package dev.miudog.linebotdocument.controller;

import dev.miudog.linebotdocument.companyasset.AssetImportRepository.Batch;
import dev.miudog.linebotdocument.companyasset.AssetImportService;
import dev.miudog.linebotdocument.companyasset.AssetImportService.ExportBundle;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 受網路與管理 token 雙重保護的圖片資產批次 API。
 */
@RestController
@RequestMapping("/api/admin/asset-imports")
public class AssetImportAdminController {

	private final AssetImportService service;

	// 方法：執行此方法定義的受控處理流程。
	public AssetImportAdminController(AssetImportService service) {
		this.service = service;
	}

	// 方法：執行此方法定義的受控處理流程。
	@PostMapping(value = "/stages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Batch stage(
		@RequestPart("manifest") MultipartFile manifest,
		@RequestPart("files") List<MultipartFile> files,
		@RequestHeader("X-Admin-Actor") String actor
	) throws IOException {
		return service.stage(manifest.getBytes(), files, actor);
	}

	// 方法：執行此方法定義的受控處理流程。
	@PostMapping("/{batchId}/validation")
	public Batch validate(
		@PathVariable String batchId,
		@RequestHeader("X-Admin-Actor") String actor
	) {
		return service.validateAndPromote(batchId, actor);
	}

	// 方法：執行此方法定義的受控處理流程。
	@PostMapping("/{batchId}/commit")
	public Batch commit(
		@PathVariable String batchId,
		@RequestHeader("X-Admin-Actor") String actor
	) {
		return service.commit(batchId, actor);
	}

	// 方法：執行此方法定義的受控處理流程。
	@GetMapping
	public List<Batch> list() {
		return service.list();
	}

	// 方法：執行此方法定義的受控處理流程。
	@GetMapping("/{batchId}/export")
	public ResponseEntity<byte[]> export(@PathVariable String batchId) {
		ExportBundle bundle = service.export(batchId);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType("application/zip"))
			.header(
				HttpHeaders.CONTENT_DISPOSITION,
				ContentDisposition.attachment()
					.filename(bundle.fileName(), StandardCharsets.UTF_8)
					.build()
					.toString()
			)
			.body(bundle.content());
	}
}
