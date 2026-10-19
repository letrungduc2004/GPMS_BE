package com.example.tlu_gpms_system.features.account.service;

import com.example.tlu_gpms_system.features.account.dto.response.MajorResponse;

import java.util.List;

public interface MajorService {
    // Danh sách ngành cho dropdown lọc (sắp xếp theo tên)
    List<MajorResponse> getAllMajors();
}
