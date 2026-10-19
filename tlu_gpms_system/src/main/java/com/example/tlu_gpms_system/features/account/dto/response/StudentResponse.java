package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

// 1 dòng trên bảng danh sách sinh viên: chỉ giữ cột để Trưởng bộ môn dò nhanh.
// Thông tin đầy đủ (email, SĐT, khóa...) lấy qua GET /student/{publicId} -> StudentDetailResponse
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentResponse {
    UUID publicId; // Không hiển thị, FE dùng để mở popup chi tiết và bấm Kích hoạt
    String studentCode;
    String name; // Họ tên đầy đủ: Họ + Tên, ví dụ "Lê Trung Đức"
    String majorName; // Trưởng bộ môn quản lý nhiều ngành nên giữ cột này
    String className;
    Boolean status; // true: đang hoạt động (cả Account và Student đều bật); false: đã tắt, FE hiện nút Kích hoạt
}
