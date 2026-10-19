package com.example.tlu_gpms_system.features.semester.mapper;

import com.example.tlu_gpms_system.common.enums.SemesterStatus;
import com.example.tlu_gpms_system.features.semester.dto.request.SemesterRequest;
import com.example.tlu_gpms_system.features.semester.dto.response.SemesterResponse;
import com.example.tlu_gpms_system.features.semester.entity.Semester;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SemesterMapping {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Semester toEntity(SemesterRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void toUpdate(@MappingTarget Semester entity, SemesterRequest request);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "statusCode", source = "semester", qualifiedByName = "mapStatusCode")
    @Mapping(target = "status", source = "semester", qualifiedByName = "mapStatusDescription")
    SemesterResponse toResponse(Semester semester);

    List<SemesterResponse> toListResponse(List<Semester> semesters);

    @Named("mapStatusCode")
    default SemesterStatus mapStatusCode(Semester semester) {
        if (semester == null) {
            return null;
        }
        if (semester.getStatus() != null) {
            return semester.getStatus();
        }
        return semester.calculateStatus();
    }

    @Named("mapStatusDescription")
    default String mapStatusDescription(Semester semester) {
        if (semester == null) {
            return null;
        }
        SemesterStatus effectiveStatus = semester.getStatus() != null ? semester.getStatus() : semester.calculateStatus();
        return effectiveStatus != null ? effectiveStatus.getDescription() : null;
    }
}
