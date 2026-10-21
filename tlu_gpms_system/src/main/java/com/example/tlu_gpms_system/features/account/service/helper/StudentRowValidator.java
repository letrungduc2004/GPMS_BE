package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.faculty.entity.Major;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

// Bước 1: kiểm tra Trống -> Định dạng/Danh mục cho từng cột (fail-fast trên từng cột).
// Kiểm tra trùng trong file nằm ở service vì cần nhìn toàn bộ các dòng.
public final class StudentRowValidator {

    // Giới hạn của cột VARCHAR(255) trong DB, tránh lỗi SQL làm hỏng cả lô
    private static final int MAX_LENGTH = 255;

    private static final Pattern STUDENT_CODE = Pattern.compile("^\\d{10}$");
    // Chữ cái tiếng Việt (Latin, sau NFC) và khoảng trắng đơn
    private static final Pattern FIRST_NAME = Pattern.compile("^\\p{IsLatin}+( \\p{IsLatin}+)*$");
    private static final Pattern LAST_NAME = Pattern.compile("^\\p{IsLatin}+$");
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");
    // Đuôi email bắt buộc của sinh viên (giáo viên không áp dụng)
    private static final String EMAIL_DOMAIN = "@gmail.com";
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern BATCH = Pattern.compile("^K\\d{2}$");
    private static final Pattern CLASS_NAME = Pattern.compile("^[\\p{L}\\p{N}]+( [\\p{L}\\p{N}]+)*$");

    private StudentRowValidator() {
    }

    // Trả về lỗi theo từng cột, mỗi cột tối đa 1 lỗi, giữ đúng thứ tự cột
    public static Map<StudentColumn, ErrorCode> validate(StudentRowData row, Map<String, Major> majorMap) {
        Map<StudentColumn, ErrorCode> errors = new EnumMap<>(StudentColumn.class);

        check(errors, StudentColumn.STUDENT_CODE, row.getStudentCode(), STUDENT_CODE,
                ErrorCode.STUDENT_CODE_EMPTY, ErrorCode.STUDENT_CODE_INVALID);
        check(errors, StudentColumn.FIRST_NAME, row.getFirstName(), FIRST_NAME,
                ErrorCode.FIRST_NAME_EMPTY, ErrorCode.FIRST_NAME_INVALID);
        check(errors, StudentColumn.LAST_NAME, row.getLastName(), LAST_NAME,
                ErrorCode.LAST_NAME_EMPTY, ErrorCode.LAST_NAME_INVALID);
        check(errors, StudentColumn.EMAIL, row.getEmail(), EMAIL,
                ErrorCode.EMAIL_EMPTY, ErrorCode.EMAIL_INVALID);
        // Đúng định dạng rồi mới xét đuôi; email đã được chuẩn hóa về chữ thường
        if (!errors.containsKey(StudentColumn.EMAIL) && !row.getEmail().endsWith(EMAIL_DOMAIN)) {
            errors.put(StudentColumn.EMAIL, ErrorCode.STUDENT_EMAIL_DOMAIN_INVALID);
        }
        check(errors, StudentColumn.PHONE, row.getPhone(), PHONE,
                ErrorCode.PHONE_EMPTY, ErrorCode.PHONE_INVALID);
        check(errors, StudentColumn.BATCH, row.getBatch(), BATCH,
                ErrorCode.BATCH_EMPTY, ErrorCode.BATCH_INVALID);
        check(errors, StudentColumn.CLASS_NAME, row.getClassName(), CLASS_NAME,
                ErrorCode.CLASS_NAME_EMPTY, ErrorCode.CLASS_NAME_INVALID);

        if (row.getMajor().isEmpty()) {
            errors.put(StudentColumn.MAJOR, ErrorCode.MAJOR_EMPTY);
        } else if (!majorMap.containsKey(TextNormalizer.toLowerKey(row.getMajor()))) {
            errors.put(StudentColumn.MAJOR, ErrorCode.MAJOR_INVALID);
        }
        return errors;
    }

    // Ô bị lỗi công thức (#N/A, #REF!, ...) chứa ký tự '#' nên luôn rơi vào lỗi định dạng
    private static void check(Map<StudentColumn, ErrorCode> errors, StudentColumn column, String value,
                              Pattern pattern, ErrorCode emptyError, ErrorCode invalidError) {
        if (value.isEmpty()) {
            errors.put(column, emptyError);
        } else if (value.length() > MAX_LENGTH || !pattern.matcher(value).matches()) {
            errors.put(column, invalidError);
        }
    }
}
