package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;

import java.util.Set;

// Các cột trong file mẫu, đúng thứ tự cột Excel. AMOUNT chỉ có khi nhập thủ công (không có trong file)
public enum TeacherColumn {
    FIRST_NAME(Set.of("họ và tên đệm", "họ")),
    LAST_NAME(Set.of("tên")),
    EMAIL(Set.of("email")),
    PHONE(Set.of("số điện thoại")),
    ORGANIZATION(Set.of("đơn vị công tác")),
    MAJOR(Set.of("ngành")),
    AMOUNT(Set.of());

    // Số cột của file Excel (không tính AMOUNT)
    public static final int EXCEL_COLUMN_COUNT = 6;

    // Tiêu đề hợp lệ, đã viết thường
    private final Set<String> headers;

    TeacherColumn(Set<String> headers) {
        this.headers = headers;
    }

    public boolean matchesHeader(String header) {
        return headers.contains(TextNormalizer.toLowerKey(header));
    }
}
