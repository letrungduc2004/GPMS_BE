package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import lombok.Getter;

// Đơn vị công tác của giáo viên: chỉ có 2 giá trị và được suy ra từ đuôi email
@Getter
public enum TeacherOrganization {
    INTERNAL("Trong trường", 20L),
    EXTERNAL("Ngoài trường", 10L);

    private static final String INTERNAL_EMAIL_DOMAIN = "@tlu.edu.vn";

    // Chuỗi chuẩn lưu vào DB
    private final String label;
    // Số lượng sinh viên hướng dẫn mặc định
    private final Long defaultAmount;

    TeacherOrganization(String label, Long defaultAmount) {
        this.label = label;
        this.defaultAmount = defaultAmount;
    }

    // email đã chuẩn hóa về chữ thường. "@e.tlu.edu.vn" không khớp "@tlu.edu.vn" nên là ngoài trường
    public static TeacherOrganization ofEmail(String email) {
        return email.endsWith(INTERNAL_EMAIL_DOMAIN) ? INTERNAL : EXTERNAL;
    }

    // Không phân biệt hoa/thường. Không khớp -> null
    public static TeacherOrganization fromLabel(String value) {
        String key = TextNormalizer.toLowerKey(value);
        for (TeacherOrganization organization : values()) {
            if (TextNormalizer.toLowerKey(organization.label).equals(key)) {
                return organization;
            }
        }
        return null;
    }

    // Đổi về chuỗi chuẩn nếu khớp, ngược lại giữ nguyên để validator báo lỗi
    public static String canonicalize(String value) {
        TeacherOrganization organization = fromLabel(value);
        return organization == null ? value : organization.label;
    }
}
