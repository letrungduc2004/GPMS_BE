package com.example.tlu_gpms_system.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;

@Configuration
public class CustomJwtDecoder implements JwtDecoder {

    private final NimbusJwtDecoder nimbusJwtDecoder;

    public CustomJwtDecoder(@Value("${jwt.sign-key}")String SIGN_KEY, CustomJwtBlackListToken customJwtBlackListToken) {
        SecretKeySpec spec = new SecretKeySpec(SIGN_KEY.getBytes(), "HS256");
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(spec)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Set time refresh hết hạn chuẩn, không delay giống createDefault
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ZERO);
        JwtIssuerValidator issuer = new JwtIssuerValidator("https://www.tlu-gpms.com");
        OAuth2TokenValidator<Jwt> jwtRules = new DelegatingOAuth2TokenValidator<>(
                timestamp, // verifier time
                issuer, // verifier người cấp
                customJwtBlackListToken // verifier black list
        );
        jwtDecoder.setJwtValidator(jwtRules);
        this.nimbusJwtDecoder = jwtDecoder;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        return nimbusJwtDecoder.decode(token);
    }
}
