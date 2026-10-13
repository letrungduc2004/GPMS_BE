package com.example.tlu_gpms_system.features.notification.dto.request;

import com.example.tlu_gpms_system.features.account.entity.Account;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    private String message;
    private Account recipient;
}
