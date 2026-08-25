package dev.miudog.linebotdocument.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 集中保存可由客戶設定的圖片歸檔代碼格式與回覆範例。
 */
@Component
public class AssetArchivePolicy {

	//#region 欄位

	private static final int MAXIMUM_FORMAT_COUNT = 10;
	private static final int MAXIMUM_CODE_LENGTH = 64;

	private final List<Pattern> codePatterns;
	private final Set<String> recognizedPrefixes;
	private final String example;

	//#endregion

	//#region 建構子

	// 方法：將客戶易讀的格式遮罩轉為安全 regex，並由第一組格式產生顯示範例。
	public AssetArchivePolicy(
		@Value("${app.archive.code-formats:ZD#####,ZD#####@,ZD-JY#####,YJ######}") String codeFormats
	) {
		List<String> masks = parseMasks(codeFormats);
		List<Pattern> patterns = new ArrayList<>();
		Set<String> prefixes = new LinkedHashSet<>();

		// 單一演算法：逐一編譯遮罩並擷取用於語法提示的固定前綴。
		for (String mask : masks) {
			patterns.add(compileMask(mask));
			prefixes.add(fixedPrefix(mask));
		}

		this.codePatterns = List.copyOf(patterns);
		this.recognizedPrefixes = Set.copyOf(prefixes);
		this.example = createExample(masks.getFirst());
	}

	//#endregion

	//#region 方法

	// 方法：在固定輸入長度上限內判斷完整歸檔代碼。
	public boolean matches(String value) {
		return isCandidate(value)
			&& codePatterns.stream().anyMatch(pattern -> pattern.matcher(value).matches());
	}

	// 方法：在固定輸入長度上限內判斷是否為值得提示的已知前綴。
	public boolean hasRecognizedPrefix(String value) {
		return isCandidate(value)
			&& recognizedPrefixes.stream().anyMatch(value::startsWith);
	}

	// 方法：取得錯誤提示與說明共用的合法代碼範例。
	public String example() {
		return example;
	}

	// 方法：限制使用者輸入長度，避免任何比對規則放大運算成本。
	private boolean isCandidate(String value) {
		return value != null && !value.isBlank() && value.length() <= MAXIMUM_CODE_LENGTH;
	}

	// 方法：解析逗號分隔的格式遮罩，拒絕空白、過量或難以理解的格式。
	private static List<String> parseMasks(String codeFormats) {
		if (codeFormats == null || codeFormats.isBlank()) {
			throw new IllegalArgumentException("歸檔代碼格式不可為空白");
		}

		String[] sourceMasks = codeFormats.split(",", -1);
		if (sourceMasks.length > MAXIMUM_FORMAT_COUNT) {
			throw new IllegalArgumentException("歸檔代碼格式不可超過 10 組");
		}

		List<String> masks = new ArrayList<>();
		for (String sourceMask : sourceMasks) {
			String mask = sourceMask.trim();
			if (!isValidMask(mask)) {
				throw new IllegalArgumentException("歸檔格式只能使用大寫英數字、-、# 與 @");
			}

			masks.add(mask);
		}

		return List.copyOf(masks);
	}

	// 方法：驗證遮罩長度、允許字元與至少一個變動位置。
	private static boolean isValidMask(String mask) {
		if (mask.isBlank() || mask.length() > MAXIMUM_CODE_LENGTH) return false;

		if (mask.charAt(0) == '#' || mask.charAt(0) == '@') return false;

		boolean hasPlaceholder = false;
		for (int index = 0; index < mask.length(); index++) {
			char character = mask.charAt(index);
			if (character == '#' || character == '@') {
				hasPlaceholder = true;
				continue;
			}

			if (character == '-' || Character.isDigit(character)) continue;

			if (character < 'A' || character > 'Z') return false;
		}

		return hasPlaceholder;
	}

	// 方法：依遮罩產生一致且一定合法的顯示範例，避免客戶重複維護設定。
	private static String createExample(String mask) {
		StringBuilder example = new StringBuilder();
		int nextDigit = 1;
		for (int index = 0; index < mask.length(); index++) {
			char character = mask.charAt(index);
			if (character == '#') {
				example.append(nextDigit % 10);
				nextDigit++;
			}
			else if (character == '@') {
				example.append('A');
			}
			else {
				example.append(character);
			}
		}

		return example.toString();
	}

	// 方法：將 # 數字與 @ 大寫字母遮罩轉成完整匹配的內部 regex。
	private static Pattern compileMask(String mask) {
		StringBuilder expression = new StringBuilder("^");
		for (int index = 0; index < mask.length(); index++) {
			char character = mask.charAt(index);
			if (character == '#') {
				expression.append("[0-9]");
			}
			else if (character == '@') {
				expression.append("[A-Z]");
			}
			else {
				expression.append(Pattern.quote(Character.toString(character)));
			}
		}
		expression.append('$');

		// 外部函式：只編譯程式由安全遮罩產生的 regex，不直接執行客戶 regex。
		return Pattern.compile(expression.toString());
	}

	// 方法：取得遮罩第一個變動位置前的固定文字，供錯誤提示判斷。
	private static String fixedPrefix(String mask) {
		int digitIndex = mask.indexOf('#');
		int letterIndex = mask.indexOf('@');
		int firstPlaceholder = mask.length();
		if (digitIndex >= 0) firstPlaceholder = digitIndex;

		if (letterIndex >= 0) firstPlaceholder = Math.min(firstPlaceholder, letterIndex);

		return mask.substring(0, firstPlaceholder);
	}

	//#endregion
}
