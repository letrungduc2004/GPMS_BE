package com.example.tlu_gpms_system.features.faculty.service;

import com.example.tlu_gpms_system.features.faculty.dto.request.MajorHeadRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Major;

import java.util.List;

public interface MajorService {

    List<MajorResponse> getAllMajors();

    // Dùng nội bộ cho feature khác (ví dụ account khi import sinh viên/giáo viên cần đối chiếu ngành)
    List<Major> getAllMajorEntities();

    MajorResponse getById(String id);

    void update(String id, MajorUpdateRequest request);

    void delete(String id);

    // Gán Trưởng ngành; người cũ (nếu có) bị gỡ role HEAD_OF_MAJOR
    void assignHead(String id, MajorHeadRequest request);
}
