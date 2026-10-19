package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherImportResponse {
    int totalRows; // Tổng số dòng có dữ liệu đã xử lý
    int successCount;
    int warningCount;
    int errorCount;
    List<TeacherImportRowResult> rowResults;
}
