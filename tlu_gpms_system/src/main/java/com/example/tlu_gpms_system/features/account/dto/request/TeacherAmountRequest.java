package com.example.tlu_gpms_system.features.account.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Sửa số lượng sinh viên hướng dẫn của 1 giáo viên đang hoạt động.
// Không dùng @NotNull/@Min: kiểm tra bằng TeacherRowValidator.isValidAmount, cùng quy tắc với nhập thủ công
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherAmountRequest {
    // Bắt buộc, số nguyên >= 0, không giới hạn trên. Nhận dạng chuỗi thô để không bị Jackson làm tròn 5.5 -> 5
    String amount;
}
