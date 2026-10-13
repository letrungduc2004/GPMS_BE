package com.example.tlu_gpms_system.features.account.service;

import com.example.tlu_gpms_system.features.account.dto.request.PermissionRequest;
import com.example.tlu_gpms_system.features.account.dto.response.PermissionResponse;

import java.util.List;

public interface PermissionService {
    List<PermissionResponse> getAllPermission();
    void createPermission(PermissionRequest request);
    void deletePermission(String permissionId);
}
