package com.example.tlu_gpms_system.configuration.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
// COMMON
    SYSTEM_ERROR(1001, "Hệ thống không phản hồi", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(1002, "Hệ thống không hỗ trợ", HttpStatus.SERVICE_UNAVAILABLE),
    VALIDATION_FAIL(1003, "Xác thực thất bại", HttpStatus.UNPROCESSABLE_ENTITY),
    RESOURCE_ALREADY_EXISTS(1004, "Thông tin đã tồn tại", HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND(1005, "Thông tin không có sẵn", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(1006, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    OPERATION_NOT_ALLOWED(1007, "Hệ thống không cấp phép", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_SORT_FIELD(1008, "Trường sắp xếp không hợp lệ", HttpStatus.BAD_REQUEST),

    // SECURITY
    UNAUTHORIZED(2000, "Phiên đăng nhập của bạn đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(2000, "Vui lòng xác minh danh tính", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(2001, "Bạn không có quyền truy cập (access denied)", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED(2002, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(2003, "Token không đúng định dạng. Đăng nhập lại", HttpStatus.UNAUTHORIZED),

    // ROLE_PERMISSION
    PERMISSION_EXIST(2004, "Permession đã tồn tại", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_EXIST(2005, "Permession không tồn tại", HttpStatus.NOT_FOUND),
    ROLE_EXIST(2006, "Role đã tồn tại", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXIST(2007, "Role không tồn tại", HttpStatus.NOT_FOUND),

// API THIRD
    // MAIL SERVICE
    MAIL_SERVICE_FAIL(4000, "Không gửi được mail", HttpStatus.BAD_REQUEST),

    // CLOUDINARY_SERVICE
    CLOUDINARY_SERVICE_FAIL(4000, "Lưu thất bại", HttpStatus.BAD_REQUEST),

// FIELD VALIDATION
    // ACCOUNT
    USER_NOT_FOUND(4000, "Tài khoản của bạn không tồn tại", HttpStatus.NOT_FOUND),
    ACCOUNT_INVALID(4000, "Tài khoản của bạn không được cấp phép hệ thống", HttpStatus.BAD_REQUEST),
    EMAIL_EMPTY(4000, "Email không được để trống", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(4000, "Email không đúng định dạng", HttpStatus.BAD_REQUEST),
    FIRST_NAME_EMPTY(4000, "Họ không được để trống", HttpStatus.BAD_REQUEST),
    CODE_EMPTY(4000, "Mã định danh google gửi không được để trống", HttpStatus.BAD_REQUEST),
    PHONE_EMPTY(4000, "Số điện thoại không được để trống", HttpStatus.BAD_REQUEST),
    LAST_NAME_EMPTY(4000, "Tên không được để trống", HttpStatus.BAD_REQUEST),
    STUDENT_CODE_EMPTY(4000, "Mã sinh viên không được để trống", HttpStatus.BAD_REQUEST),
    BATCH_EMPTY(4000, "Khóa học không được để trống", HttpStatus.BAD_REQUEST),
    CLASS_NAME_EMPTY(4000, "Tên lớp không được để trống", HttpStatus.BAD_REQUEST),
    MAJOR_ID_NULL(4000, "Ngành học không được để trống", HttpStatus.BAD_REQUEST),
    ORGANIZATION_EMPTY(4000, "Đơn vị công tác không được để trống", HttpStatus.BAD_REQUEST),
    AMOUNT_NULL(4000, "Số lượng sinh viên hướng dẫn không được để trống", HttpStatus.BAD_REQUEST),
    AMOUNT_INVALID(4000, "Số lượng sinh viên hướng dẫn phải từ 1 đến 20", HttpStatus.BAD_REQUEST),

    // BUSINESS LOGIC
    EMAIL_EXIST(3001, "Email đã tồn tại trong hệ thống", HttpStatus.CONFLICT),
    STUDENT_CODE_EXIST(3002, "Mã sinh viên đã tồn tại trong hệ thống", HttpStatus.CONFLICT),
    MAJOR_NOT_FOUND(3003, "Ngành học không tồn tại", HttpStatus.NOT_FOUND),

    // STUDENT IMPORT - FILE
    FILE_EMPTY(4000, "File import không được để trống", HttpStatus.BAD_REQUEST),
    FILE_INVALID_FORMAT(4000, "File import phải có định dạng .xlsx", HttpStatus.BAD_REQUEST),
    FILE_HEADER_INVALID(4000, "Dòng tiêu đề không đúng với file mẫu", HttpStatus.BAD_REQUEST),
    FILE_MERGED_CELLS(4000, "File import không được chứa ô bị gộp", HttpStatus.BAD_REQUEST),
    FILE_NO_DATA(4000, "File import không có dòng dữ liệu nào", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(4000, "File import vượt quá dung lượng cho phép (50MB)", HttpStatus.BAD_REQUEST),

    // STUDENT IMPORT - ROW (dùng chung cho import Excel và nhập thủ công)
    STUDENT_CODE_INVALID(4000, "Mã sinh viên phải gồm đúng 10 chữ số", HttpStatus.BAD_REQUEST),
    STUDENT_CODE_DUPLICATED_IN_FILE(4000, "Mã sinh viên bị trùng với dòng khác trong file", HttpStatus.BAD_REQUEST),
    FIRST_NAME_INVALID(4000, "Họ chỉ được chứa chữ cái và khoảng trắng", HttpStatus.BAD_REQUEST),
    LAST_NAME_INVALID(4000, "Tên chỉ được chứa chữ cái", HttpStatus.BAD_REQUEST),
    EMAIL_DUPLICATED_IN_FILE(4000, "Email bị trùng với dòng khác trong file", HttpStatus.BAD_REQUEST),
    PHONE_INVALID(4000, "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0", HttpStatus.BAD_REQUEST),
    BATCH_INVALID(4000, "Khóa học không đúng định dạng (VD: K64)", HttpStatus.BAD_REQUEST),
    CLASS_NAME_INVALID(4000, "Tên lớp không được chứa ký tự đặc biệt", HttpStatus.BAD_REQUEST),
    MAJOR_EMPTY(4000, "Ngành học không được để trống", HttpStatus.BAD_REQUEST),
    MAJOR_INVALID(4000, "Ngành học không tồn tại trong hệ thống", HttpStatus.BAD_REQUEST),
    STUDENT_EMAIL_DOMAIN_INVALID(4000, "Email sinh viên phải có đuôi @gmail.com", HttpStatus.BAD_REQUEST),
    STUDENT_INFO_INVALID(4000, "Thông tin nhập không hợp lệ, vui lòng kiểm tra lại", HttpStatus.BAD_REQUEST),

    // STUDENT IMPORT - DATABASE
    STUDENT_IDENTITY_CONFLICT(3004, "Email (hoặc Mã sinh viên) đã thuộc về một tài khoản khác trong hệ thống", HttpStatus.CONFLICT),
    STUDENT_PROFILE_MISMATCH(3005, "Thông tin không khớp với hồ sơ gốc trong hệ thống", HttpStatus.CONFLICT),
    // Cảnh báo vàng: không phải lỗi, trả HTTP 200 kèm code riêng
    // Dùng khi bấm Kích hoạt (cách 3) trên sinh viên đang hoạt động sẵn
    STUDENT_ALREADY_ACTIVE(1100, "Sinh viên đã tồn tại/đang hoạt động trong kì này", HttpStatus.OK),
    // Sinh viên đang hoạt động và nhập lại (form hoặc Excel) y hệt thông tin cũ
    STUDENT_NO_CHANGE(1101, "Sinh viên đã có trong hệ thống, thông tin không thay đổi", HttpStatus.OK),

    // STUDENT ACTIVATE (cách 3: kích hoạt từ danh sách)
    STUDENT_NOT_FOUND(3008, "Sinh viên không tồn tại", HttpStatus.NOT_FOUND),

    // TEACHER IMPORT - ROW (dùng chung cho import Excel và nhập thủ công)
    ORGANIZATION_INVALID(4000, "Đơn vị công tác chỉ nhận Trong trường hoặc Ngoài trường", HttpStatus.BAD_REQUEST),
    ORGANIZATION_NOT_MATCH_EMAIL(4000, "Đơn vị công tác không khớp với email (email @tlu.edu.vn là Trong trường, email khác là Ngoài trường)", HttpStatus.BAD_REQUEST),
    TEACHER_INFO_INVALID(4000, "Thông tin nhập không hợp lệ, vui lòng kiểm tra lại", HttpStatus.BAD_REQUEST),

    // TEACHER IMPORT - DATABASE
    TEACHER_IDENTITY_CONFLICT(3006, "Email đã thuộc về một tài khoản khác trong hệ thống", HttpStatus.CONFLICT),
    TEACHER_PROFILE_MISMATCH(3007, "Thông tin không khớp với hồ sơ gốc trong hệ thống", HttpStatus.CONFLICT),
    // Cảnh báo vàng: không phải lỗi, trả HTTP 200 kèm code riêng. Dùng chung cho cả 3 cách thêm giáo viên
    TEACHER_ALREADY_ACTIVE(1100, "Thông tin giáo viên đã có trong hệ thống", HttpStatus.OK),

    // TEACHER ACTIVATE (cách 3: kích hoạt từ danh sách)
    TEACHER_NOT_FOUND(3009, "Giáo viên không tồn tại", HttpStatus.NOT_FOUND),
    ;
    private int code;
    private String message;
    private HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
