package com.example.tlu_gpms_system.features.account.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutBoundUserResponse {
    String id;
    String email;

    @JsonProperty("verified_email")
    String verifiedEmail;

    String name; // Đức Lê Trung

    @JsonProperty("given_name")
    String givenName; // Đức

    @JsonProperty("family_name")
    String familyName; // Lê Trung

    String picture; // Lê Trung
}
