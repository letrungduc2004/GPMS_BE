package com.example.tlu_gpms_system.features.semester.dto.request;

import com.example.tlu_gpms_system.common.enums.SemesterStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SemesterRequest {

    @NotBlank(message = "SEMESTER_NAME_EMPTY")
    String name;

    @NotNull(message = "SEMESTER_START_DATE_NULL")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate;

    @NotNull(message = "SEMESTER_END_DATE_NULL")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate;

    // Tùy chọn nếu cần cập nhật thủ công
    SemesterStatus status;
}
