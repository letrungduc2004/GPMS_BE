package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.List;

// 1 dòng dữ liệu giáo viên ĐÃ chuẩn hóa (từ Excel hoặc từ form nhập thủ công)
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeacherRowData {
    int rowNumber;
    String firstName;
    String lastName;
    String email;
    String phone;
    String organization;
    String major;
    Long amount; // Chỉ có khi nhập thủ công. null -> gán mặc định theo email

    // cells theo đúng thứ tự cột của file mẫu (xem TeacherColumn)
    public static TeacherRowData fromCells(int rowNumber, List<String> cells) {
        return fromCells(rowNumber, cells, null);
    }

    public static TeacherRowData fromRequest(TeacherCreationRequest request) {
        return fromCells(0, List.of(
                nullToEmpty(request.getFirstName()),
                nullToEmpty(request.getLastName()),
                nullToEmpty(request.getEmail()),
                nullToEmpty(request.getPhone()),
                nullToEmpty(request.getOrganization()),
                nullToEmpty(request.getMajor())), request.getAmount());
    }

    private static TeacherRowData fromCells(int rowNumber, List<String> cells, Long amount) {
        return TeacherRowData.builder()
                .rowNumber(rowNumber)
                .firstName(TextNormalizer.normalize(cells.get(0)))
                .lastName(TextNormalizer.normalize(cells.get(1)))
                .email(TextNormalizer.toLowerKey(cells.get(2)))
                .phone(TextNormalizer.normalize(cells.get(3)))
                .organization(TeacherOrganization.canonicalize(TextNormalizer.normalize(cells.get(4))))
                .major(TextNormalizer.normalize(cells.get(5)))
                .amount(amount)
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
