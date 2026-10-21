package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import lombok.Getter;

// Loại giáo viên (trong trường / ngoài trường), suy ra từ đuôi email.
// Cột đơn vị công tác lưu thẳng tên trường; giáo viên trong trường bắt buộc là TLU_NAME
@Getter
public enum TeacherOrganization {
    INTERNAL(20L),
    EXTERNAL(10L);

    // Tên chuẩn lưu vào DB cho giáo viên trong trường
    public static final String TLU_NAME = "Trường Đại học Thủy lợi";

    private static final String INTERNAL_EMAIL_DOMAIN = "@tlu.edu.vn";
    private static final String EXTERNAL_EMAIL_DOMAIN = "@gmail.com";

    // Số lượng sinh viên hướng dẫn mặc định
    private final Long defaultAmount;

    TeacherOrganization(Long defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    // email đã chuẩn hóa về chữ thường. Chỉ nhận "@tlu.edu.vn" (trong trường) và "@gmail.com" (ngoài trường),
    // đuôi khác (kể cả email sinh viên "@e.tlu.edu.vn") -> null
    public static TeacherOrganization ofEmail(String email) {
        if (email.endsWith(INTERNAL_EMAIL_DOMAIN)) {
            return INTERNAL;
        }
        return email.endsWith(EXTERNAL_EMAIL_DOMAIN) ? EXTERNAL : null;
    }

    // Không phân biệt hoa/thường
    public static boolean isTluName(String value) {
        return TextNormalizer.toLowerKey(TLU_NAME).equals(TextNormalizer.toLowerKey(value));
    }

    // Tên TLU viết khác hoa/thường -> đổi về chuỗi chuẩn; tên trường khác giữ nguyên
    public static String canonicalize(String value) {
        return isTluName(value) ? TLU_NAME : value;
    }

    // Bộ lọc danh sách: FE gửi organizationType = "internal" / "external" (không phân biệt hoa/thường).
    // Trống -> null (không lọc); giá trị khác -> INVALID_REQUEST
    public static TeacherOrganization fromFilter(String value) {
        String key = TextNormalizer.toUpperKey(value);
        if (key.isEmpty()) {
            return null;
        }
        for (TeacherOrganization type : values()) {
            if (type.name().equals(key)) {
                return type;
            }
        }
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }
}
