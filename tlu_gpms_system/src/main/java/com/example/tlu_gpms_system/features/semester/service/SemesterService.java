package com.example.tlu_gpms_system.features.semester.service;

import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.features.semester.dto.request.SemesterRequest;
import com.example.tlu_gpms_system.features.semester.dto.response.SemesterResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SemesterService {

    PageResponse<SemesterResponse> getAll(String keyword, String status, Pageable pageable);

    SemesterResponse getByPublicId(UUID publicId);

    void create(SemesterRequest request);

    void update(UUID publicId, SemesterRequest request);

    void delete(UUID publicId);
}
