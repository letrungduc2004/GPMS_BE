package com.example.tlu_gpms_system.features.faculty.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HeadCandidateResponse {
    Long id;
    String fullName;
    String email;
    String academicRank; // TS., PGS., etc.
}
