package dev.miudog.linebotdocument.service;

import dev.miudog.linebotdocument.domain.Asset;
import java.nio.file.Path;
import org.springframework.stereotype.Service;

/**
 * 將資產的相對路徑解析為一般資產根目錄下的實際位置。
 *
 * <p>報價單目錄屬於商用機器人；本產品只有一般資產根目錄一種儲存範圍，
 * 因此不需要再依資料庫關聯挑選根目錄。
 */
@Service
public class AssetPathResolver {

	private final FileStorageService storage;

	// 方法：建立只使用一般資產根目錄的資產路徑解析器。
	public AssetPathResolver(FileStorageService storage) {
		this.storage = storage;
	}

	// 方法：解析資產檔案的實際位置；不採信 file_path 的文字前綴，一律由儲存服務決定根目錄。
	public Path resolve(Asset asset) {
		if (asset == null || asset.id() == null) throw new IllegalArgumentException("資產不可留空");

		return storage.resolve(asset.filePath());
	}
}
