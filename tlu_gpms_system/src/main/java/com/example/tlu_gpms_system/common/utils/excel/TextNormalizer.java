package com.example.tlu_gpms_system.common.utils.excel;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

// Chuẩn hóa chuỗi người dùng nhập (form, file Excel, ...) trước khi validate / so sánh / lưu DB
public final class TextNormalizer {

    // Zero-width space, zero-width (non-)joiner, word joiner, BOM
    private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF]");
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    private static final char NO_BREAK_SPACE = ' ';

    private TextNormalizer() {
    }

    // Xóa ký tự ẩn, gộp khoảng trắng (cả tab, xuống dòng, NBSP), trim, chuẩn Unicode dựng sẵn (NFC). null -> ""
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String result = INVISIBLE_CHARS.matcher(value).replaceAll("").replace(NO_BREAK_SPACE, ' ');
        result = WHITESPACES.matcher(result).replaceAll(" ").trim();
        return Normalizer.normalize(result, Normalizer.Form.NFC);
    }

    // Key so sánh không phân biệt hoa/thường (email, tiêu đề cột, mã/tên danh mục)
    public static String toLowerKey(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    // VD: k64 -> K64
    public static String toUpperKey(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }
}
