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
    String organization; // Đơn vị công tác: tên trường; email @tlu.edu.vn bắt buộc "Trường Đại học Thủy lợi"
    String major; // Mã ngành hoặc tên ngành; giáo viên ngoài trường (@gmail.com) phải để trống
    // Số lượng sinh viên hướng dẫn tối đa: số nguyên >= 0, không giới hạn trên. null / "" -> gán mặc định theo email.
    // Nhận dạng chuỗi thô để TeacherRowValidator báo AMOUNT_INVALID cho 5.5, "abc"...
    // (kiểu Long thì Jackson tự làm tròn 5.5 -> 5 và lỗi parse "abc" thành 400 / 1006 chung chung)
    String amount;
}
