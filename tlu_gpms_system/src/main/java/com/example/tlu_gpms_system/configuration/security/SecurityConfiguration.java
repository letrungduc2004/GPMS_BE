package com.example.tlu_gpms_system.configuration.security;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final String[] PUBLIC_POST_ENDPOINT = {
            "/login", "/refresh-token", "/logout", "/notification/**"
    };
    private final String[] PUBLIC_GET_ENDPOINT = {};
    private final CustomJwtDecoder jwtDecoder;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Cấu hình CORS cho Spring Security
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // Cấu hình endpoint mặc định
        http.authorizeHttpRequests(config ->
                config.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINT).permitAll()
                        .anyRequest().authenticated());

        // Cấu hình protected endpoint (hỗ trợ đọc token từ Header & HttpOnly Cookie)
        http.oauth2ResourceServer(oauth ->
                oauth.bearerTokenResolver(bearerTokenResolver())
                        .jwt(config -> config.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(converter()))
                        .authenticationEntryPoint(authenticationEntryPoint));

        // Cấu hình bắt lỗi 403 ở FilterChain (phân quyền theo endpoint)
        http.exceptionHandling(config ->
                config.accessDeniedHandler(accessDeniedHandler));

        http.csrf(config -> config.disable());
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter converter() {
        JwtGrantedAuthoritiesConverter grant = new JwtGrantedAuthoritiesConverter();
        grant.setAuthoritiesClaimName("authorities");
        grant.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grant);
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Hỗ trợ cả React chạy trên port 3000 lẫn Vite port 5173
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Cho phép trình duyệt gửi cookie tự động
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return request -> {
            // 1. Ưu tiên kiểm tra Header "Authorization: Bearer ..." trước (để test trên Postman / Swagger)
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }

            // 2. Cấu hình đọc Token trong HttpOnly Cookie
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("access_token".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }

            return null;
        };
    }
}
