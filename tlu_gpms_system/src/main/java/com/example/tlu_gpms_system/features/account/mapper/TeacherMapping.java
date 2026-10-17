package com.example.tlu_gpms_system.features.account.mapper;

import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TeacherMapping {

    @Mapping(target = "majorName", source = "major.name")
    @Mapping(target = "phone", source = "account.phone")
    @Mapping(target = "status", source = "account.status")
    @Mapping(target = "name", source = "teacher", qualifiedByName = "concatTeacherFullName")
    TeacherResponse toResponse(Teacher teacher);

    @Named("concatTeacherFullName")
    default String concatTeacherFullName(Teacher teacher) {
        if (teacher == null || teacher.getAccount() == null) {
            return null;
        }
        String lastName = teacher.getAccount().getLastName();
        String firstName = teacher.getAccount().getFirstName();
        String full = (lastName != null ? lastName : "") + (firstName != null ? " " + firstName : "");
        return full.trim();
    }
}
