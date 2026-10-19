package com.example.tlu_gpms_system.common.enums;

// Bộ lọc trạng thái trên danh sách tài khoản (dropdown "Trạng thái" ở FE)
public enum AccountStatusFilter {
    ACTIVE,   // Đang hoạt động (mặc định)
    INACTIVE, // Đã tắt, FE hiện nút Kích hoạt
    ALL;      // Tất cả

    // Giá trị truyền vào query: null nghĩa là không lọc theo trạng thái
    public Boolean toActive() {
        return switch (this) {
            case ACTIVE -> true;
            case INACTIVE -> false;
            case ALL -> null;
        };
    }
}
