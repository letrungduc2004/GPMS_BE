package com.example.tlu_gpms_system.features.account.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.HashSet;
import java.util.Set;

@Table(name = "account")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Không cần publicId dùng để truy xuất thông tin
    // Vì thông tin đã lưu trong Security Context Holder
    @Column(nullable = false, unique = true)
    String email;

    @Column(name = "first_name", nullable = false)
    String firstName; // Họ

    @Column(name = "last_name", nullable = false)
    String lastName; // Tên

    @Column(name = "phone", nullable = false, unique = true)
    String phone;

    @Column(name = "status", nullable = false)
    Boolean status;

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_name"))
    Set<Role> roles = new HashSet<>();
}
