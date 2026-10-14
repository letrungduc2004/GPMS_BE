package com.example.tlu_gpms_system.features.notification.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.features.notification.dto.request.NotificationRequest;
import com.example.tlu_gpms_system.features.notification.dto.response.NotificationResponse;
import com.example.tlu_gpms_system.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/notification" )
    public APIResponse<List<NotificationResponse>> getNotification() {
        return APIResponse.<List<NotificationResponse>>builder()
                .code(1000)
                .data(notificationService.getNotification())
                .message("Danh sách thông báo chưa đọc")
                .build();
    }

    @GetMapping(value = "/connection" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectNotification() {
        return notificationService.getConnection();
    }

//    @PostMapping("/notification" )
//    public APIResponse<Void > pushNotification(@RequestBody NotificationRequest message) {
//        notificationService.pushNotification(message);
//        return APIResponse.<Void>builder()
//                .code(1000)
//                .message("Đã gửi thông báo")
//                .build();
//    }

//    @PatchMapping("/notification" )
//    public APIResponse<Void > readNotification() {
//        notificationService.readNotification();
//        return APIResponse.<Void>builder()
//                .code(1000)
//                .message("Đã đọc toàn bộ thông báo")
//                .build();
//    }
}
