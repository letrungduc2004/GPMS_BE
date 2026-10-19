package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;

import java.util.Set;

// Các cột trong file mẫu, đúng thứ tự cột Excel
public enum StudentColumn {
    STUDENT_CODE(Set.of("mã sinh viên")),
    FIRST_NAME(Set.of("họ")),
    LAST_NAME(Set.of("tên")),
    EMAIL(Set.of("email")),
    PHONE(Set.of("số điện thoại")),
    BATCH(Set.of("khóa", "khoá")), // Chấp nhận cả kiểu bỏ dấu cũ "khoá"
    CLASS_NAME(Set.of("lớp")),
    MAJOR(Set.of("ngành"));

    // Tiêu đề hợp lệ, đã viết thường
    private final Set<String> headers;

    StudentColumn(Set<String> headers) {
        this.headers = headers;
    }

    public boolean matchesHeader(String header) {
        return headers.contains(TextNormalizer.toLowerKey(header));
    }
}
