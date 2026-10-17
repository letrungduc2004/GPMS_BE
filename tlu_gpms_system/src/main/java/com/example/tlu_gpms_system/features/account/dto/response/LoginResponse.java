package com.example.tlu_gpms_system.features.account.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    @JsonIgnore
    private String token;
    private boolean authenticated;
    private String name;
    private Set<String> roles;
}
