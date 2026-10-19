package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MajorResponse {
    UUID publicId; // FE gửi lại làm tham số majorId khi lọc danh sách sinh viên
    String code;
    String name;
}
