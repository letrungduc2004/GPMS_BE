package com.example.tlu_gpms_system.features.notification.mapper;

import com.example.tlu_gpms_system.features.notification.dto.response.NotificationResponse;
import com.example.tlu_gpms_system.features.notification.entity.Notification;
import com.example.tlu_gpms_system.features.notification.repository.NotificationRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface NotificationMapping {
    @Mapping(target = "message", source = "content")
    @Mapping(target = "read", source = "read")
    NotificationResponse toNotiResponse(Notification n);

    List<NotificationResponse> toNotiListResponse(List<Notification> n);
}
