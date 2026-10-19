package com.example.tlu_gpms_system.features.semester.entity;

import com.example.tlu_gpms_system.common.enums.SemesterStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "semester")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin từ bên ngoài qua API
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    @Builder.Default
    private UUID publicId = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "name", nullable = false, unique = true)
    String name;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    SemesterStatus status;

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
            this.status = calculateStatus();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Tự động tính toán trạng thái dựa trên ngày hiện tại so với startDate và endDate
     */
    public SemesterStatus calculateStatus() {
        LocalDate now = LocalDate.now();
        if (startDate != null && now.isBefore(startDate)) {
            return SemesterStatus.UPCOMING;
        } else if (endDate != null && now.isAfter(endDate)) {
            return SemesterStatus.COMPLETED;
        } else {
            return SemesterStatus.ONGOING;
        }
    }
}
