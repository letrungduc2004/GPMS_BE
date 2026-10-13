package com.example.tlu_gpms_system.features.account.service;

import com.example.tlu_gpms_system.features.account.dto.request.RoleRequest;
import com.example.tlu_gpms_system.features.account.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> getAllRole();
    void createRole(RoleRequest request);
    void deleteRole(String roleId);
}
