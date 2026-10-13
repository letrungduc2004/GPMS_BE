package com.example.tlu_gpms_system.configuration.exception;
import com.example.tlu_gpms_system.common.dto.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@Slf4j
@ControllerAdvice
public class GlobalException {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<APIResponse> exception(Exception e) {
        log.error("Bắt lỗi Controller Advice " + e.getMessage());
        e.printStackTrace();
        ErrorCode error = ErrorCode.SYSTEM_ERROR;
        APIResponse response = APIResponse.builder()
                .code(error.getCode())
                .message(error.getMessage())
                .build();
        return ResponseEntity.status(error.getHttpStatus())
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
                .body(response);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse> methodArgument(MethodArgumentNotValidException exception) {
        String keyException = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.valueOf(keyException);
        APIResponse response = APIResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
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
                .body(apiResponse);
    }

}
