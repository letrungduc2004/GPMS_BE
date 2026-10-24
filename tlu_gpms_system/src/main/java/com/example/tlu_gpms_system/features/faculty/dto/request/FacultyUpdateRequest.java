package com.example.tlu_gpms_system.features.faculty.dto.request;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FacultyUpdateRequest {

    @NotBlank(message = "FACULTY_CODE_EMPTY")
    String code;

    @NotBlank(message = "FACULTY_NAME_EMPTY")
    String name;

    String description;

    @NotNull(message = "FACULTY_STATUS_NULL")
    ActiveStatus status;
}
