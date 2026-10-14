package com.example.tlu_gpms_system.features.account.mapper;

import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StudentMapping {

    @Mapping(target = "majorName", source = "major.name")
    @Mapping(target = "name", source = "student", qualifiedByName = "concatFullName")
    StudentResponse toResponse(Student student);

    @Named("concatFullName")
    default String concatFullName(Student student) {
        if (student == null || student.getAccount() == null) {
            return null;
        }
        String lastName = student.getAccount().getLastName();
        String firstName = student.getAccount().getFirstName();
        String full = (lastName != null ? lastName : "") + (firstName != null ? " " + firstName : "");
        return full.trim();
    }
}
