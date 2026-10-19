package com.example.tlu_gpms_system.features.faculty.dto.request;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MajorCreationRequest {

    @NotBlank(message = "MAJOR_NAME_EMPTY")
    String name;

    String description;

    ActiveStatus status;
}
