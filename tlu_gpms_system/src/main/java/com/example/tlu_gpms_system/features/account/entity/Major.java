package com.example.tlu_gpms_system.features.account.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    // Bảng ngành
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin
    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UuidCreator.getTimeOrderedEpoch();

    // Mã ngành, dùng làm key khi import sinh viên
    // Để nullable vì bảng major đã có dữ liệu cũ chưa có mã
    @Column(name = "code", unique = true)
    String code;

    @Column(name = "name", nullable = false, unique = true)
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    Faculty faculty;
}
