package com.example.tlu_gpms_system.features.faculty.mapper;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MajorMapping {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "faculty", ignore = true)
    @Mapping(target = "head", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Major toEntity(MajorCreationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "faculty", ignore = true)
    @Mapping(target = "head", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void toUpdate(@MappingTarget Major entity, MajorUpdateRequest request);

    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "status", source = "major", qualifiedByName = "mapMajorStatusDescription")
    @Mapping(target = "statusCode", source = "status")
    @Mapping(target = "headAccountId", source = "head.id")
    @Mapping(target = "headFullName", source = "head", qualifiedByName = "mapHeadFullName")
    MajorResponse toResponse(Major major);

    @Named("mapMajorStatusDescription")
    default String mapMajorStatusDescription(Major major) {
        if (major == null || major.getStatus() == null) {
            return null;
        }
        return major.getStatus().getDescription();
    }

    @Named("mapHeadFullName")
    default String mapHeadFullName(Account head) {
        if (head == null) {
            return null;
        }
        return (head.getFirstName() + " " + head.getLastName()).trim();
    }
}
