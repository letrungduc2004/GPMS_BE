package com.example.tlu_gpms_system.features.account.controller;
import com.example.tlu_gpms_system.common.dto.APIResponse;
import com.example.tlu_gpms_system.features.account.dto.request.RoleRequest;
import com.example.tlu_gpms_system.features.account.dto.response.RoleResponse;
import com.example.tlu_gpms_system.features.account.service.RoleService;
import com.example.tlu_gpms_system.features.account.service.impl.RoleServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/role")
    public APIResponse<List<RoleResponse>> getAllRole() {
        APIResponse<List<RoleResponse>> response = APIResponse.<List<RoleResponse>>builder()
                .code(1000)
                .data(roleService.getAllRole())
                .build();
        return response;
    }

    @PostMapping("/role")
    public APIResponse<Void> createRole(@RequestBody RoleRequest request) {
        roleService.createRole(request);
        APIResponse<Void> response = APIResponse.<Void>builder()
                .code(1000)
                .message("Tạo role thành công")
                .build();
        return response;
    }

    @DeleteMapping("/role/{roleName}")
    public APIResponse<RoleResponse> deleteRole(@PathVariable(name = "roleName") String idRole) {
        roleService.deleteRole(idRole);
        APIResponse<RoleResponse> response = APIResponse.<RoleResponse>builder()
                .code(1000)
                .message("Xóa Role thành công")
                .build();
        return response;
    }
}
