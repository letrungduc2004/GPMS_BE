package com.example.tlu_gpms_system.features.faculty.dto.response;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
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
    String description;
    String status;
    ActiveStatus statusCode;
    Long headAccountId; // null khi ngành chưa có trưởng
    String headFullName;

    // Dùng cho DTO Projection dropdown "Ngành" (MajorRepository.findAllMajors)
    public MajorResponse(UUID publicId, String code, String name) {
        this.publicId = publicId;
        this.code = code;
        this.name = name;
    }
}
