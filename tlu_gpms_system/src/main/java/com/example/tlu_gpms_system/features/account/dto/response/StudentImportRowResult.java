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
public class StudentImportRowResult {
    int rowNumber; // Số dòng thực tế trong file Excel (dòng tiêu đề là dòng 1)
    String studentCode;
    String firstName;
    String lastName;
    String email;
    String phone;
    String batch;
    String className;
    String majorName;
    RowImportStatus status; // SUCCESS | WARNING | ERROR
    String message; // SUCCESS: thêm mới / kích hoạt lại / đã cập nhật field nào; WARNING, ERROR: lý do
}
