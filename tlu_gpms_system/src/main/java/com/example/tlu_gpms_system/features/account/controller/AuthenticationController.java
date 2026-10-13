package com.example.tlu_gpms_system.features.account.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.IntrospectRequest;
import com.example.tlu_gpms_system.features.account.dto.response.IntrospectResponse;
import com.example.tlu_gpms_system.features.account.dto.response.LoginResponse;
import com.example.tlu_gpms_system.features.account.dto.response.UserInfoResponse;
import com.example.tlu_gpms_system.features.account.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public APIResponse<LoginResponse> login(@RequestParam(name = "code") String code,
                                            HttpServletResponse servletResponse) {
        LoginResponse loginResponse = authenticationService.login(code);

        // Tạo HttpOnly Cookie chứa access_token
        ResponseCookie cookie = ResponseCookie.from("access_token", loginResponse.getToken())
                .httpOnly(true)          // Chống XSS (JavaScript không đọc được)
                .secure(false)           // Đổi thành true khi chạy trên môi trường HTTPS production
                .path("/")               // Có hiệu lực trên toàn domain
                .maxAge(Duration.ofHours(2))
                .sameSite("Lax")         // Cho phép gửi cookie khi chuyển trang
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return APIResponse.<LoginResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công")
                .data(loginResponse)
                .build();
    }

    @PostMapping("/refresh-token")
    public APIResponse<IntrospectResponse> refreshToken(
            @RequestBody(required = false) IntrospectRequest request,
            @CookieValue(name = "access_token", required = false) String tokenFromCookie,
            HttpServletResponse servletResponse) {

        String token = (request != null && request.getToken() != null && !request.getToken().isBlank())
                ? request.getToken()
                : tokenFromCookie;

        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        IntrospectResponse introspectResponse = authenticationService.refreshToken(
                IntrospectRequest.builder().token(token).build());

        // Cập nhật lại access_token mới vào HttpOnly Cookie
        ResponseCookie cookie = ResponseCookie.from("access_token", introspectResponse.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofHours(2))
                .sameSite("Lax")
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return APIResponse.<IntrospectResponse>builder()
                .code(1000)
                .data(introspectResponse)
                .build();
    }

    @PostMapping("/logout")
    public APIResponse<Void> logoutToken(
            @RequestBody(required = false) IntrospectRequest request,
            @CookieValue(name = "access_token", required = false) String tokenFromCookie,
            HttpServletResponse servletResponse) {

        String token = (request != null && request.getToken() != null && !request.getToken().isBlank())
                ? request.getToken()
                : tokenFromCookie;

        if (token != null && !token.isBlank()) {
            authenticationService.logout(IntrospectRequest.builder().token(token).build());
        }

        // Xóa cookie bằng cách set maxAge = 0
        ResponseCookie cleanCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cleanCookie.toString());

        return APIResponse.<Void>builder()
                .code(1000)
                .message("Bạn đã đăng xuất tài khoản")
                .build();
    }

    @GetMapping("/my-info")
    public APIResponse<UserInfoResponse> getInfo() {
        return APIResponse.<UserInfoResponse>builder()
                .code(1000)
                .data(authenticationService.userInfo())
                .message("Thông tin người dùng")
                .build();
    }
}
