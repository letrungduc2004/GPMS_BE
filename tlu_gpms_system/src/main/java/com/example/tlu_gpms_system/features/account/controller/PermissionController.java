package com.example.tlu_gpms_system.features.account.controller;

import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.features.account.dto.request.PermissionRequest;
import com.example.tlu_gpms_system.features.account.dto.response.PermissionResponse;
import com.example.tlu_gpms_system.features.account.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    @GetMapping("/permission")
    public APIResponse<List<PermissionResponse>> getAllPermission() {
        APIResponse<List<PermissionResponse>> response = APIResponse.<List<PermissionResponse>>builder()
                .code(1000)
                .data(permissionService.getAllPermission())
                .build();
        return response;
    }

    @PostMapping("/permission")
    public APIResponse<Void> createPermission(@RequestBody PermissionRequest request) {
        permissionService.createPermission(request);
        APIResponse<Void> response = APIResponse.<Void>builder()
                .code(1000)
                .message("Tạo permission thành công")
                .build();
        return response;
    }

    @DeleteMapping("/permission/{permissionName}")
    public APIResponse<Void> deletePermission(@PathVariable(name = "permissionName") String idPermission) {
        permissionService.deletePermission(idPermission);
        APIResponse<Void> response = APIResponse.<Void>builder()
                .code(1000)
                .message("Xóa permission thành công")
                .build();
        return response;
    }

}
