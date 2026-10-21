package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Kết quả nhập thủ công 1 giáo viên: service trả về để controller gói vào APIResponse<Void> (code + message), không trả ra data
@Getter
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherCreationResult {
    int code; // 1000: xanh, 11xx: vàng
    String message;
}
