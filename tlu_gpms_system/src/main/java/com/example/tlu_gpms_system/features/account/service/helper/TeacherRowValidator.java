package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.entity.Major;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

// Bước 1: kiểm tra Trống -> Định dạng/Danh mục cho từng cột (fail-fast trên từng cột).
// Kiểm tra trùng trong file nằm ở service vì cần nhìn toàn bộ các dòng.
public final class TeacherRowValidator {

    // Giới hạn của cột VARCHAR(255) trong DB, tránh lỗi SQL làm hỏng cả lô
    private static final int MAX_LENGTH = 255;
    private static final long MIN_AMOUNT = 1L;
    private static final long MAX_AMOUNT = 20L;

    // Chữ cái tiếng Việt (Latin, sau NFC) và khoảng trắng đơn
    private static final Pattern FIRST_NAME = Pattern.compile("^\\p{IsLatin}+( \\p{IsLatin}+)*$");
    private static final Pattern LAST_NAME = Pattern.compile("^\\p{IsLatin}+$");
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");

    private TeacherRowValidator() {
    }

    // Trả về lỗi theo từng cột, mỗi cột tối đa 1 lỗi, giữ đúng thứ tự cột
    public static Map<TeacherColumn, ErrorCode> validate(TeacherRowData row, Map<String, Major> majorMap) {
        Map<TeacherColumn, ErrorCode> errors = new EnumMap<>(TeacherColumn.class);

        check(errors, TeacherColumn.FIRST_NAME, row.getFirstName(), FIRST_NAME,
                ErrorCode.FIRST_NAME_EMPTY, ErrorCode.FIRST_NAME_INVALID);
        check(errors, TeacherColumn.LAST_NAME, row.getLastName(), LAST_NAME,
                ErrorCode.LAST_NAME_EMPTY, ErrorCode.LAST_NAME_INVALID);
        check(errors, TeacherColumn.EMAIL, row.getEmail(), EMAIL,
                ErrorCode.EMAIL_EMPTY, ErrorCode.EMAIL_INVALID);
        check(errors, TeacherColumn.PHONE, row.getPhone(), PHONE,
                ErrorCode.PHONE_EMPTY, ErrorCode.PHONE_INVALID);

        TeacherOrganization organization = TeacherOrganization.fromLabel(row.getOrganization());
        if (row.getOrganization().isEmpty()) {
            errors.put(TeacherColumn.ORGANIZATION, ErrorCode.ORGANIZATION_EMPTY);
        } else if (organization == null) {
            errors.put(TeacherColumn.ORGANIZATION, ErrorCode.ORGANIZATION_INVALID);
        } else if (!errors.containsKey(TeacherColumn.EMAIL) && organization != TeacherOrganization.ofEmail(row.getEmail())) {
            // Chỉ đối chiếu với email khi ô email đã hợp lệ
            errors.put(TeacherColumn.ORGANIZATION, ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL);
        }

        if (row.getMajor().isEmpty()) {
            errors.put(TeacherColumn.MAJOR, ErrorCode.MAJOR_EMPTY);
        } else if (!majorMap.containsKey(TextNormalizer.toLowerKey(row.getMajor()))) {
            errors.put(TeacherColumn.MAJOR, ErrorCode.MAJOR_INVALID);
        }

        // amount được bỏ trống (gán mặc định theo email)
        Long amount = row.getAmount();
        if (amount != null && (amount < MIN_AMOUNT || amount > MAX_AMOUNT)) {
            errors.put(TeacherColumn.AMOUNT, ErrorCode.AMOUNT_INVALID);
        }
        return errors;
    }

    // Ô bị lỗi công thức (#N/A, #REF!, ...) chứa ký tự '#' nên luôn rơi vào lỗi định dạng
    private static void check(Map<TeacherColumn, ErrorCode> errors, TeacherColumn column, String value,
                              Pattern pattern, ErrorCode emptyError, ErrorCode invalidError) {
        if (value.isEmpty()) {
            errors.put(column, emptyError);
        } else if (value.length() > MAX_LENGTH || !pattern.matcher(value).matches()) {
            errors.put(column, invalidError);
        }
    }
}
