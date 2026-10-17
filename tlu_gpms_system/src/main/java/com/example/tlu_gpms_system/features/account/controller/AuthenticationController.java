package com.example.tlu_gpms_system.features.account.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.IntrospectRequest;
import com.example.tlu_gpms_system.features.account.dto.request.LoginRequest;
import com.example.tlu_gpms_system.features.account.dto.response.IntrospectResponse;
import com.example.tlu_gpms_system.features.account.dto.response.LoginPostManResponse;
import com.example.tlu_gpms_system.features.account.dto.response.LoginResponse;
import com.example.tlu_gpms_system.features.account.dto.response.UserInfoResponse;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.example.tlu_gpms_system.features.account.entity.Role;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {
    private final AccountRepository accountRepository;
    private final AuthenticationService authenticationService;

    @Value("${jwt.time-expiration}")
    private long expirationTime;

    @Value("${jwt.time-refresh}")
    private long timeRefresh;

    @PostMapping("/login")
    public APIResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request,
            HttpServletResponse servletResponse) {
        LoginResponse loginResponse = authenticationService.login(request.getCode());

        // Tạo HttpOnly Cookie chứa access_token
        ResponseCookie cookie = createAccessTokenCookie(loginResponse.getToken(), getCookieDuration());
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
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            HttpServletResponse servletResponse) {

        String token = (request != null && request.getToken() != null && !request.getToken().isBlank())
                ? request.getToken()
                : (tokenFromCookie != null && !tokenFromCookie.isBlank())
                        ? tokenFromCookie
                        : refreshTokenFromCookie;

        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        IntrospectResponse introspectResponse = authenticationService.refreshToken(
                IntrospectRequest.builder().token(token).build());

        // Cập nhật lại access_token mới vào HttpOnly Cookie
        ResponseCookie cookie = createAccessTokenCookie(introspectResponse.getToken(), getCookieDuration());
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return APIResponse.<IntrospectResponse>builder()
                .code(1000)
                .data(introspectResponse)
                .build();
    }

    @PostMapping("/log-out")
    public APIResponse<Void> logoutToken(
            @RequestBody(required = false) IntrospectRequest request,
            @CookieValue(name = "access_token", required = false) String tokenFromCookie,
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            HttpServletResponse servletResponse) {

        String token = (request != null && request.getToken() != null && !request.getToken().isBlank())
                ? request.getToken()
                : (tokenFromCookie != null && !tokenFromCookie.isBlank())
                        ? tokenFromCookie
                        : refreshTokenFromCookie;

        if (token != null && !token.isBlank()) {
            try {
                authenticationService.logout(IntrospectRequest.builder().token(token).build());
            } catch (Exception e) {
                log.warn("Token already invalid or expired during logout: {}", e.getMessage());
            }
        }

        // Xóa cả cookie access_token và refresh_token bằng Max-Age=0, cùng tên và Path="/"
        ResponseCookie cleanAccessCookie = createCookie("access_token", "", Duration.ZERO);
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cleanAccessCookie.toString());

        ResponseCookie cleanRefreshCookie = createCookie("refresh_token", "", Duration.ZERO);
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString());

        return APIResponse.<Void>builder()
                .code(1000)
                .message("Bạn đã đăng xuất tài khoản")
                .build();
    }

    @GetMapping("/my-info")
    @Transactional(readOnly = true)
    public APIResponse<UserInfoResponse> getInfo() {
        return APIResponse.<UserInfoResponse>builder()
                .code(1000)
                .data(authenticationService.userInfo())
                .message("Thông tin người dùng")
                .build();
    }

    @PostMapping("/login-postman")
    @Transactional
    public APIResponse<LoginPostManResponse> loginPostMan(@RequestBody String email,
            HttpServletResponse servletResponse) {
        Account account = accountRepository.findByEmailAndStatus(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_INVALID));
        String createToken = authenticationService.createToken(account);

        Set<String> roles = account.getRoles() != null
                ? account.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
                : Collections.emptySet();
        StringBuilder builder = new StringBuilder();
        if (account.getFirstName() != null) {
            builder.append(account.getFirstName()).append(" ");
        }
        if (account.getLastName() != null) {
            builder.append(account.getLastName());
        }
        LoginPostManResponse response = LoginPostManResponse.builder()
                .token(createToken)
                .authenticated(true)
                .name(builder.toString().trim())
                .role(roles)
                .build();

        return APIResponse.<LoginPostManResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công")
                .data(response)
                .build();
    }

    private ResponseCookie createAccessTokenCookie(String token, Duration maxAge) {
        return createCookie("access_token", token, maxAge);
    }

    private ResponseCookie createCookie(String name, String token, Duration maxAge) {
        return ResponseCookie.from(name, token)
                .httpOnly(true) // Chống XSS (JavaScript không đọc được)
                .secure(false) // Đổi thành true khi chạy trên môi trường HTTPS production
                .path("/") // Có hiệu lực trên toàn domain
                .maxAge(maxAge)
                .sameSite("Lax") // Cho phép gửi cookie khi chuyển trang
                .build();
    }

    private Duration getCookieDuration() {
        return Duration.ofMillis(expirationTime + timeRefresh);
    }
}
