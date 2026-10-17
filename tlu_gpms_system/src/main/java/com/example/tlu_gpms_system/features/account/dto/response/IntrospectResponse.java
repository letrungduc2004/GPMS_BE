package com.example.tlu_gpms_system.features.account.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntrospectResponse {
    @JsonIgnore
    private String token;

    @Builder.Default
    private boolean authenticated = true;
}
