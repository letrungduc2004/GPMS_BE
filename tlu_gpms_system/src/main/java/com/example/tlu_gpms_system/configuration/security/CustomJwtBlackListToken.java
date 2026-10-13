package com.example.tlu_gpms_system.configuration.security;

import com.example.tlu_gpms_system.features.account.repository.InvalidateTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@RequiredArgsConstructor
public class CustomJwtBlackListToken implements OAuth2TokenValidator<Jwt> {
   private final InvalidateTokenRepository tokenRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jwtID = token.getId();
        // Kiểm tra token trong blacklist
        if(jwtID == null){
            OAuth2Error error = new OAuth2Error("jwtID can not be null");
            return OAuth2TokenValidatorResult.failure(error);
        }
        if(jwtID != null && tokenRepository.existsById(jwtID)){
            OAuth2Error error = new OAuth2Error("jwtID has exist Blacklist");
            return OAuth2TokenValidatorResult.failure(error);
        }
        // Kiểm tra người dùng còn hạn truy cập không ?
        String accountId = token.getSubject();

        return OAuth2TokenValidatorResult.success();
    }
}
