package com.example.tlu_gpms_system.features.faculty.entity;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "major")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin từ bên ngoài qua API
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    @Builder.Default
    UUID publicId = UuidCreator.getTimeOrderedEpoch();

    // Mã ngành, dùng làm key khi import sinh viên
    // Để nullable vì bảng major đã có dữ liệu cũ chưa có mã
    @Column(name = "code", unique = true)
    String code;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    ActiveStatus status = ActiveStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    Faculty faculty;

    // Trưởng ngành; null khi ngành chưa có trưởng. unique: mỗi tài khoản làm trưởng tối đa 1 ngành
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_account_id", unique = true)
    Account head;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.publicId == null) {
            this.publicId = UuidCreator.getTimeOrderedEpoch();
        }
        if (this.status == null) {
            this.status = ActiveStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
