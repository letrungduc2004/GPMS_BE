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
public class MajorUpdateRequest {

    @NotBlank(message = "MAJOR_NAME_EMPTY")
    String name;

    @NotNull(message = "MAJOR_STATUS_NULL")
    ActiveStatus status;

    String description;
}
