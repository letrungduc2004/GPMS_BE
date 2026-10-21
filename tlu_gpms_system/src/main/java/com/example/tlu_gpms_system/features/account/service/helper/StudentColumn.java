package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;

import java.util.Set;

// Các cột trong file mẫu, đúng thứ tự cột Excel
public enum StudentColumn {
    STUDENT_CODE("Mã sinh viên", Set.of("mã sinh viên")),
    FIRST_NAME("Họ", Set.of("họ")),
    LAST_NAME("Tên", Set.of("tên")),
    EMAIL("Email", Set.of("email")),
    PHONE("Số điện thoại", Set.of("số điện thoại")),
    BATCH("Khóa", Set.of("khóa", "khoá")), // Chấp nhận cả kiểu bỏ dấu cũ "khoá"
    CLASS_NAME("Lớp", Set.of("lớp")),
    MAJOR("Ngành", Set.of("ngành"));

    // Tên trường hiển thị trong message lỗi
    private final String label;
    // Tiêu đề hợp lệ, đã viết thường
    private final Set<String> headers;

    StudentColumn(String label, Set<String> headers) {
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
