package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherResponse {
    UUID publicId;
    String name;
    String majorName;
    String organization;
    String phone;
    Long amount;
    Boolean status;

    public TeacherResponse(String name, String majorName, String organization, String phone, Long amount) {
        this.name = name;
        this.majorName = majorName;
        this.organization = organization;
        this.phone = phone;
        this.amount = amount;
    }
}
