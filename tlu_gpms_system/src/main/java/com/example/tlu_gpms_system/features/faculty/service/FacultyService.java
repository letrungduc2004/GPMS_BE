package com.example.tlu_gpms_system.features.faculty.service;

import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyDetailResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadCandidateResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FacultyService {

    PageResponse<FacultyResponse> getAll(String keyword, String status, Pageable pageable);

    List<HeadCandidateResponse> getHeadCandidates();

    FacultyDetailResponse getById(String id);

    void create(FacultyCreationRequest request);

    void update(String id, FacultyUpdateRequest request);

    void delete(String id);

    PageResponse<MajorResponse> getMajorsByFaculty(String id, String keyword, String status, Pageable pageable);

    void addMajor(String id, MajorCreationRequest request);
}
