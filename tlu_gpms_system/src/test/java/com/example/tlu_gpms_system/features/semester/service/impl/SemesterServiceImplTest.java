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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemesterServiceImplTest {

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private SemesterMapping semesterMapping;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SemesterServiceImpl semesterService;

    private Semester semester;
    private SemesterRequest request;
    private SemesterResponse response;
    private UUID publicId;

    @BeforeEach
    void setUp() {
        publicId = UUID.randomUUID();

        semester = Semester.builder()
                .id(1L)
                .publicId(publicId)
                .name("Học kỳ 1 - 2025")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .status(SemesterStatus.ONGOING)
                .build();

        request = SemesterRequest.builder()
                .name("Học kỳ 1 - 2025")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .build();

        response = SemesterResponse.builder()
                .id(publicId)
                .publicId(publicId)
                .name("Học kỳ 1 - 2025")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .status("Đang diễn ra")
                .statusCode(SemesterStatus.ONGOING)
                .build();
    }

    @Nested
    @DisplayName("Lấy danh sách học kỳ phân trang")
    class GetAllTests {

        @Test
        @DisplayName("Lấy danh sách thành công khi có filter")
        void getAll_whenValidFilter_shouldReturnPageResponse() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Semester> semesterPage = new PageImpl<>(List.of(semester), pageable, 1);

            when(semesterRepository.findAllWithFilter("2025", SemesterStatus.ONGOING, pageable))
                    .thenReturn(semesterPage);
            when(semesterMapping.toResponse(semester)).thenReturn(response);

            PageResponse<SemesterResponse> result = semesterService.getAll("2025", "ONGOING", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Học kỳ 1 - 2025");
        }

        @Test
        @DisplayName("Lấy danh sách khi status là ALL hoặc null thì truyền null status vào repository")
        void getAll_whenStatusIsAll_shouldPassNullStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Semester> semesterPage = new PageImpl<>(List.of(semester), pageable, 1);

            when(semesterRepository.findAllWithFilter(null, null, pageable))
                    .thenReturn(semesterPage);
            when(semesterMapping.toResponse(semester)).thenReturn(response);

            PageResponse<SemesterResponse> result = semesterService.getAll(null, "ALL", pageable);

            assertThat(result).isNotNull();
            verify(semesterRepository).findAllWithFilter(null, null, pageable);
        }
    }

    @Nested
    @DisplayName("Lấy chi tiết học kỳ theo publicId")
    class GetByPublicIdTests {

        @Test
        @DisplayName("Tìm thấy học kỳ thành công")
        void getByPublicId_whenExists_shouldReturnResponse() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.of(semester));
            when(semesterMapping.toResponse(semester)).thenReturn(response);

            SemesterResponse result = semesterService.getByPublicId(publicId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(publicId);
            assertThat(result.getName()).isEqualTo("Học kỳ 1 - 2025");
        }

        @Test
        @DisplayName("Throw SEMESTER_NOT_FOUND khi không tìm thấy")
        void getByPublicId_whenNotFound_shouldThrowException() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> semesterService.getByPublicId(publicId))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEMESTER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Thêm mới học kỳ")
    class CreateTests {

        @Test
        @DisplayName("Thêm mới thành công khi dữ liệu hợp lệ")
        void create_whenValidRequest_shouldSaveSemester() {
            when(semesterRepository.existsByName("Học kỳ 1 - 2025")).thenReturn(false);
            when(semesterMapping.toEntity(request)).thenReturn(semester);

            semesterService.create(request);

            verify(semesterRepository).save(semester);
        }

        @Test
        @DisplayName("Throw INVALID_SEMESTER_DATES khi startDate không đứng trước endDate")
        void create_whenStartDateAfterEndDate_shouldThrowException() {
            request.setStartDate(LocalDate.of(2026, 2, 1));
            request.setEndDate(LocalDate.of(2026, 1, 1));

            assertThatThrownBy(() -> semesterService.create(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEMESTER_DATES);

            verify(semesterRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw SEMESTER_NAME_EXIST khi tên học kỳ đã tồn tại")
        void create_whenDuplicateName_shouldThrowException() {
            when(semesterRepository.existsByName("Học kỳ 1 - 2025")).thenReturn(true);

            assertThatThrownBy(() -> semesterService.create(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEMESTER_NAME_EXIST);

            verify(semesterRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cập nhật học kỳ")
    class UpdateTests {

        @Test
        @DisplayName("Cập nhật thành công khi dữ liệu hợp lệ")
        void update_whenValidRequest_shouldUpdateSemester() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.of(semester));
            when(semesterRepository.existsByNameAndPublicIdNot("Học kỳ 1 - 2025", publicId)).thenReturn(false);

            semesterService.update(publicId, request);

            verify(semesterMapping).toUpdate(semester, request);
            verify(semesterRepository).save(semester);
        }

        @Test
        @DisplayName("Throw SEMESTER_NOT_FOUND khi cập nhật học kỳ không tồn tại")
        void update_whenNotFound_shouldThrowException() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> semesterService.update(publicId, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEMESTER_NOT_FOUND);
        }

        @Test
        @DisplayName("Throw INVALID_SEMESTER_DATES khi cập nhật startDate sau endDate")
        void update_whenStartDateAfterEndDate_shouldThrowException() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.of(semester));
            request.setStartDate(LocalDate.of(2026, 5, 1));
            request.setEndDate(LocalDate.of(2026, 1, 1));

            assertThatThrownBy(() -> semesterService.update(publicId, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SEMESTER_DATES);
        }

        @Test
        @DisplayName("Throw SEMESTER_NAME_EXIST khi đổi tên trùng với học kỳ khác")
        void update_whenDuplicateName_shouldThrowException() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.of(semester));
            when(semesterRepository.existsByNameAndPublicIdNot("Học kỳ 1 - 2025", publicId)).thenReturn(true);

            assertThatThrownBy(() -> semesterService.update(publicId, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEMESTER_NAME_EXIST);
        }
    }

    @Nested
    @DisplayName("Xóa học kỳ")
    class DeleteTests {

        @Test
        @DisplayName("Xóa thành công khi không có dữ liệu liên quan")
        void delete_whenValid_shouldDeleteSemester() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.of(semester));

            semesterService.delete(publicId);

            verify(semesterRepository).delete(semester);
            verify(semesterRepository).flush();
        }

        @Test
        @DisplayName("Throw SEMESTER_NOT_FOUND khi xóa học kỳ không tồn tại")
        void delete_whenNotFound_shouldThrowException() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> semesterService.delete(publicId))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEMESTER_NOT_FOUND);
        }

        @Test
        @DisplayName("Throw SEMESTER_HAS_RELATED_DATA khi bắt được DataIntegrityViolationException")
        void delete_whenDataIntegrityViolation_shouldThrowException() {
            when(semesterRepository.findByPublicId(publicId)).thenReturn(Optional.of(semester));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(semesterRepository).flush();

            assertThatThrownBy(() -> semesterService.delete(publicId))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEMESTER_HAS_RELATED_DATA);
        }
    }
}
