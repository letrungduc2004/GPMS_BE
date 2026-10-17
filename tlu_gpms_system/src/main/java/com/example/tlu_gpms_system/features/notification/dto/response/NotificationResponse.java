package com.example.tlu_gpms_system.features.notification.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private boolean read;
    private String message;
    private LocalDateTime createdAt;
}
