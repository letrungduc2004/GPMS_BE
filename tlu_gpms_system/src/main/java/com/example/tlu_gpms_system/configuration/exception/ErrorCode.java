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
    AMOUNT_INVALID(4000, "Số lượng sinh viên hướng dẫn không hợp lệ", HttpStatus.BAD_REQUEST),

    // BUSINESS LOGIC
    EMAIL_EXIST(3001, "Email đã tồn tại trong hệ thống", HttpStatus.CONFLICT),
    STUDENT_CODE_EXIST(3002, "Mã sinh viên đã tồn tại trong hệ thống", HttpStatus.CONFLICT),
    MAJOR_NOT_FOUND(3003, "Ngành học không tồn tại", HttpStatus.NOT_FOUND),
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
