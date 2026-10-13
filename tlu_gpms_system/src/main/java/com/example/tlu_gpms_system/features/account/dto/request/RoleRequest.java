package com.example.tlu_gpms_system.features.account.dto.request;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {
    private String roleName;
    private String description;

    private Set<String> permissionName;
}
