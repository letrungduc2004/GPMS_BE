package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.RoleRequest;
import com.example.tlu_gpms_system.features.account.dto.response.RoleResponse;
import com.example.tlu_gpms_system.features.account.entity.Permission;
import com.example.tlu_gpms_system.features.account.entity.Role;
import com.example.tlu_gpms_system.features.account.mapper.RoleMapping;
import com.example.tlu_gpms_system.features.account.repository.PermissionRepository;
import com.example.tlu_gpms_system.features.account.repository.RoleRepository;
import com.example.tlu_gpms_system.features.account.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapping mapping;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RoleResponse> getAllRole() {
        List<Role> getAll = roleRepository.findAll();
        List<RoleResponse> convertResponse = mapping.toRoleListResponse(getAll);
        return convertResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleRequest request) {
        boolean existingRole = roleRepository.existsById(request.getRoleName());
        if (existingRole) {
            throw new AppException(ErrorCode.ROLE_EXIST);
        }
        List<Permission> getPermissionName = permissionRepository.findAllById(request.getPermissionName());
        Role mappingRole = mapping.toRole(request);
        mappingRole.setPermissions(new HashSet<>(getPermissionName));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String request) {
        Role existingRole = roleRepository.findById(request).orElseThrow(()
                -> new AppException(ErrorCode.ROLE_NOT_EXIST));
        roleRepository.deleteById(request);
    }
}
