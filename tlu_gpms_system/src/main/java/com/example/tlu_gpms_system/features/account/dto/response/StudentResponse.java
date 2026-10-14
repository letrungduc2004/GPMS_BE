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
public class StudentResponse {
    UUID publicId;
    String studentCode;
    String name;
    String majorName;
    String className;
    String batch;

    public StudentResponse(String name, String majorName, String className, String batch) {
        this.name = name;
        this.majorName = majorName;
        this.className = className;
        this.batch = batch;
    }
}
