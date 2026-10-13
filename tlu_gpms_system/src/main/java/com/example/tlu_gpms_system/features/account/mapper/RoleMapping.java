package com.example.tlu_gpms_system.features.account.mapper;
import com.example.tlu_gpms_system.features.account.dto.request.RoleRequest;
import com.example.tlu_gpms_system.features.account.dto.response.RoleResponse;
import com.example.tlu_gpms_system.features.account.entity.Permission;
import com.example.tlu_gpms_system.features.account.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapping {

    @Mapping(target = "name", source = "roleName")
    @Mapping(target = "permissions",ignore = true)
    Role toRole(RoleRequest role);

    @Mapping(target = "roleName", source = "name")
    @Mapping(target = "permissionName", source = "permissions", qualifiedByName = "definedPermissionName")
    // Không thể mapping trực tiếp Set<Permission> => Set<String>
    RoleResponse toRoleResponse(Role role);
    List<RoleResponse> toRoleListResponse(List<Role> role);

    @Named("definedPermissionName")
    default String custom(Permission permissions){
        //  Set<Permission> => Set<String>
        return permissions.getName();
    }
}
