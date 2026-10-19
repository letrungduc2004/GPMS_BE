package com.example.tlu_gpms_system.features.faculty.dto.response;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FacultyResponse {
    UUID id;
    UUID publicId;
    String code;
    String name;
    HeadOfDepartmentResponse headOfDepartment;
    Long totalMajors;
    String status;
    ActiveStatus statusCode;
}
