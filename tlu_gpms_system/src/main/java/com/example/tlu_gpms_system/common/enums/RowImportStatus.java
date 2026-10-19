package com.example.tlu_gpms_system.common.enums;

public enum RowImportStatus {
    SUCCESS, // Hợp lệ -> Đã lưu DB (Tạo mới hoặc Kích hoạt lại)
    WARNING, // Báo Vàng -> Đã active sẵn trong kì, bỏ qua không lưu lại
    ERROR    // Báo Đỏ -> Lỗi dữ liệu hoặc xung đột DB, không lưu
}
