package com.example.tlu_gpms_system.features.account.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntrospectRequest {
    @NotEmpty(message = "TOKEN_EMPTY")
    private String token;
}
