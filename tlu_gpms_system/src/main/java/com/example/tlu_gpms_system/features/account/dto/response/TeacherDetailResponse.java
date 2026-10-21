package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

// Popup chi tiết 1 giáo viên (GET /teacher/{publicId})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherDetailResponse {
    UUID publicId;
    String name; // Họ + Tên
    String email;
    String phone;
    String organization; // Đơn vị công tác: tên trường
    String majorName; // Khoa phụ trách
    Long amount; // Hạn mức: số sinh viên hướng dẫn tối đa
    Boolean status; // true: tài khoản đang hoạt động
}
