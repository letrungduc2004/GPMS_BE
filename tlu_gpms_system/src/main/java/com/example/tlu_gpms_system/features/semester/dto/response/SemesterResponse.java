package com.example.tlu_gpms_system.features.semester.dto.response;

import com.example.tlu_gpms_system.common.enums.SemesterStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SemesterResponse {

    UUID id;
    UUID publicId;
    String name;

    @JsonFormat(pattern = "dd-MM-yyyy")
    LocalDate startDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    LocalDate endDate;

    String status;

    SemesterStatus statusCode;
}
