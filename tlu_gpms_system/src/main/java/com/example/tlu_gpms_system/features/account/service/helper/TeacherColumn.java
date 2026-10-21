package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;

import java.util.Set;

// Các cột trong file mẫu, đúng thứ tự cột Excel. AMOUNT chỉ có khi nhập thủ công (không có trong file)
public enum TeacherColumn {
    FIRST_NAME("Họ", Set.of("họ và tên đệm", "họ")),
    LAST_NAME("Tên", Set.of("tên")),
    EMAIL("Email", Set.of("email")),
    PHONE("Số điện thoại", Set.of("số điện thoại")),
    ORGANIZATION("Đơn vị công tác", Set.of("đơn vị công tác")),
    MAJOR("Ngành", Set.of("ngành")),
    AMOUNT("Số lượng hướng dẫn", Set.of());

    // Số cột của file Excel (không tính AMOUNT)
    public static final int EXCEL_COLUMN_COUNT = 6;

    // Tên trường hiển thị trong message lỗi
    private final String label;
    // Tiêu đề hợp lệ, đã viết thường
    private final Set<String> headers;

    TeacherColumn(String label, Set<String> headers) {
        this.label = label;
        this.headers = headers;
    }

    public String getLabel() {
        return label;
    }

    public boolean matchesHeader(String header) {
        return headers.contains(TextNormalizer.toLowerKey(header));
    }
}
