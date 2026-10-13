package com.example.tlu_gpms_system.configuration.security;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
//import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Configuration
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorCode code = ErrorCode.FORBIDDEN;
        log.warn("Bạn không có quyền truy cập - Lỗi 403");
        APIResponse apiResponse = APIResponse.builder()
                .code(code.getCode())
                .message(code.getMessage())
                .build();

        response.setStatus(code.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(apiResponse));
    }
}
