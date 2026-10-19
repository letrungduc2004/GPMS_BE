package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.features.account.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.account.repository.MajorRepository;
import com.example.tlu_gpms_system.features.account.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public List<MajorResponse> getAllMajors() {
        return majorRepository.findAllMajors();
    }
}
