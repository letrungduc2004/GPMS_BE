package com.example.tlu_gpms_system.features.notification.service.impl;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.service.AuthenticationService;
import com.example.tlu_gpms_system.features.notification.dto.response.NotificationResponse;
import com.example.tlu_gpms_system.features.notification.entity.Notification;
import com.example.tlu_gpms_system.features.notification.mapper.NotificationMapping;
import com.example.tlu_gpms_system.features.notification.repository.NotificationRepository;
import com.example.tlu_gpms_system.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final Map<Long, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();
    private final NotificationRepository notificationRepository;
    private final AuthenticationService authenticationService;
    private final NotificationMapping notificationMapping;

    // Lấy các kết nối mà User chưa đọc
    @Override
    @Transactional
    public List<NotificationResponse> getNotification(Boolean isRead) {
        Account account = authenticationService.getAccountInfo();
        List<Notification> notification = notificationRepository.findNotifications(account.getId(), isRead);
        return notificationMapping.toNotiListResponse(notification);
    }

    @Override
    public SseEmitter getConnection() {
        // Thông tin kết nối user
        Account account = authenticationService.getAccountInfo();

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitterMap.computeIfAbsent(account.getId(), k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = emitterMap.get(account.getId());
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    emitterMap.remove(account.getId());
                }
            }
        };
        // Dọn dẹp SseEmitter khi client ngắt / timeout / lỗi
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected")
                    .data("to client " + account.getId()));
        } catch (IOException e) {
            cleanup.run();
        }
        return emitter;
    }

    @Override
    @Transactional
    public void pushNotification(String message, Account recipient) {
        // Người nhận: recipient
        Notification notification = new Notification();
        notification.setContent(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setAccount(recipient);
        notification.setRead(false);
        notificationRepository.save(notification);
        // KIỂM TRA account ONLINE ĐỂ ĐẨY QUA SSE
        List<SseEmitter> emitters = emitterMap.get(recipient.getId());
        if (emitters != null && !emitters.isEmpty()) {
            NotificationResponse response = notificationMapping.toNotiResponse(notification);
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(notification.getId()))
                            .name("notification")
                            .data(response));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            }
        }
    }

    @Override
    @Transactional
    public void readNotification() {
        Account account = authenticationService.getAccountInfo();
        // Update toàn bộ thông báo chưa đọc theo AccountId
        notificationRepository.updateNotification(account.getId());
    }

    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        emitterMap.values().forEach(
                list -> list.forEach(em -> {
                    try {
                        em.send(SseEmitter.event().comment("ping"));
                    } catch (IOException e) {
                        list.remove(em);
                    }
                }));
    }
}
