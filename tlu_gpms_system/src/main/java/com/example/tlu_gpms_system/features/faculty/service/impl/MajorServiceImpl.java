package com.example.tlu_gpms_system.features.faculty.service.impl;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import com.example.tlu_gpms_system.features.faculty.mapper.MajorMapping;
import com.example.tlu_gpms_system.features.faculty.repository.MajorRepository;
import com.example.tlu_gpms_system.features.faculty.service.MajorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final MajorMapping majorMapping;

    @Override
    public MajorResponse getById(String id) {
        Major major = findMajorByIdOrPublicId(id);
        return majorMapping.toResponse(major);
    }

    @Override
    @Transactional
    public void update(String id, MajorUpdateRequest request) {
        Major major = findMajorByIdOrPublicId(id);

        String trimmedName = request.getName().trim();

        // Ràng buộc: Tên ngành không được trùng lặp trong cùng một khoa (trừ chính nó)
        if (majorRepository.existsByNameAndFacultyAndIdNot(trimmedName, major.getFaculty(), major.getId())) {
            throw new AppException(ErrorCode.MAJOR_NAME_EXIST_IN_FACULTY);
        }

        majorMapping.toUpdate(major, request);
        major.setName(trimmedName);
        if (request.getStatus() != null) {
            major.setStatus(request.getStatus());
        }

        majorRepository.save(major);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Major major = findMajorByIdOrPublicId(id);

        // Ràng buộc: Không thể xóa ngành nếu vẫn còn sinh viên hoặc giảng viên thuộc ngành này
        if (hasDataInMajor(major)) {
            throw new AppException(ErrorCode.MAJOR_CANNOT_BE_DELETED);
        }

        try {
            majorRepository.delete(major);
            majorRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.warn("Lỗi ràng buộc khóa ngoại khi xóa ngành: {}", e.getMessage());
            throw new AppException(ErrorCode.MAJOR_CANNOT_BE_DELETED);
        }
    }

    /**
     * Tìm ngành theo publicId (UUID) hoặc id (Long)
     */
    private Major findMajorByIdOrPublicId(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
        }
        try {
            UUID publicId = UUID.fromString(idStr.trim());
            return majorRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            try {
                Long id = Long.parseLong(idStr.trim());
                return majorRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));
            } catch (NumberFormatException nfe) {
                throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
            }
        }
    }

    /**
     * Kiểm tra xem ngành có dữ liệu liên quan (sinh viên hoặc giảng viên) không
     */
    private boolean hasDataInMajor(Major major) {
        // 1. Kiểm tra có sinh viên thuộc ngành này không
        if (studentRepository.countByMajor(major) > 0) {
            return true;
        }
        // 2. Kiểm tra có giảng viên thuộc ngành này không
        if (teacherRepository.countByMajor(major) > 0) {
            return true;
        }
        return false;
    }
}
