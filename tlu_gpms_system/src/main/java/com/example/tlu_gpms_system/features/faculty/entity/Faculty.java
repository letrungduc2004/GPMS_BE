package com.example.tlu_gpms_system.features.faculty.entity;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "faculty")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin từ bên ngoài qua API

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    @Builder.Default
    UUID publicId = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "code", nullable = false, unique = true)
    String code;

    @Column(name = "name", nullable = false, unique = true)
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_of_department_id")
    Account headOfDepartment;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    ActiveStatus status = ActiveStatus.ACTIVE;

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Major> majors = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
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
