package com.example.tlu_gpms_system.features.account.service.helper;

import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.faculty.entity.Major;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

// Bước 1: kiểm tra Trống -> Định dạng/Danh mục cho từng cột (fail-fast trên từng cột).
// Kiểm tra trùng trong file nằm ở service vì cần nhìn toàn bộ các dòng.
public final class TeacherRowValidator {

    // Giới hạn của cột VARCHAR(255) trong DB, tránh lỗi SQL làm hỏng cả lô
    private static final int MAX_LENGTH = 255;

    // Chữ cái tiếng Việt (Latin, sau NFC) và khoảng trắng đơn
    private static final Pattern FIRST_NAME = Pattern.compile("^\\p{IsLatin}+( \\p{IsLatin}+)*$");
    private static final Pattern LAST_NAME = Pattern.compile("^\\p{IsLatin}+$");
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");
    // Tên trường: chữ cái tiếng Việt, chữ số, khoảng trắng đơn và các dấu câu . , - & ( ) ' /
    private static final Pattern ORGANIZATION = Pattern.compile("^[\\p{IsLatin}\\d.,&()'/-]+( [\\p{IsLatin}\\d.,&()'/-]+)*$");
    // Số nguyên >= 0, không làm tròn (5.5, 5.0, -1, "abc" đều lỗi). 0 hợp lệ (không nhận hướng dẫn, chỉ phản biện / hội đồng).
    // Không đặt giới hạn trên theo nghiệp vụ; tối đa 18 chữ số chỉ để luôn nằm trong phạm vi Long
    private static final Pattern AMOUNT = Pattern.compile("^\\d{1,18}$");

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
        // Đúng định dạng rồi mới xét đuôi; email đã được chuẩn hóa về chữ thường
        if (!errors.containsKey(TeacherColumn.EMAIL) && TeacherOrganization.ofEmail(row.getEmail()) == null) {
            errors.put(TeacherColumn.EMAIL, ErrorCode.TEACHER_EMAIL_DOMAIN_INVALID);
        }
        check(errors, TeacherColumn.PHONE, row.getPhone(), PHONE,
                ErrorCode.PHONE_EMPTY, ErrorCode.PHONE_INVALID);

        // Loại giáo viên chỉ xác định được khi ô email đã hợp lệ, ngược lại null
        TeacherOrganization type = errors.containsKey(TeacherColumn.EMAIL) ? null : TeacherOrganization.ofEmail(row.getEmail());

        check(errors, TeacherColumn.ORGANIZATION, row.getOrganization(), ORGANIZATION,
                ErrorCode.ORGANIZATION_EMPTY, ErrorCode.ORGANIZATION_INVALID);
        // Email trong trường bắt buộc là tên TLU; email ngoài trường không được ghi tên TLU
        if (type != null && !errors.containsKey(TeacherColumn.ORGANIZATION)
                && (type == TeacherOrganization.INTERNAL) != TeacherOrganization.isTluName(row.getOrganization())) {
            errors.put(TeacherColumn.ORGANIZATION, ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL);
        }

        // Giáo viên ngoài trường không có ngành; trong trường bắt buộc có.
        // Email lỗi (chưa biết loại) thì bỏ qua ô trống, ô có giá trị vẫn kiểm tra danh mục
        if (type == TeacherOrganization.EXTERNAL) {
            if (!row.getMajor().isEmpty()) {
                errors.put(TeacherColumn.MAJOR, ErrorCode.MAJOR_NOT_ALLOWED);
            }
        } else if (row.getMajor().isEmpty()) {
            if (type == TeacherOrganization.INTERNAL) {
                errors.put(TeacherColumn.MAJOR, ErrorCode.MAJOR_EMPTY);
            }
        } else if (!majorMap.containsKey(TextNormalizer.toLowerKey(row.getMajor()))) {
            errors.put(TeacherColumn.MAJOR, ErrorCode.MAJOR_INVALID);
        }

        // amount được bỏ trống (gán mặc định theo email)
        if (!row.getAmount().isEmpty() && !isValidAmount(row.getAmount())) {
            errors.put(TeacherColumn.AMOUNT, ErrorCode.AMOUNT_INVALID);
        }
        return errors;
    }

    // Dùng chung cho nhập thủ công và API sửa số lượng hướng dẫn. value đã được chuẩn hóa (trim)
    public static boolean isValidAmount(String value) {
        return AMOUNT.matcher(value).matches();
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
