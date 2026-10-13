package com.example.tlu_gpms_system.features.account.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {
    private String permissionName;
    private String description;
}
