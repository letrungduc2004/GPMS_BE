package com.example.tlu_gpms_system.features.notification.entity;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "notification")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "content", nullable = false)
    String content;

//    @Column(name = "type", nullable = false)
//    String type;// INFO, WARNING, SUCCESS

    @Column(name = "is_read", nullable = false)
    boolean isRead;

    @Column(name = "created_at", nullable = false)
    LocalDateTime created_at;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    Account account; // Message truyền đến ai ?
}
