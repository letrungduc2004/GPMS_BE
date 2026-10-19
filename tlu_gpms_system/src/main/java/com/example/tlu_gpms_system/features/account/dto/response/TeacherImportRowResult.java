package com.example.tlu_gpms_system.features.account.dto.response;

import com.example.tlu_gpms_system.common.enums.RowImportStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherImportRowResult {
    int rowNumber; // Số dòng thực tế trong file Excel (dòng tiêu đề là dòng 1)
    String firstName;
    String lastName;
    String email;
    String phone;
    String organization;
    String majorName;
    Long amount; // SUCCESS: giá trị vừa gán; WARNING: giá trị hiện có trong DB; ERROR: null
    RowImportStatus status; // SUCCESS | WARNING | ERROR
    String message; // null nếu SUCCESS
}
