package com.example.tlu_gpms_system.features.faculty.mapper;

import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyDetailResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadOfDepartmentResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FacultyMapping {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "headOfDepartment", ignore = true)
    @Mapping(target = "majors", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Faculty toEntity(FacultyCreationRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "headOfDepartment", ignore = true)
    @Mapping(target = "majors", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void toUpdate(@MappingTarget Faculty entity, FacultyUpdateRequest request);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "headOfDepartment", source = "faculty.headOfDepartment", qualifiedByName = "mapHeadOfDepartment")
    @Mapping(target = "totalMajors", ignore = true)
    @Mapping(target = "status", source = "faculty", qualifiedByName = "mapFacultyStatusDescription")
    @Mapping(target = "statusCode", source = "status")
    FacultyResponse toResponse(Faculty faculty);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "publicId", source = "publicId")
    @Mapping(target = "headOfDepartment", source = "faculty.headOfDepartment", qualifiedByName = "mapHeadOfDepartment")
    @Mapping(target = "status", source = "faculty", qualifiedByName = "mapFacultyStatusDescription")
    @Mapping(target = "statusCode", source = "status")
    FacultyDetailResponse toDetailResponse(Faculty faculty);

    @Named("mapHeadOfDepartment")
    default HeadOfDepartmentResponse mapHeadOfDepartment(Account account) {
        if (account == null) {
            return null;
        }
        String lastName = account.getLastName() != null ? account.getLastName().trim() : "";
        String firstName = account.getFirstName() != null ? account.getFirstName().trim() : "";
        String fullName = (lastName + " " + firstName).trim();
        if (fullName.isEmpty()) {
            fullName = account.getEmail();
        }
        return HeadOfDepartmentResponse.builder()
                .id(account.getId())
                .fullName(fullName)
                .title(null)
                .build();
    }

    @Named("mapFacultyStatusDescription")
    default String mapFacultyStatusDescription(Faculty faculty) {
        if (faculty == null || faculty.getStatus() == null) {
            return null;
        }
        return faculty.getStatus().getDescription();
    }
}
