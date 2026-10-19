package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

// Popup chi tiết 1 sinh viên (GET /student/{publicId})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentDetailResponse {
    UUID publicId;
    String studentCode;
    String name; // Họ + Tên
    String email;
    String phone;
    String majorName;
    String className;
    String batch;
    Boolean status; // true: đang hoạt động (cả Account và Student đều bật)
}
