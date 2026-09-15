package dev.miudog.linebotdocument.companyasset;

import java.util.List;

/**
 * 可由公司保存、匯出並在另一套部署重新匯入的圖片資產 manifest。
 */
public record AssetImportManifest(
	String schemaVersion,
	String companyId,
	String batchId,
	List<AssetObject> objects
) {

	public record AssetObject(
		String assetExternalId,
		String assetCode,
		String folderCode,
		List<String> tags,
		String sourceType,
		String sourceId,
		String sourceEventId,
		String fileName,
		String contentType,
		long size,
		String sha256
	) {
	}
}
