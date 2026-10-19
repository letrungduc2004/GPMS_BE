package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.ExchangeTokenRequest;
import com.example.tlu_gpms_system.features.account.dto.request.IntrospectRequest;
import com.example.tlu_gpms_system.features.account.dto.response.*;
import com.example.tlu_gpms_system.features.account.entity.*;
import com.example.tlu_gpms_system.features.account.repository.InvalidateTokenRepository;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.httpclient.OutBoundIdentityClient;
import com.example.tlu_gpms_system.features.account.repository.httpclient.OutBoundUserClient;
import com.example.tlu_gpms_system.features.account.service.AuthenticationService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    @Value("${jwt.sign-key}")
    private String SIGN_KEY;

    @Value("${jwt.time-refresh}")
    private long timeRefresh;

    @Value("${jwt.time-expiration}")
    private long expirationTime;

    private final InvalidateTokenRepository invalidateTokenRepository;
    private final AccountRepository accountRepository;
    private final OutBoundIdentityClient outBoundIdentityClient;
    private final OutBoundUserClient outBoundUserClient;

    @Value("${oauth2.client_id}")
    private String CLIENT_ID;

    @Value("${oauth2.client_secret}")
    private String CLIENT_SECRET;

    @Value("${oauth2.redirect_url}")
    private String RIDERECT_URL;

    @Value("${oauth2.grant_type}")
    private String GRANT_TYPE;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(String code) {
        // Google Oauth2
        OutBoundUserResponse userInfo = outBoundUserResponse(code);
        // Kiểm tra tai_khoan
        Account account = accountRepository.findByEmailAndStatus(userInfo.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_INVALID));

        StringBuilder builder = new StringBuilder();
        if (account.getFirstName() != null) {
            builder.append(account.getFirstName()).append(" ");
        }
        if (account.getLastName() != null) {
            builder.append(account.getLastName());
        }

        Set<String> roles = account.getRoles() != null
                ? account.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        // Login thành công tạo token
        return LoginResponse.builder()
                .token(createToken(account))
                .authenticated(true)
                .name(builder.toString().trim())
                .roles(roles)
                .build();
    }

    // Tạo Token: header: chứa thuật toán kí(HS256)
    // payload: chứa thông tin (người dùng, thời gian tạo - hạn token)
    // signature: chữ kí số tạo từ header + payload + secret key
    @Override
    public String createToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(account.getId().toString())
                .issuer("https://www.tlu-gpms.com")
                .issueTime(new Date(System.currentTimeMillis()))
                .expirationTime(new Date(System.currentTimeMillis() + expirationTime))
                .jwtID(UUID.randomUUID().toString())
                .claim("authorities", getRoleAndPermission(account))
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGN_KEY));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRoleAndPermission(Account account) {
        StringJoiner joiner = new StringJoiner(" ");
        Set<Role> roles = account.getRoles();
        for (Role r : roles) {
            joiner.add("ROLE_" + r.getName());
            Set<Permission> permissions = r.getPermissions();
            for (Permission p : permissions) {
                joiner.add(p.getName());
            }
        }
        return joiner.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IntrospectResponse refreshToken(IntrospectRequest request) {
        // Kiểm tra token cấp từ Server (đã kiểm tra ở verifyToken)
        // Kiểm tra blacklist (đã kiểm tra ở verifyToken)
        // Kiểm tra hạn refresh
        SignedJWT signedJWT = verifyToken(request.getToken());
        try {
            Instant expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
            Instant maxTimeRefresh = expirationTime.plusMillis(timeRefresh);

            String userId = signedJWT.getJWTClaimsSet().getSubject();
            // Kiểm tra thêm status phòng tránh case
            // Admin vô hiệu hóa nhưng người dùng vẫn còn hạn
            Account account = accountRepository.findByIdAndStatus(Long.valueOf(userId))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            // Chỉ refresh: nếu time hiện tại < (hạn token + thời gian tối đa refresh)
            if (Instant.now().isAfter(maxTimeRefresh)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            // Lưu token cũ vào BlackList
            String jwtID = signedJWT.getJWTClaimsSet().getJWTID();
            InvalidateToken token = InvalidateToken.builder()
                    .id(jwtID)
                    .expirationTime(maxTimeRefresh)
                    .build();
            invalidateTokenRepository.save(token);

            return IntrospectResponse.builder()
                    .token(createToken(account))
                    .build();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(IntrospectRequest request) {
        // Kiểm tra token cấp từ Server (đã kiểm tra ở verifyToken)
        // Kiểm tra blacklist (đã kiểm tra ở verifyToken)
        SignedJWT signedJWT = verifyToken(request.getToken());
        try {
            Instant expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
            String jwtID = signedJWT.getJWTClaimsSet().getJWTID();
            InvalidateToken token = InvalidateToken.builder()
                    .id(jwtID)
                    .expirationTime(expirationTime.plusMillis(timeRefresh))
                    .build();
            invalidateTokenRepository.save(token);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private SignedJWT verifyToken(String token) {
        try {
            JWSVerifier jwsVerifier = new MACVerifier(SIGN_KEY.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            String jwtID = signedJWT.getJWTClaimsSet().getJWTID();
            Instant expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();

            if (jwtID == null || expirationTime == null) {
                throw new AppException(ErrorCode.TOKEN_INVALID);
            }
            // Kiểm tra chữ kí
            boolean verifier = signedJWT.verify(jwsVerifier);
            // Kiểm tra blacklist
            if (!verifier || invalidateTokenRepository.existsById(jwtID)) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            return signedJWT;
        } catch (ParseException | JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public OutBoundUserResponse outBoundUserResponse(String code) {
        ExchangeTokenRequest re = ExchangeTokenRequest.builder()
                .code(code)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(RIDERECT_URL)
                .grantType(GRANT_TYPE)
                .build();
        // Tạo token mới từ google
        ExchangeTokenResponse response = outBoundIdentityClient.exchangeToken(re);

        // Dùng token mới để lấy các thông tin
        OutBoundUserResponse userInfo = outBoundUserClient.getUserInfo("json", response.getAccessToken());
        return userInfo;
    }

    @Override
    public Account getAccountInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String userId = authentication.getName();
        try {
            Long accountId = Long.valueOf(userId);
            // Kiểm tra account còn hạn không
            return accountRepository.findByIdAndStatus(accountId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        } catch (NumberFormatException e) {
            log.error("Invalid user ID in authentication token: {}", userId);
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    @Override
    @Transactional
    public UserInfoResponse userInfo() {
        Account account = getAccountInfo();
        StringBuilder builder = new StringBuilder();
        if (account.getFirstName() != null) {
            builder.append(account.getFirstName()).append(" ");
        }
        if (account.getLastName() != null) {
            builder.append(account.getLastName());
        }

        Set<String> roles = account.getRoles() != null
                ? account.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        return UserInfoResponse.builder()
                .name(builder.toString().trim())
                .roles(roles)
                .build();
    }
}
