package com.example.tlu_gpms_system.features.account.service;

import com.example.tlu_gpms_system.common.enums.Role;

// Service ghi role của tài khoản cho feature khác (ví dụ faculty khi gán Trưởng ngành).
// Tách riêng khỏi UserService để tránh vòng phụ thuộc: UserServiceImpl đã gọi MajorService
public interface AccountRoleService {

    // Thêm role cho tài khoản; đã có thì bỏ qua
    void grantRole(Long accountId, Role role);

    // Gỡ role khỏi tài khoản; chưa có thì bỏ qua
    void revokeRole(Long accountId, Role role);
}
