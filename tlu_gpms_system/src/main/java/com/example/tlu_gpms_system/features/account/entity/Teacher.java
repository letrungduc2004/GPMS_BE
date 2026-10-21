package com.example.tlu_gpms_system.features.account.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Table(name = "teacher")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin
    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "organization", nullable = false)
    String organization; // Đơn vị công tác: tên trường (trong trường luôn là "Trường Đại học Thủy lợi")

    // Số lượng sinh viên tối đa hướng dẫn
    @Column(name = "amount", nullable = false)
    Long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id") // null với giáo viên ngoài trường
    Major major;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;
}
