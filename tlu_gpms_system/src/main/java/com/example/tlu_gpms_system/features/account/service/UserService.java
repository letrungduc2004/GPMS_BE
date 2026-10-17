package com.example.tlu_gpms_system.features.account.service;

import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    Page<StudentResponse> getAllStudents(String lastName, String batch, String majorName, Pageable pageable);
    void createStudents(List<StudentCreationRequest> requests);
    Page<TeacherResponse> getAllTeachers(String lastName, String organization, String majorName, Pageable pageable);
    void createTeachers(List<TeacherCreationRequest> requests);
}
