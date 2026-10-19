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
public class MajorResponse {
    UUID id;
    UUID publicId;
    String name;
    String description;
    String status;
    ActiveStatus statusCode;
}
