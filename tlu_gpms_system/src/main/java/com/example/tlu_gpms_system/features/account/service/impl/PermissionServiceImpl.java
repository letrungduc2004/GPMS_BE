package com.example.tlu_gpms_system.features.account.service.impl;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.PermissionRequest;
import com.example.tlu_gpms_system.features.account.dto.response.PermissionResponse;
import com.example.tlu_gpms_system.features.account.entity.Permission;
import com.example.tlu_gpms_system.features.account.mapper.PermissionMapping;
import com.example.tlu_gpms_system.features.account.repository.PermissionRepository;
import com.example.tlu_gpms_system.features.account.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapping mapping;

    @Override
    public List<PermissionResponse> getAllPermission() {
        List<Permission> getAll = permissionRepository.findAll();
        List<PermissionResponse> convertResponse = mapping.toPermissionListResponse(getAll);
        return convertResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPermission(PermissionRequest request) {
        boolean existingPermission = permissionRepository.existsById(request.getPermissionName());
        if(existingPermission){
            throw new AppException(ErrorCode.PERMISSION_EXIST);
        }
        Permission permission = mapping.toPermision(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(String request) {
        Permission existingPermission = permissionRepository.findById(request).orElseThrow(()
                -> new AppException(ErrorCode.PERMISSION_NOT_EXIST));
        permissionRepository.deleteById(request);
    }

}
