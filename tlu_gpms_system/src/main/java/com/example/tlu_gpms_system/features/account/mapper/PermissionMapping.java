package com.example.tlu_gpms_system.features.account.mapper;

import com.example.tlu_gpms_system.features.account.dto.request.PermissionRequest;
import com.example.tlu_gpms_system.features.account.dto.response.PermissionResponse;
import com.example.tlu_gpms_system.features.account.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PermissionMapping {

    @Mapping(target = "name", source = "permissionName")
    Permission toPermision (PermissionRequest request);
    void toPermissionUpdate(@MappingTarget Permission permission, PermissionRequest request);

    @Mapping(target = "permissionName", source = "name")
    PermissionResponse toPermissionResponse (Permission request);
    List<PermissionResponse> toPermissionListResponse (List<Permission> request);


}
