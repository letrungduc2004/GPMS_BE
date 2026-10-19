package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

// 1 dòng dữ liệu sinh viên ĐÃ chuẩn hóa (từ Excel hoặc từ form nhập thủ công)
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentRowData {
    int rowNumber;
    String studentCode;
    String firstName;
    String lastName;
    String email;
    String phone;
    String batch;
    String className;
    String major;

    // cells theo đúng thứ tự cột của file mẫu (xem StudentColumn)
    public static StudentRowData fromCells(int rowNumber, List<String> cells) {
        return StudentRowData.builder()
                .rowNumber(rowNumber)
                .studentCode(TextNormalizer.normalize(cells.get(0)))
                .firstName(TextNormalizer.normalize(cells.get(1)))
                .lastName(TextNormalizer.normalize(cells.get(2)))
                .email(TextNormalizer.toLowerKey(cells.get(3)))
                .phone(TextNormalizer.normalize(cells.get(4)))
                .batch(TextNormalizer.toUpperKey(cells.get(5)))
                .className(TextNormalizer.normalize(cells.get(6)))
                .major(TextNormalizer.normalize(cells.get(7)))
                .build();
    }

    public static StudentRowData fromRequest(StudentCreationRequest request) {
        return fromCells(0, List.of(
                nullToEmpty(request.getStudentCode()),
                nullToEmpty(request.getFirstName()),
                nullToEmpty(request.getLastName()),
                nullToEmpty(request.getEmail()),
                nullToEmpty(request.getPhone()),
                nullToEmpty(request.getBatch()),
                nullToEmpty(request.getClassName()),
                nullToEmpty(request.getMajor())));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
