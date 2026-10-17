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
    // Hiển thị toàn bộ thông báo, thông báo chưa đọc hiển thị lên đầu
    // Sắp xếp thời gian mới nhất -> cũ nhất     LocalDateTime createdAt;
    @Query("select n from Notification n " +
            "where n.account.id = :accountId " +
            "and (:isRead is null or n.isRead = :isRead) " +
            "order by n.isRead asc, n.createdAt desc")
    List<Notification> findNotifications(@Param("accountId") Long accountId, @Param("isRead") Boolean isRead);


    // Update toàn bộ thông báo chưa đọc theo userId
    @Modifying
    @Query("update Notification n set n.isRead = true where n.account.id = :accountId")
    void updateNotification(@Param("accountId") Long accountId);

}
