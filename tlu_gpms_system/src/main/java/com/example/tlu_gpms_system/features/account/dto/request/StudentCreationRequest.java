package com.example.tlu_gpms_system.features.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentCreationRequest {

    @NotBlank(message = "EMAIL_EMPTY")
    @Email(message = "EMAIL_INVALID")
    String email;

    @NotBlank(message = "FIRST_NAME_EMPTY")
    String firstName; // Họ

    @NotBlank(message = "LAST_NAME_EMPTY")
    String lastName; // Tên

    String phone;

    @NotBlank(message = "STUDENT_CODE_EMPTY")
    String studentCode;

    @NotBlank(message = "BATCH_EMPTY")
    String batch; // Khóa

    @NotBlank(message = "CLASS_NAME_EMPTY")
    String className; // Tên lớp

    @NotNull(message = "MAJOR_ID_NULL")
    Long majorId;
}
