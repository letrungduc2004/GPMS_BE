package com.example.tlu_gpms_system.features.account.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Table(name = "student")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin
    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "student_code", nullable = false, unique = true)
    String studentCode;

    @Column(name = "batch", nullable = false)
    String batch; // Khóa

    @Column(name = "class_name", nullable = false)
    String className; // Tên lớp

    @Column(name = "status", nullable = false)
    Boolean status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    Major major;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;
}
