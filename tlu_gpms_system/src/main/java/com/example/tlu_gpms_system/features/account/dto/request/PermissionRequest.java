package com.example.tlu_gpms_system.features.account.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRequest {
    @JsonProperty("name")
    private String permissionName;
    private String description;
}
