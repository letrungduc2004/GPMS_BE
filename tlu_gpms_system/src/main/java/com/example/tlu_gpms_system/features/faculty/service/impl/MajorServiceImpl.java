package com.example.tlu_gpms_system.features.faculty.service.impl;

import com.example.tlu_gpms_system.common.enums.Role;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.account.service.AccountQueryService;
import com.example.tlu_gpms_system.features.account.service.AccountRoleService;
import com.example.tlu_gpms_system.features.account.service.helper.TeacherOrganization;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorHeadRequest;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepository;
    private final AccountQueryService accountQueryService;
    private final AccountRoleService accountRoleService;
    private final MajorMapping majorMapping;

    @Override
    @PreAuthorize("hasAuthority('VIEW_MAJOR')")
    public List<MajorResponse> getAllMajors() {
        return majorRepository.findAllMajors();
    }

    // Chỉ gọi nội bộ từ Service của feature khác (đã kiểm tra quyền ở đó), nên không đặt @PreAuthorize
    @Override
    public List<Major> getAllMajorEntities() {
        return majorRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_MAJOR')")
    public MajorResponse getById(String id) {
        Major major = findMajorByIdOrPublicId(id);
        return majorMapping.toResponse(major);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('UPDATE_MAJOR')")
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
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('DELETE_MAJOR')")
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAuthority('ASSIGN_HEAD_OF_MAJOR')")
    public void assignHead(String id, MajorHeadRequest request) {
        Major major = findMajorByIdOrPublicId(id);
        Long accountId = request.getAccountId();

        Account oldHead = major.getHead();
        if (oldHead != null && oldHead.getId().equals(accountId)) {
            return;
        }

        // Ràng buộc: Trưởng ngành là giáo viên trong trường, thuộc chính ngành này và tài khoản đang hoạt động
        if (accountQueryService.findAccountById(accountId).isEmpty()) {
            throw new AppException(ErrorCode.HEAD_OF_MAJOR_NOT_FOUND);
        }
        Teacher teacher = accountQueryService.findTeacherByAccountId(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_ELIGIBLE_HEAD_OF_MAJOR));
        Account account = teacher.getAccount();
        boolean eligible = Boolean.TRUE.equals(account.getStatus())
                && TeacherOrganization.TLU_NAME.equals(teacher.getOrganization())
                && teacher.getMajor() != null
                && teacher.getMajor().getId().equals(major.getId());
        if (!eligible) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ELIGIBLE_HEAD_OF_MAJOR);
        }

        // Ràng buộc: Mỗi tài khoản làm trưởng tối đa 1 ngành
        if (majorRepository.existsByHeadIdAndIdNot(accountId, major.getId())) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_HEAD_OF_OTHER_MAJOR);
        }

        if (oldHead != null) {
            accountRoleService.revokeRole(oldHead.getId(), Role.HEAD_OF_MAJOR);
        }
        accountRoleService.grantRole(accountId, Role.HEAD_OF_MAJOR);

        major.setHead(account);
        try {
            majorRepository.saveAndFlush(major);
        } catch (DataIntegrityViolationException e) {
            // Hai request gán cùng một người cho 2 ngành cùng lúc: unique head_account_id chặn request sau
            log.warn("Trùng head_account_id khi gán Trưởng ngành: {}", e.getMessage());
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_HEAD_OF_OTHER_MAJOR);
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
        return accountQueryService.existsStudentOrTeacherInMajor(major);
    }
}
