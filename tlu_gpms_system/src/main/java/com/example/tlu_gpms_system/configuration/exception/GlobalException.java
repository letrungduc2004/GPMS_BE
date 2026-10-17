package com.example.tlu_gpms_system.configuration.exception;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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
                .message(errorCode.getMessage())
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
