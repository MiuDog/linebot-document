package dev.miudog.linebotdocument.companyasset;

import dev.miudog.linebotdocument.config.runtime.CompanyProperties;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 圖片匯入批次、metadata revision 與稽核事件的資料庫邊界。
 */
@Repository
public class AssetImportRepository {
	private static final ObjectMapper JSON = new ObjectMapper();

	private final JdbcClient jdbc;
	private final String companyId;

	// 方法：執行此方法定義的受控處理流程。
	public AssetImportRepository(JdbcClient jdbc, CompanyProperties companyProperties) {
		this.jdbc = jdbc;
		this.companyId = companyProperties.id();
	}

	// 方法：執行此方法定義的受控處理流程。
	public long createStaged(
		AssetImportManifest manifest,
		String manifestHash,
		String actor,
		List<ImportObject> objects
	) {
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.sql("""
				INSERT INTO asset_import_batch (
					company_id, batch_id, schema_version, manifest_hash, status, created_by
				)
				VALUES (?, ?, ?, ?, 'STAGED', ?)
				""")
			.params(
				companyId,
				manifest.batchId(),
				manifest.schemaVersion(),
				manifestHash,
				actor
			)
			.update(keys);
		Number key = keys.getKey();
		if (key == null) throw new IllegalStateException("無法建立資產匯入批次");

		long databaseId = key.longValue();
		for (ImportObject object : objects) {
			jdbc.sql("""
					INSERT INTO asset_import_object (
						import_batch_id, asset_external_id, asset_code, folder_code,
						tags_json, source_type, source_id, source_event_id,
						file_name, object_key, object_version,
						content_type, file_size, content_hash
					)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""")
				.params(
					databaseId,
					object.assetExternalId(),
					object.assetCode(),
					object.folderCode(),
					object.tagsJson(),
					object.sourceType(),
					object.sourceId(),
					object.sourceEventId(),
					object.fileName(),
					object.objectKey(),
					object.objectVersion(),
					object.contentType(),
					object.size(),
					object.sha256()
				)
				.update();
		}
		audit(databaseId, null, "STAGED", actor, "{\"objectCount\":" + objects.size() + "}");
		return databaseId;
	}

	// 方法：執行此方法定義的受控處理流程。
	public Batch required(String batchId) {
		return find(batchId)
			.orElseThrow(() -> new IllegalArgumentException("找不到資產匯入批次"));
	}

	// 方法：執行此方法定義的受控處理流程。
	public Optional<Batch> find(String batchId) {
		return jdbc.sql("""
				SELECT id, batch_id, schema_version, manifest_hash, status,
					created_by, created_at, committed_at
				FROM asset_import_batch
				WHERE company_id = ? AND batch_id = ?
				""")
			.params(companyId, batchId)
			.query((result, rowNumber) -> new Batch(
				result.getLong("id"),
				result.getString("batch_id"),
				result.getString("schema_version"),
				result.getString("manifest_hash"),
				result.getString("status"),
				result.getString("created_by"),
				result.getString("created_at"),
				result.getString("committed_at")
			))
			.optional();
	}

	// 方法：執行此方法定義的受控處理流程。
	public List<Batch> list() {
		return jdbc.sql("""
				SELECT id, batch_id, schema_version, manifest_hash, status,
					created_by, created_at, committed_at
				FROM asset_import_batch
				WHERE company_id = ?
				ORDER BY id DESC
				""")
			.param(companyId)
			.query((result, rowNumber) -> new Batch(
				result.getLong("id"),
				result.getString("batch_id"),
				result.getString("schema_version"),
				result.getString("manifest_hash"),
				result.getString("status"),
				result.getString("created_by"),
				result.getString("created_at"),
				result.getString("committed_at")
			))
			.list();
	}

	// 方法：執行此方法定義的受控處理流程。
	public List<ImportObject> objects(long databaseBatchId) {
		return jdbc.sql("""
				SELECT asset_external_id, asset_code, folder_code, tags_json,
					source_type, source_id, source_event_id,
					file_name, object_key, object_version,
					content_type, file_size, content_hash
				FROM asset_import_object
				WHERE import_batch_id = ?
				ORDER BY id
				""")
			.param(databaseBatchId)
			.query((result, rowNumber) -> new ImportObject(
				result.getString("asset_external_id"),
				result.getString("asset_code"),
				result.getString("folder_code"),
				result.getString("tags_json"),
				result.getString("source_type"),
				result.getString("source_id"),
				result.getString("source_event_id"),
				result.getString("file_name"),
				result.getString("object_key"),
				result.getString("object_version"),
				result.getString("content_type"),
				result.getLong("file_size"),
				result.getString("content_hash")
			))
			.list();
	}

	// 方法：執行此方法定義的受控處理流程。
	public void promoteObject(
		long databaseBatchId,
		String assetExternalId,
		String objectKey,
		String objectVersion
	) {
		jdbc.sql("""
				UPDATE asset_import_object
				SET object_key = ?, object_version = ?
				WHERE import_batch_id = ? AND asset_external_id = ?
				""")
			.params(objectKey, objectVersion, databaseBatchId, assetExternalId)
			.update();
	}

	// 方法：執行此方法定義的受控處理流程。
	public void markValidated(long databaseBatchId, String actor) {
		int updated = jdbc.sql("""
				UPDATE asset_import_batch
				SET status = 'VALIDATED'
				WHERE id = ? AND company_id = ? AND status = 'STAGED'
				""")
			.params(databaseBatchId, companyId)
			.update();
		if (updated != 1) throw new IllegalStateException("只有 STAGED 批次可驗證");

		audit(databaseBatchId, null, "VALIDATED", actor, "{}");
	}

	// 方法：執行此方法定義的受控處理流程。
	@Transactional
	public void commit(long databaseBatchId, String actor) {
		Batch batch = requiredById(databaseBatchId);
		if (!"VALIDATED".equals(batch.status())) {
			throw new IllegalStateException("只有 VALIDATED 批次可提交");
		}

		for (ImportObject object : objects(databaseBatchId)) {
			KeyHolder keys = new GeneratedKeyHolder();
			String messageId = object.sourceEventId() == null || object.sourceEventId().isBlank()
				? "import:" + batch.batchId() + ":" + object.assetExternalId()
				: object.sourceEventId();
			jdbc.sql("""
					INSERT INTO asset (
						message_id, share_token, source_type, source_id, uploader_id,
						file_path, content_type, file_size, created_at,
						company_id, object_key, object_version, content_hash,
						import_batch_id, status
					)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
					""")
				.params(
					messageId,
					UUID.randomUUID().toString().replace("-", ""),
					object.sourceType(),
					object.sourceId(),
					actor,
					object.objectKey(),
					object.contentType(),
					object.size(),
					Instant.now().toString(),
					companyId,
					object.objectKey(),
					object.objectVersion(),
					object.sha256(),
					databaseBatchId
				)
				.update(keys);
			Number key = keys.getKey();
			if (key == null) throw new IllegalStateException("無法建立匯入資產");

			long assetId = key.longValue();
			linkTag(assetId, object.assetCode());
			for (String tag : parseTags(object.tagsJson())) linkTag(assetId, tag);
			jdbc.sql("""
					INSERT INTO asset_metadata_revision (
						asset_id, revision, folder_code, tags_json, actor
					)
					VALUES (?, 1, ?, ?, ?)
					""")
				.params(assetId, object.folderCode(), object.tagsJson(), actor)
				.update();
			audit(databaseBatchId, assetId, "COMMITTED_ASSET", actor, "{}");
		}

		jdbc.sql("""
				UPDATE asset_import_batch
				SET status = 'COMMITTED', committed_at = ?
				WHERE id = ? AND company_id = ? AND status = 'VALIDATED'
				""")
			.params(Instant.now().toString(), databaseBatchId, companyId)
			.update();
		audit(databaseBatchId, null, "COMMITTED", actor, "{}");
	}

	// 方法：執行此方法定義的受控處理流程。
	private void linkTag(long assetId, String tag) {
		jdbc.sql("INSERT INTO tag (name) VALUES (?) ON CONFLICT (name) DO NOTHING")
			.param(tag)
			.update();
		Long tagId = jdbc.sql("SELECT id FROM tag WHERE name = ?")
			.param(tag)
			.query(Long.class)
			.single();
		jdbc.sql("""
				INSERT INTO asset_tag (asset_id, tag_id)
				VALUES (?, ?)
				ON CONFLICT (asset_id, tag_id) DO NOTHING
				""")
			.params(assetId, tagId)
			.update();
	}

	// 方法：執行此方法定義的受控處理流程。
	private void audit(
		Long batchId,
		Long assetId,
		String action,
		String actor,
		String summary
	) {
		jdbc.sql("""
				INSERT INTO asset_audit_event (
					company_id, asset_id, batch_id, action, actor, summary_json
				)
				VALUES (?, ?, ?, ?, ?, ?)
				""")
			.params(companyId, assetId, batchId, action, actor, summary)
			.update();
	}

	// 方法：執行此方法定義的受控處理流程。
	private Batch requiredById(long id) {
		return jdbc.sql("""
				SELECT id, batch_id, schema_version, manifest_hash, status,
					created_by, created_at, committed_at
				FROM asset_import_batch
				WHERE company_id = ? AND id = ?
				""")
			.params(companyId, id)
			.query((result, rowNumber) -> new Batch(
				result.getLong("id"),
				result.getString("batch_id"),
				result.getString("schema_version"),
				result.getString("manifest_hash"),
				result.getString("status"),
				result.getString("created_by"),
				result.getString("created_at"),
				result.getString("committed_at")
			))
			.optional()
			.orElseThrow(() -> new IllegalArgumentException("找不到資產匯入批次"));
	}

	// 方法：執行此方法定義的受控處理流程。
	private static List<String> parseTags(String value) {
		if (value == null || value.length() < 2) return List.of();

		try {
			return JSON.readValue(value, new TypeReference<List<String>>() {});
		}
		catch (RuntimeException exception) {
			throw new IllegalStateException("匯入標籤 JSON 無法解析", exception);
		}
	}

	public record Batch(
		long id,
		String batchId,
		String schemaVersion,
		String manifestHash,
		String status,
		String createdBy,
		String createdAt,
		String committedAt
	) {
	}

	public record ImportObject(
		String assetExternalId,
		String assetCode,
		String folderCode,
		String tagsJson,
		String sourceType,
		String sourceId,
		String sourceEventId,
		String fileName,
		String objectKey,
		String objectVersion,
		String contentType,
		long size,
		String sha256
	) {
	}
}
