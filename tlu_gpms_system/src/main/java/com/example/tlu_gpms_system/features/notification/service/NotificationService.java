package com.example.tlu_gpms_system.features.notification.service;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.notification.dto.request.NotificationRequest;
import com.example.tlu_gpms_system.features.notification.dto.response.NotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotification();
    SseEmitter getConnection();
    void pushNotification(String message, Account recipient);
    void readNotification();
}
