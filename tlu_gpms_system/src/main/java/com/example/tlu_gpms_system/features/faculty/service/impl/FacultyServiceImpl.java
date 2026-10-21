package com.example.tlu_gpms_system.features.faculty.service.impl;

import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyDetailResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadCandidateResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import com.example.tlu_gpms_system.features.faculty.mapper.FacultyMapping;
import com.example.tlu_gpms_system.features.faculty.mapper.MajorMapping;
import com.example.tlu_gpms_system.features.faculty.repository.FacultyRepository;
import com.example.tlu_gpms_system.features.faculty.repository.MajorRepository;
import com.example.tlu_gpms_system.features.faculty.service.FacultyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacultyServiceImpl implements FacultyService {

    private final FacultyRepository facultyRepository;
    private final MajorRepository majorRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AccountRepository accountRepository;
    private final FacultyMapping facultyMapping;
    private final MajorMapping majorMapping;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<FacultyResponse> getAll(String keyword, String status, Pageable pageable) {
        ActiveStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            statusEnum = ActiveStatus.fromValue(status.trim());
        }

        Page<Faculty> page = facultyRepository.findAllWithFilter(keyword, statusEnum, pageable);
        List<Faculty> faculties = page.getContent();

        if (faculties.isEmpty()) {
            return PageResponse.from(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }

        List<Long> facultyIds = faculties.stream().map(Faculty::getId).toList();
        List<Object[]> majorCounts = majorRepository.countMajorsByFacultyIds(facultyIds);
        Map<Long, Long> majorCountMap = majorCounts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<FacultyResponse> responses = faculties.stream().map(faculty -> {
            FacultyResponse response = facultyMapping.toResponse(faculty);
            response.setTotalMajors(majorCountMap.getOrDefault(faculty.getId(), 0L));
            return response;
        }).toList();

        Page<FacultyResponse> responsePage = new PageImpl<>(responses, pageable, page.getTotalElements());
        return PageResponse.from(responsePage);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<HeadCandidateResponse> getHeadCandidates() {
        List<Account> candidates = accountRepository.findActiveHeadOfDepartmentCandidates();
        return candidates.stream().map(a -> {
            String lastName = a.getLastName() != null ? a.getLastName().trim() : "";
            String firstName = a.getFirstName() != null ? a.getFirstName().trim() : "";
            String fullName = (lastName + " " + firstName).trim();
            if (fullName.isEmpty()) {
                fullName = a.getEmail();
            }
            return HeadCandidateResponse.builder()
                    .id(a.getId())
                    .fullName(fullName)
                    .email(a.getEmail())
                    .academicRank(null)
                    .build();
        }).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public FacultyDetailResponse getById(String id) {
        Faculty faculty = findFacultyByIdOrPublicId(id);
        return facultyMapping.toDetailResponse(faculty);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void create(FacultyCreationRequest request) {
        String trimmedCode = request.getCode().trim();
        String trimmedName = request.getName().trim();

        if (facultyRepository.existsByCode(trimmedCode)) {
            throw new AppException(ErrorCode.FACULTY_CODE_EXIST);
        }
        if (facultyRepository.existsByName(trimmedName)) {
            throw new AppException(ErrorCode.FACULTY_NAME_EXIST);
        }

        Faculty entity = facultyMapping.toEntity(request);
        entity.setCode(trimmedCode);
        entity.setName(trimmedName);
        if (entity.getStatus() == null) {
            entity.setStatus(ActiveStatus.ACTIVE);
        }

        entity.setHeadOfDepartment(resolveHeadOfDepartment(request.getHeadOfDepartmentId()));

        facultyRepository.save(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void update(String id, FacultyUpdateRequest request) {
        Faculty entity = findFacultyByIdOrPublicId(id);

        String trimmedCode = request.getCode().trim();
        String trimmedName = request.getName().trim();

        if (facultyRepository.existsByCodeAndIdNot(trimmedCode, entity.getId())) {
            throw new AppException(ErrorCode.FACULTY_CODE_EXIST);
        }
        if (facultyRepository.existsByNameAndIdNot(trimmedName, entity.getId())) {
            throw new AppException(ErrorCode.FACULTY_NAME_EXIST);
        }

        // Ràng buộc: Không thể vô hiệu hóa khoa nếu vẫn còn ngành hoặc sinh viên/giảng viên đang hoạt động trong khoa đó
        if (request.getStatus() == ActiveStatus.INACTIVE && entity.getStatus() == ActiveStatus.ACTIVE) {
            if (hasActiveDataInFaculty(entity)) {
                throw new AppException(ErrorCode.FACULTY_CANNOT_BE_DEACTIVATED);
            }
        }

        facultyMapping.toUpdate(entity, request);
        entity.setCode(trimmedCode);
        entity.setName(trimmedName);
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        if (request.getHeadOfDepartmentId() != null) {
            entity.setHeadOfDepartment(resolveHeadOfDepartment(request.getHeadOfDepartmentId()));
        }

        facultyRepository.save(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(String id) {
        Faculty entity = findFacultyByIdOrPublicId(id);

        // Ràng buộc: Không thể xóa khoa nếu vẫn còn ngành hoặc sinh viên/giảng viên đang hoạt động trong khoa đó
        if (hasDataInFaculty(entity)) {
            throw new AppException(ErrorCode.FACULTY_CANNOT_BE_DELETED);
        }

        try {
            facultyRepository.delete(entity);
            facultyRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.warn("Lỗi ràng buộc khóa ngoại khi xóa khoa: {}", e.getMessage());
            throw new AppException(ErrorCode.FACULTY_CANNOT_BE_DELETED);
        }
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<MajorResponse> getMajorsByFaculty(String id, String keyword, String status, Pageable pageable) {
        Faculty faculty = findFacultyByIdOrPublicId(id);

        ActiveStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            statusEnum = ActiveStatus.fromValue(status.trim());
        }

        Page<Major> page = majorRepository.findMajorsByFacultyWithFilter(faculty, keyword, statusEnum, pageable);
        return PageResponse.from(page.map(majorMapping::toResponse));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void addMajor(String id, MajorCreationRequest request) {
        Faculty faculty = findFacultyByIdOrPublicId(id);

        String trimmedName = request.getName().trim();
        // Ràng buộc: Tên ngành không được trùng lặp trong cùng một khoa
        if (majorRepository.existsByNameAndFaculty(trimmedName, faculty)) {
            throw new AppException(ErrorCode.MAJOR_NAME_EXIST_IN_FACULTY);
        }

        Major major = majorMapping.toEntity(request);
        major.setName(trimmedName);
        major.setFaculty(faculty);
        if (major.getStatus() == null) {
            major.setStatus(ActiveStatus.ACTIVE);
        }

        majorRepository.save(major);
    }

    /**
     * Xác thực và tìm tài khoản Trưởng khoa theo ID (có role HEAD_OF_DEPARTMENT và status = true)
     */
    private Account resolveHeadOfDepartment(Long headOfDepartmentId) {
        if (headOfDepartmentId == null) {
            return null;
        }

        Account account = accountRepository.findById(headOfDepartmentId)
                .orElseThrow(() -> new AppException(ErrorCode.HEAD_OF_DEPARTMENT_NOT_FOUND));

        if (account.getStatus() == null || !account.getStatus()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_HEAD_OF_DEPARTMENT);
        }

        boolean hasHeadRole = account.getRoles() != null && account.getRoles().stream()
                .anyMatch(r -> "HEAD_OF_DEPARTMENT".equalsIgnoreCase(r.getName()));

        if (!hasHeadRole) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_HEAD_OF_DEPARTMENT);
        }

        return account;
    }

    /**
     * Tìm khoa theo publicId (UUID) hoặc id (Long)
     */
    private Faculty findFacultyByIdOrPublicId(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            throw new AppException(ErrorCode.FACULTY_NOT_FOUND);
        }
        try {
            UUID publicId = UUID.fromString(idStr.trim());
            return facultyRepository.findByPublicId(publicId)
                    .orElseThrow(() -> new AppException(ErrorCode.FACULTY_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            try {
                Long id = Long.parseLong(idStr.trim());
                return facultyRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.FACULTY_NOT_FOUND));
            } catch (NumberFormatException nfe) {
                throw new AppException(ErrorCode.FACULTY_NOT_FOUND);
            }
        }
    }

    /**
     * Kiểm tra xem khoa có dữ liệu đang hoạt động (ngành hoạt động, sinh viên hoạt động, giảng viên hoạt động) không
     */
    private boolean hasActiveDataInFaculty(Faculty faculty) {
        // 1. Kiểm tra có ngành nào đang hoạt động
        if (majorRepository.existsByFacultyAndStatus(faculty, ActiveStatus.ACTIVE)) {
            return true;
        }
        // 2. Kiểm tra có sinh viên đang hoạt động
        if (studentRepository.countActiveStudentsByFaculty(faculty) > 0) {
            return true;
        }
        // 3. Kiểm tra có giảng viên đang hoạt động
        if (teacherRepository.countActiveTeachersByFaculty(faculty) > 0) {
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra xem khoa có dữ liệu liên quan (ngành, sinh viên, giảng viên) không
     */
    private boolean hasDataInFaculty(Faculty faculty) {
        // 1. Có bất kỳ ngành nào không
        if (majorRepository.countByFaculty(faculty) > 0) {
            return true;
        }
        // 2. Có bất kỳ sinh viên nào không
        if (studentRepository.countByMajorFaculty(faculty) > 0) {
            return true;
        }
        // 3. Có bất kỳ giảng viên nào không
        if (teacherRepository.countByMajorFaculty(faculty) > 0) {
            return true;
        }
        return false;
    }
}
