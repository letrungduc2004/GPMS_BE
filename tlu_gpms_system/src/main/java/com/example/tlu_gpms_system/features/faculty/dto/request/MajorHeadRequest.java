package com.example.tlu_gpms_system.features.faculty.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MajorHeadRequest {

    // id tài khoản giáo viên trong trường thuộc ngành được gán
    @NotNull(message = "MAJOR_HEAD_ACCOUNT_NULL")
    Long accountId;
}
