package com.example.tlu_gpms_system.features.semester.service.impl;

import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.common.enums.SemesterStatus;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.semester.dto.request.SemesterRequest;
import com.example.tlu_gpms_system.features.semester.dto.response.SemesterResponse;
import com.example.tlu_gpms_system.features.semester.entity.Semester;
import com.example.tlu_gpms_system.features.semester.mapper.SemesterMapping;
import com.example.tlu_gpms_system.features.semester.repository.SemesterRepository;
import com.example.tlu_gpms_system.features.semester.service.SemesterService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final SemesterMapping semesterMapping;
    private final EntityManager entityManager;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<SemesterResponse> getAll(String keyword, String status, Pageable pageable) {
        SemesterStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            statusEnum = SemesterStatus.fromValue(status.trim());
        }

        Page<Semester> page = semesterRepository.findAllWithFilter(keyword, statusEnum, pageable);
        return PageResponse.from(page.map(semesterMapping::toResponse));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public SemesterResponse getByPublicId(UUID publicId) {
        Semester semester = semesterRepository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));
        return semesterMapping.toResponse(semester);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN')")
    public void create(SemesterRequest request) {
        // Ràng buộc: Ngày bắt đầu phải đứng trước ngày kết thúc
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_SEMESTER_DATES);
        }

        // Ràng buộc: Tên học kỳ không được trùng lặp
        String trimmedName = request.getName().trim();
        if (semesterRepository.existsByName(trimmedName)) {
            throw new AppException(ErrorCode.SEMESTER_NAME_EXIST);
        }

        Semester entity = semesterMapping.toEntity(request);
        entity.setName(trimmedName);

        // Trạng thái (status) tự động tính toán dựa trên ngày hiện tại so với startDate và endDate, hoặc theo request nếu có
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        } else {
            entity.setStatus(entity.calculateStatus());
        }

        semesterRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN')")
    public void update(UUID publicId, SemesterRequest request) {
        Semester entity = semesterRepository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        // Ràng buộc: Ngày bắt đầu phải đứng trước ngày kết thúc
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_SEMESTER_DATES);
        }

        // Ràng buộc: Tên học kỳ không trùng với học kỳ khác
        String trimmedName = request.getName().trim();
        if (semesterRepository.existsByNameAndPublicIdNot(trimmedName, publicId)) {
            throw new AppException(ErrorCode.SEMESTER_NAME_EXIST);
        }

        semesterMapping.toUpdate(entity, request);
        entity.setName(trimmedName);

        // Trạng thái (status) tự động tính toán hoặc cập nhật thủ công nếu có
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        } else {
            entity.setStatus(entity.calculateStatus());
        }

        semesterRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID publicId) {
        Semester entity = semesterRepository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));

        // Ràng buộc: Không được xóa nếu có dữ liệu liên quan
        if (hasRelatedData(entity)) {
            throw new AppException(ErrorCode.SEMESTER_HAS_RELATED_DATA);
        }

        try {
            semesterRepository.delete(entity);
            semesterRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.warn("Lỗi ràng buộc khóa ngoại khi xóa học kỳ: {}", e.getMessage());
            throw new AppException(ErrorCode.SEMESTER_HAS_RELATED_DATA);
        }
    }

    /**
     * Kiểm tra xem học kỳ có dữ liệu liên quan (đồ án, đề tài, đợt đăng ký, hội đồng,...) hay không
     */
    private boolean hasRelatedData(Semester semester) {
        if (semester.getId() == null) {
            return false;
        }

        String[] potentialTables = {
                "topic", "project", "defense_council", "registration_round",
                "schedule", "student_project", "teacher_semester"
        };

        for (String table : potentialTables) {
            try {
                Number tableExists = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = :tableName")
                        .setParameter("tableName", table)
                        .getSingleResult();

                if (tableExists != null && tableExists.intValue() > 0) {
                    Number colExists = (Number) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = :tableName AND column_name = 'semester_id'")
                            .setParameter("tableName", table)
                            .getSingleResult();

                    if (colExists != null && colExists.intValue() > 0) {
                        Number count = (Number) entityManager.createNativeQuery(
                                "SELECT COUNT(*) FROM " + table + " WHERE semester_id = :semesterId")
                                .setParameter("semesterId", semester.getId())
                                .getSingleResult();

                        if (count != null && count.longValue() > 0) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Kiểm tra bảng liên quan {} thất bại: {}", table, e.getMessage());
            }
        }
        return false;
    }
}
