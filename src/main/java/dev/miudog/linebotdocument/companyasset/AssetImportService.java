package dev.miudog.linebotdocument.companyasset;

import dev.miudog.linebotdocument.companyasset.AssetImportManifest.AssetObject;
import dev.miudog.linebotdocument.companyasset.AssetImportRepository.Batch;
import dev.miudog.linebotdocument.companyasset.AssetImportRepository.ImportObject;
import dev.miudog.linebotdocument.config.runtime.CompanyProperties;
import dev.miudog.linebotdocument.storage.ObjectStorage;
import dev.miudog.linebotdocument.storage.StoredObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 圖片資產包的 stage、驗證、原子提交、完整匯出與重新匯入流程。
 */
@Service
public class AssetImportService {

	private static final String SCHEMA_VERSION = "1";
	private static final long MAXIMUM_IMAGE_BYTES = 20L * 1024L * 1024L;

	private final CompanyProperties company;
	private final ObjectStorage storage;
	private final AssetImportRepository repository;
	private final ObjectMapper mapper;

	// 方法：執行此方法定義的受控處理流程。
	public AssetImportService(
		CompanyProperties company,
		ObjectStorage storage,
		AssetImportRepository repository,
		ObjectMapper mapper
	) {
		this.company = company;
		this.storage = storage;
		this.repository = repository;
		this.mapper = mapper;
	}

	// 方法：執行此方法定義的受控處理流程。
	@Transactional
	public Batch stage(byte[] manifestBytes, List<MultipartFile> files, String actor) {
		actor = requireActor(actor);
		AssetImportManifest manifest = parse(manifestBytes);
		validateManifest(manifest);
		Map<String, MultipartFile> byName = filesByName(files);
		List<String> staged = new ArrayList<>();
		List<ImportObject> objects = new ArrayList<>();

		try {
			for (AssetObject declared : manifest.objects()) {
				MultipartFile file = byName.remove(declared.fileName());
				if (file == null) throw new IllegalArgumentException("manifest 宣告檔案不存在");

				byte[] content = file.getBytes();
				validateImage(declared, content, file.getContentType());
				String key = stagingKey(manifest.batchId(), declared.assetExternalId());
				StoredObject stored = storage.put(
					key,
					content,
					declared.contentType(),
					Map.of(
						"batch-id", manifest.batchId(),
						"asset-external-id", declared.assetExternalId()
					)
				);
				staged.add(key);
				objects.add(new ImportObject(
					declared.assetExternalId(),
					declared.assetCode(),
					declared.folderCode(),
					mapper.writeValueAsString(declared.tags() == null ? List.of() : declared.tags()),
					declared.sourceType(),
					declared.sourceId(),
					declared.sourceEventId(),
					declared.fileName(),
					key,
					stored.versionId(),
					declared.contentType(),
					declared.size(),
					declared.sha256().toLowerCase()
				));
			}
			if (!byName.isEmpty()) throw new IllegalArgumentException("資產包含 manifest 未宣告檔案");

			repository.createStaged(manifest, sha256(manifestBytes), actor, objects);
			storage.put(
				"asset-imports/staging/" + manifest.batchId() + "/manifest.json",
				manifestBytes,
				"application/json",
				Map.of("batch-id", manifest.batchId())
			);
			return repository.required(manifest.batchId());
		}
		catch (IOException | RuntimeException exception) {
			for (String key : staged) {
				try {
					storage.delete(key);
				}
				catch (RuntimeException ignored) {
					// reconciliation 可重試補償。
				}
			}
			if (exception instanceof RuntimeException runtime) throw runtime;

			throw new IllegalStateException("無法讀取資產匯入包", exception);
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	@Transactional
	public Batch validateAndPromote(String batchId, String actor) {
		actor = requireActor(actor);
		Batch batch = repository.required(batchId);
		if (!"STAGED".equals(batch.status())) throw new IllegalStateException("只有 STAGED 批次可驗證");

		List<String> promoted = new ArrayList<>();
		try {
			for (ImportObject object : repository.objects(batch.id())) {
				StoredObject actual = storage.metadata(object.objectKey());
				if (
					actual.contentLength() != object.size()
						|| !object.sha256().equalsIgnoreCase(actual.sha256())
				) {
					throw new IllegalStateException("圖片資產完整性驗證失敗");
				}
				String target = finalKey(batchId, object.assetExternalId());
				storage.copy(object.objectKey(), target);
				StoredObject copied = storage.metadata(target);
				if (
					copied.contentLength() != object.size()
						|| !object.sha256().equalsIgnoreCase(copied.sha256())
				) {
					throw new IllegalStateException("圖片資產 promote 驗證失敗");
				}
				repository.promoteObject(
					batch.id(),
					object.assetExternalId(),
					target,
					copied.versionId()
				);
				promoted.add(target);
			}
			storage.copy(
				"asset-imports/staging/" + batchId + "/manifest.json",
				"asset-imports/batches/" + batchId + "/manifest.json"
			);
			repository.markValidated(batch.id(), actor);
			return repository.required(batchId);
		}
		catch (RuntimeException exception) {
			for (String key : promoted) {
				try {
					storage.delete(key);
				}
				catch (RuntimeException ignored) {
					// reconciliation 可重試補償。
				}
			}
			throw exception;
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	public Batch commit(String batchId, String actor) {
		Batch batch = repository.required(batchId);
		repository.commit(batch.id(), requireActor(actor));
		return repository.required(batchId);
	}

	// 方法：執行此方法定義的受控處理流程。
	public List<Batch> list() {
		return repository.list();
	}

	// 方法：執行此方法定義的受控處理流程。
	public ExportBundle export(String batchId) {
		Batch batch = repository.required(batchId);
		if (!"COMMITTED".equals(batch.status())) {
			throw new IllegalStateException("只有已提交批次可完整匯出");
		}

		List<AssetObject> manifestObjects = new ArrayList<>();
		List<ExportObject> contents = new ArrayList<>();
		for (ImportObject object : repository.objects(batch.id())) {
			byte[] content = storage.get(object.objectKey());
			List<String> tags = readTags(object.tagsJson());
			manifestObjects.add(new AssetObject(
				object.assetExternalId(),
				object.assetCode(),
				object.folderCode(),
				tags,
				object.sourceType(),
				object.sourceId(),
				object.sourceEventId(),
				object.fileName(),
				object.contentType(),
				object.size(),
				object.sha256()
			));
			contents.add(new ExportObject(object.fileName(), content));
		}
		AssetImportManifest manifest = new AssetImportManifest(
			SCHEMA_VERSION,
			company.id(),
			batch.batchId(),
			manifestObjects
		);

		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (ZipOutputStream zip = new ZipOutputStream(output)) {
				zip.putNextEntry(new ZipEntry("manifest.json"));
				zip.write(mapper.writeValueAsBytes(manifest));
				zip.closeEntry();
				for (ExportObject object : contents) {
					zip.putNextEntry(new ZipEntry("objects/" + object.fileName()));
					zip.write(object.content());
					zip.closeEntry();
				}
			}
			return new ExportBundle("asset-batch-" + batchId + ".zip", output.toByteArray());
		}
		catch (IOException exception) {
			throw new IllegalStateException("無法建立資產匯出包", exception);
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	private AssetImportManifest parse(byte[] bytes) {
		try {
			return mapper.readValue(bytes, AssetImportManifest.class);
		}
		catch (RuntimeException exception) {
			throw new IllegalArgumentException("manifest 不是有效 JSON", exception);
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	private void validateManifest(AssetImportManifest manifest) {
		if (!SCHEMA_VERSION.equals(manifest.schemaVersion())) {
			throw new IllegalArgumentException("不支援的 manifest schemaVersion");
		}
		if (!company.id().equals(manifest.companyId())) {
			throw new IllegalArgumentException("manifest companyId 與部署不符");
		}
		if (manifest.batchId() == null || !manifest.batchId().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
			throw new IllegalArgumentException("batchId 格式不合法");
		}
		if (manifest.objects() == null || manifest.objects().isEmpty()) {
			throw new IllegalArgumentException("manifest 不得沒有物件");
		}
		Set<String> ids = new HashSet<>();
		Set<String> names = new HashSet<>();
		for (AssetObject object : manifest.objects()) {
			if (
				object.assetExternalId() == null
					|| !object.assetExternalId().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")
					|| !ids.add(object.assetExternalId())
			) {
				throw new IllegalArgumentException("資產外部識別重複或不合法");
			}
			if (object.assetCode() == null || !object.assetCode().matches("[A-Za-z0-9-]{1,64}")) {
				throw new IllegalArgumentException("資產編號格式不合法");
			}
			if (
				object.fileName() == null
					|| !object.fileName().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
					|| !names.add(object.fileName())
			) {
				throw new IllegalArgumentException("資產檔名重複或不安全");
			}
			if (object.contentType() == null || !object.contentType().startsWith("image/")) {
				throw new IllegalArgumentException("只允許圖片 MIME");
			}
			if (object.size() <= 0 || object.size() > MAXIMUM_IMAGE_BYTES) {
				throw new IllegalArgumentException("圖片大小超出允許範圍");
			}
			if (object.sha256() == null || !object.sha256().matches("[0-9a-fA-F]{64}")) {
				throw new IllegalArgumentException("圖片 SHA-256 格式不合法");
			}
			validateTags(object.tags());
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	private void validateImage(AssetObject object, byte[] content, String uploadedType) {
		if (content.length != object.size()) throw new IllegalArgumentException("圖片大小不符");

		if (!sha256(content).equalsIgnoreCase(object.sha256())) {
			throw new IllegalArgumentException("圖片 SHA-256 不符");
		}
		if (
			uploadedType != null
				&& !uploadedType.equals("application/octet-stream")
				&& !uploadedType.startsWith("image/")
		) {
			throw new IllegalArgumentException("上傳 MIME 不是圖片");
		}
		if (!isSupportedImage(content)) throw new IllegalArgumentException("圖片 magic bytes 不支援");
	}

	// 方法：執行此方法定義的受控處理流程。
	private static boolean isSupportedImage(byte[] content) {
		boolean jpeg = content.length >= 3
			&& (content[0] & 0xff) == 0xff
			&& (content[1] & 0xff) == 0xd8
			&& (content[2] & 0xff) == 0xff;
		boolean png = content.length >= 8
			&& (content[0] & 0xff) == 0x89
			&& content[1] == 'P'
			&& content[2] == 'N'
			&& content[3] == 'G';
		boolean gif = content.length >= 6
			&& content[0] == 'G'
			&& content[1] == 'I'
			&& content[2] == 'F';
		boolean webp = content.length >= 12
			&& content[0] == 'R'
			&& content[1] == 'I'
			&& content[2] == 'F'
			&& content[3] == 'F'
			&& content[8] == 'W'
			&& content[9] == 'E'
			&& content[10] == 'B'
			&& content[11] == 'P';
		return jpeg || png || gif || webp;
	}

	// 方法：執行此方法定義的受控處理流程。
	private static void validateTags(List<String> tags) {
		if (tags == null) return;

		if (tags.size() > 30) throw new IllegalArgumentException("單一資產標籤過多");

		for (String tag : tags) {
			if (tag == null || tag.isBlank() || tag.length() > 80) {
				throw new IllegalArgumentException("標籤格式不合法");
			}
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	private List<String> readTags(String json) {
		try {
			return mapper.readValue(json, new TypeReference<List<String>>() {});
		}
		catch (RuntimeException exception) {
			throw new IllegalStateException("資料庫標籤 JSON 已損壞", exception);
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	private static Map<String, MultipartFile> filesByName(List<MultipartFile> files) {
		Map<String, MultipartFile> byName = new HashMap<>();
		for (MultipartFile file : files) {
			String name = file.getOriginalFilename();
			if (name == null || byName.putIfAbsent(name, file) != null) {
				throw new IllegalArgumentException("上傳檔名留空或重複");
			}
		}
		return byName;
	}

	// 方法：執行此方法定義的受控處理流程。
	private static String stagingKey(String batchId, String externalId) {
		return "asset-imports/staging/" + batchId + "/" + externalId + ".bin";
	}

	// 方法：執行此方法定義的受控處理流程。
	private static String finalKey(String batchId, String externalId) {
		return "asset-imports/batches/" + batchId + "/" + externalId + ".bin";
	}

	// 方法：執行此方法定義的受控處理流程。
	private static String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("執行環境不支援 SHA-256", exception);
		}
	}

	// 方法：執行此方法定義的受控處理流程。
	private static String requireActor(String value) {
		if (value == null || !value.matches("[A-Za-z0-9@._-]{1,80}")) {
			throw new IllegalArgumentException("管理者識別格式不合法");
		}
		return value;
	}

	private record ExportObject(String fileName, byte[] content) {
	}

	public record ExportBundle(String fileName, byte[] content) {
	}
}
