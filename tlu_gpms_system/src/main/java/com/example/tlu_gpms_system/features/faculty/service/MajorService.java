package com.example.tlu_gpms_system.features.faculty.service;

import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;

public interface MajorService {

    MajorResponse getById(String id);

    void update(String id, MajorUpdateRequest request);

    void delete(String id);
}
