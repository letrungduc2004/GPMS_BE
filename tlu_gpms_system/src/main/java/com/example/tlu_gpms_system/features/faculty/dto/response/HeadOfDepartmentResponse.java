package com.example.tlu_gpms_system.features.faculty.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HeadOfDepartmentResponse {
    Long id;
    String fullName;
    String title; // TS., PGS., etc.
}
