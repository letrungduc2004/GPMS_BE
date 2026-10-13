package com.example.tlu_gpms_system.features.account.service;


import com.example.tlu_gpms_system.features.account.dto.request.IntrospectRequest;
import com.example.tlu_gpms_system.features.account.dto.response.IntrospectResponse;
import com.example.tlu_gpms_system.features.account.dto.response.LoginResponse;
import com.example.tlu_gpms_system.features.account.dto.response.UserInfoResponse;
import com.example.tlu_gpms_system.features.account.entity.Account;

public interface AuthenticationService {
    LoginResponse login(String code);
    String createToken(Account account);
    IntrospectResponse refreshToken(IntrospectRequest request);
    void logout(IntrospectRequest request);
    Account getAccountInfo();
    UserInfoResponse userInfo();
}
