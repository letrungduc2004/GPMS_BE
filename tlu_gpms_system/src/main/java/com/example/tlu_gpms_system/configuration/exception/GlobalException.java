package com.example.tlu_gpms_system.configuration.exception;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class GlobalException {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<APIResponse> exception(Exception e) {
        log.error("Bắt lỗi Controller Advice: ", e);
        ErrorCode error = ErrorCode.SYSTEM_ERROR;
        APIResponse response = APIResponse.builder()
                .code(error.getCode())
                .message(error.getMessage())
                .build();
        return ResponseEntity.status(error.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<APIResponse> appException(AppException ap) {
        ErrorCode errorCode = ap.getErrorCode();
        APIResponse response = APIResponse.builder()
                .code(errorCode.getCode())
                .message(ap.getMessage())
                .build();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Bắt lỗi validate DTO đơn lẻ (@Valid DTO)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse> methodArgument(MethodArgumentNotValidException exception) {
        String keyException = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = getErrorCode(keyException);
        APIResponse response = APIResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // Bắt lỗi validate Collection/List (@Valid List<DTO>) từ Spring 6.1+
    @ExceptionHandler(value = HandlerMethodValidationException.class)
    public ResponseEntity<APIResponse> handleMethodValidation(HandlerMethodValidationException exception) {
        String keyException = exception.getAllErrors().stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .orElse(null);

        ErrorCode errorCode = getErrorCode(keyException);
        APIResponse response = APIResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    // File upload vượt quá spring.servlet.multipart.max-file-size
    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    public ResponseEntity<APIResponse> maxUploadSize(MaxUploadSizeExceededException exception) {
        return appException(new AppException(ErrorCode.FILE_TOO_LARGE));
    }

    // Request multipart thiếu part "file"
    @ExceptionHandler(value = MissingServletRequestPartException.class)
    public ResponseEntity<APIResponse> missingRequestPart(MissingServletRequestPartException exception) {
        return appException(new AppException(ErrorCode.FILE_EMPTY));
    }

    // Tham số query sai kiểu: majorId không phải UUID, status ngoài
    // ACTIVE/INACTIVE/ALL...
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse> argumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return appException(new AppException(ErrorCode.INVALID_REQUEST));
    }

    // Body thiếu, không phải JSON hợp lệ, hoặc field sai kiểu (object / mảng vào field chuỗi, số có 0 đứng đầu...)
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse> messageNotReadable(HttpMessageNotReadableException exception) {
        return appException(new AppException(ErrorCode.INVALID_REQUEST));
    }

    // Content-Type không đúng với API (vd gửi JSON vào API import chỉ nhận multipart/form-data)
    @ExceptionHandler(value = HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<APIResponse> mediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return appException(new AppException(ErrorCode.INVALID_REQUEST));
    }

    // Request multipart không đọc được (vd tự đặt Content-Type: multipart/form-data nên mất boundary).
    // MaxUploadSizeExceededException là lớp con nhưng vẫn vào handler riêng ở trên vì Spring chọn handler cụ thể nhất
    @ExceptionHandler(value = MultipartException.class)
    public ResponseEntity<APIResponse> multipart(MultipartException exception) {
        return appException(new AppException(ErrorCode.INVALID_REQUEST));
    }

    // Gọi sai HTTP method (vd GET vào API chỉ có PATCH)
    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIResponse> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return appException(new AppException(ErrorCode.OPERATION_NOT_ALLOWED));
    }

    // URL không khớp API nào (vd /teacher/ thừa dấu /). Spring Boot mặc định ném NoResourceFoundException,
    // NoHandlerFoundException chỉ xảy ra khi tắt spring.web.resources.add-mappings
    @ExceptionHandler(value = {NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<APIResponse> noResourceFound(Exception exception) {
        return appException(new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private ErrorCode getErrorCode(String keyException) {
        if (keyException == null) {
            return ErrorCode.VALIDATION_FAIL;
        }
        try {
            return ErrorCode.valueOf(keyException);
        } catch (IllegalArgumentException e) {
            log.error("Không tìm thấy ErrorCode tương ứng với key: {}", keyException);
            return ErrorCode.VALIDATION_FAIL;
        }
    }

    // Bắt lỗi 403 từ @PreAuthorize / Method Security
    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<APIResponse> accessDenied(AccessDeniedException exception) {
        log.error("Lỗi 403 đi vào global exception");
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        APIResponse apiResponse = APIResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiResponse);
    }
}
