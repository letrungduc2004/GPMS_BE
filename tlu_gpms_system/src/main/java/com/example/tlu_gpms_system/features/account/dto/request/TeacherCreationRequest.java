package com.example.tlu_gpms_system.features.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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
public class TeacherCreationRequest {

    @NotBlank(message = "EMAIL_EMPTY")
    @Email(message = "EMAIL_INVALID")
    String email;

    @NotBlank(message = "FIRST_NAME_EMPTY")
    String firstName; // Họ

    @NotBlank(message = "LAST_NAME_EMPTY")
    String lastName; // Tên

    @NotBlank(message = "PHONE_EMPTY")
    String phone;

    @NotBlank(message = "ORGANIZATION_EMPTY")
    String organization; // Đơn vị công tác

    @NotNull(message = "AMOUNT_NULL")
    @Min(value = 1, message = "AMOUNT_INVALID")
    Long amount; // Số lượng sinh viên tối đa hướng dẫn

    @NotNull(message = "MAJOR_ID_NULL")
    Long majorId; // Chuyên ngành phụ trách
}
