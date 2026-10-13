package com.example.tlu_gpms_system.features.notification.repository;

import com.example.tlu_gpms_system.features.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Lấy các kết nối mà người dùng chưa đọc
    @Query("select n from Notification n " +
            "where n.account.id =:accountId and n.isRead = false ")
    List<Notification> findNotificationNotRead(Long accountId);

    // Update toàn bộ thông báo chưa đọc theo userId
    @Modifying
    @Query("update Notification n set n.isRead = true where n.account.id = :accountId")
    void updateNotification(@Param("accountId") Long accountId);

}
