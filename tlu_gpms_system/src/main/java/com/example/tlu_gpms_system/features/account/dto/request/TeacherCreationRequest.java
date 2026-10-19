package com.example.tlu_gpms_system.features.account.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Không dùng @NotBlank/@Email ở đây: dữ liệu được kiểm tra bởi TeacherRowValidator
// để gom lỗi giống 1 dòng Excel (1 lỗi -> lỗi cụ thể, >= 2 lỗi -> thông báo chung)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherCreationRequest {
    String firstName; // Họ và tên đệm
    String lastName; // Tên
    String email;
    String phone;
    String organization; // Đơn vị công tác: Trong trường | Ngoài trường
    String major; // Mã ngành hoặc tên ngành
    Long amount; // Số lượng sinh viên hướng dẫn tối đa (1-20). null -> gán mặc định theo email
}
