package com.example.tlu_gpms_system.features.faculty.service;

import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;

import java.util.List;

public interface MajorService {

    List<com.example.tlu_gpms_system.features.account.dto.response.MajorResponse> getAllMajors();

    MajorResponse getById(String id);

    void update(String id, MajorUpdateRequest request);

    void delete(String id);
}
