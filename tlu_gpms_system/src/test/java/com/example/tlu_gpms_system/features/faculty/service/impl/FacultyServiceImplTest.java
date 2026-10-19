package com.example.tlu_gpms_system.features.faculty.service.impl;

import com.example.tlu_gpms_system.common.dto.PageResponse;
import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Role;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.FacultyUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorCreationRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyDetailResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.FacultyResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadCandidateResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.HeadOfDepartmentResponse;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import com.example.tlu_gpms_system.features.faculty.mapper.FacultyMapping;
import com.example.tlu_gpms_system.features.faculty.mapper.MajorMapping;
import com.example.tlu_gpms_system.features.faculty.repository.FacultyRepository;
import com.example.tlu_gpms_system.features.faculty.repository.MajorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacultyServiceImplTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FacultyMapping facultyMapping;

    @Mock
    private MajorMapping majorMapping;

    @InjectMocks
    private FacultyServiceImpl facultyService;

    private Faculty faculty;
    private Account headAccount;
    private UUID publicId;

    @BeforeEach
    void setUp() {
        publicId = UUID.randomUUID();

        Role headRole = new Role();
        headRole.setName("HEAD_OF_DEPARTMENT");
        headRole.setDescription("Trưởng bộ môn/khoa");

        headAccount = Account.builder()
                .id(10L)
                .email("head@tlu.edu.vn")
                .firstName("A")
                .lastName("Nguyễn Văn")
                .status(true)
                .roles(Set.of(headRole))
                .build();

        faculty = Faculty.builder()
                .id(1L)
                .publicId(publicId)
                .code("CNTT")
                .name("Công nghệ thông tin")
                .headOfDepartment(headAccount)
                .description("Khoa đào tạo CNTT hàng đầu")
                .status(ActiveStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("Tests for getAll")
    class GetAllTests {

        @Test
        @DisplayName("Lấy danh sách khoa kèm tổng số ngành thành công")
        void getAll_success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Faculty> page = new PageImpl<>(List.of(faculty), pageable, 1);

            when(facultyRepository.findAllWithFilter(any(), any(), eq(pageable))).thenReturn(page);
            List<Object[]> majorCounts = new ArrayList<>();
            majorCounts.add(new Object[]{1L, 4L});
            when(majorRepository.countMajorsByFacultyIds(List.of(1L))).thenReturn(majorCounts);

            FacultyResponse facultyResponse = FacultyResponse.builder()
                    .id(publicId)
                    .code("CNTT")
                    .name("Công nghệ thông tin")
                    .headOfDepartment(HeadOfDepartmentResponse.builder()
                            .id(10L)
                            .fullName("Nguyễn Văn A")
                            .build())
                    .status("Hoạt động")
                    .statusCode(ActiveStatus.ACTIVE)
                    .build();
            when(facultyMapping.toResponse(faculty)).thenReturn(facultyResponse);

            PageResponse<FacultyResponse> result = facultyService.getAll(null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTotalMajors()).isEqualTo(4L);
        }
    }

    @Nested
    @DisplayName("Tests for getHeadCandidates")
    class GetHeadCandidatesTests {

        @Test
        @DisplayName("Lấy danh sách ứng viên Trưởng khoa thành công")
        void getHeadCandidates_success() {
            when(accountRepository.findActiveHeadOfDepartmentCandidates()).thenReturn(List.of(headAccount));

            List<HeadCandidateResponse> result = facultyService.getHeadCandidates();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(10L);
            assertThat(result.get(0).getFullName()).isEqualTo("Nguyễn Văn A");
            assertThat(result.get(0).getEmail()).isEqualTo("head@tlu.edu.vn");
        }
    }

    @Nested
    @DisplayName("Tests for getById")
    class GetByIdTests {

        @Test
        @DisplayName("Tìm khoa bằng UUID thành công")
        void getById_withUUID_success() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.of(faculty));
            FacultyDetailResponse detailResponse = FacultyDetailResponse.builder()
                    .id(publicId)
                    .code("CNTT")
                    .name("Công nghệ thông tin")
                    .headOfDepartment(HeadOfDepartmentResponse.builder()
                            .id(10L)
                            .fullName("Nguyễn Văn A")
                            .build())
                    .description("Khoa đào tạo CNTT")
                    .status("Hoạt động")
                    .statusCode(ActiveStatus.ACTIVE)
                    .build();
            when(facultyMapping.toDetailResponse(faculty)).thenReturn(detailResponse);

            FacultyDetailResponse result = facultyService.getById(publicId.toString());

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("CNTT");
            assertThat(result.getHeadOfDepartment()).isNotNull();
        }

        @Test
        @DisplayName("Ném ngoại lệ FACULTY_NOT_FOUND khi không tìm thấy")
        void getById_notFound() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> facultyService.getById(publicId.toString()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACULTY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Tests for create")
    class CreateTests {

        @Test
        @DisplayName("Tạo khoa với trưởng khoa hợp lệ thành công")
        void create_withHeadOfDepartment_success() {
            FacultyCreationRequest request = FacultyCreationRequest.builder()
                    .code("KT")
                    .name("Kinh tế")
                    .headOfDepartmentId(10L)
                    .status(ActiveStatus.ACTIVE)
                    .build();

            when(facultyRepository.existsByCode("KT")).thenReturn(false);
            when(facultyRepository.existsByName("Kinh tế")).thenReturn(false);
            when(facultyMapping.toEntity(request)).thenReturn(new Faculty());
            when(accountRepository.findById(10L)).thenReturn(Optional.of(headAccount));

            facultyService.create(request);

            verify(facultyRepository).save(any(Faculty.class));
        }

        @Test
        @DisplayName("Ném ngoại lệ HEAD_OF_DEPARTMENT_NOT_FOUND khi tài khoản trưởng khoa không tồn tại")
        void create_headNotFound() {
            FacultyCreationRequest request = FacultyCreationRequest.builder()
                    .code("KT")
                    .name("Kinh tế")
                    .headOfDepartmentId(999L)
                    .build();

            when(facultyRepository.existsByCode("KT")).thenReturn(false);
            when(facultyRepository.existsByName("Kinh tế")).thenReturn(false);
            when(facultyMapping.toEntity(request)).thenReturn(new Faculty());
            when(accountRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> facultyService.create(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HEAD_OF_DEPARTMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("Ném ngoại lệ ACCOUNT_NOT_HEAD_OF_DEPARTMENT khi tài khoản không có role HEAD_OF_DEPARTMENT")
        void create_notHeadRole() {
            Role studentRole = new Role();
            studentRole.setName("STUDENT");

            Account nonHeadAccount = Account.builder()
                    .id(20L)
                    .status(true)
                    .roles(Set.of(studentRole))
                    .build();

            FacultyCreationRequest request = FacultyCreationRequest.builder()
                    .code("KT")
                    .name("Kinh tế")
                    .headOfDepartmentId(20L)
                    .build();

            when(facultyRepository.existsByCode("KT")).thenReturn(false);
            when(facultyRepository.existsByName("Kinh tế")).thenReturn(false);
            when(facultyMapping.toEntity(request)).thenReturn(new Faculty());
            when(accountRepository.findById(20L)).thenReturn(Optional.of(nonHeadAccount));

            assertThatThrownBy(() -> facultyService.create(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_HEAD_OF_DEPARTMENT);
        }

        @Test
        @DisplayName("Ném ngoại lệ FACULTY_CODE_EXIST khi mã khoa đã tồn tại")
        void create_codeExist() {
            FacultyCreationRequest request = FacultyCreationRequest.builder()
                    .code("CNTT")
                    .name("Khoa mới")
                    .build();

            when(facultyRepository.existsByCode("CNTT")).thenReturn(true);

            assertThatThrownBy(() -> facultyService.create(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACULTY_CODE_EXIST);
        }

        @Test
        @DisplayName("Ném ngoại lệ FACULTY_NAME_EXIST khi tên khoa đã tồn tại")
        void create_nameExist() {
            FacultyCreationRequest request = FacultyCreationRequest.builder()
                    .code("NEW")
                    .name("Công nghệ thông tin")
                    .build();

            when(facultyRepository.existsByCode("NEW")).thenReturn(false);
            when(facultyRepository.existsByName("Công nghệ thông tin")).thenReturn(true);

            assertThatThrownBy(() -> facultyService.create(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACULTY_NAME_EXIST);
        }
    }

    @Nested
    @DisplayName("Tests for update")
    class UpdateTests {

        @Test
        @DisplayName("Cập nhật thông tin khoa thành công")
        void update_success() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.of(faculty));
            when(facultyRepository.existsByCodeAndIdNot("CNTT_NEW", 1L)).thenReturn(false);
            when(facultyRepository.existsByNameAndIdNot("CNTT mới", 1L)).thenReturn(false);

            FacultyUpdateRequest request = FacultyUpdateRequest.builder()
                    .code("CNTT_NEW")
                    .name("CNTT mới")
                    .status(ActiveStatus.ACTIVE)
                    .build();

            facultyService.update(publicId.toString(), request);

            verify(facultyRepository).save(faculty);
        }

        @Test
        @DisplayName("Không thể vô hiệu hóa khoa khi vẫn còn ngành hoạt động")
        void update_deactivateFails_whenActiveMajorsExist() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.of(faculty));
            when(facultyRepository.existsByCodeAndIdNot("CNTT", 1L)).thenReturn(false);
            when(facultyRepository.existsByNameAndIdNot("Công nghệ thông tin", 1L)).thenReturn(false);
            when(majorRepository.existsByFacultyAndStatus(faculty, ActiveStatus.ACTIVE)).thenReturn(true);

            FacultyUpdateRequest request = FacultyUpdateRequest.builder()
                    .code("CNTT")
                    .name("Công nghệ thông tin")
                    .status(ActiveStatus.INACTIVE)
                    .build();

            assertThatThrownBy(() -> facultyService.update(publicId.toString(), request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACULTY_CANNOT_BE_DEACTIVATED);
        }
    }

    @Nested
    @DisplayName("Tests for delete")
    class DeleteTests {

        @Test
        @DisplayName("Xóa khoa thành công khi không có dữ liệu liên quan")
        void delete_success() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.of(faculty));
            when(majorRepository.countByFaculty(faculty)).thenReturn(0L);
            when(studentRepository.countByMajorFaculty(faculty)).thenReturn(0L);
            when(teacherRepository.countByMajorFaculty(faculty)).thenReturn(0L);

            facultyService.delete(publicId.toString());

            verify(facultyRepository).delete(faculty);
        }

        @Test
        @DisplayName("Không thể xóa khoa khi vẫn còn ngành")
        void delete_failsWhenMajorsExist() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.of(faculty));
            when(majorRepository.countByFaculty(faculty)).thenReturn(2L);

            assertThatThrownBy(() -> facultyService.delete(publicId.toString()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FACULTY_CANNOT_BE_DELETED);
        }
    }

    @Nested
    @DisplayName("Tests for addMajor")
    class AddMajorTests {

        @Test
        @DisplayName("Thêm mới ngành vào khoa thành công")
        void addMajor_success() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.of(faculty));
            when(majorRepository.existsByNameAndFaculty("Kỹ thuật phần mềm", faculty)).thenReturn(false);

            MajorCreationRequest request = MajorCreationRequest.builder()
                    .name("Kỹ thuật phần mềm")
                    .description("Lập trình phần mềm")
                    .status(ActiveStatus.ACTIVE)
                    .build();
            when(majorMapping.toEntity(request)).thenReturn(new Major());

            facultyService.addMajor(publicId.toString(), request);

            verify(majorRepository).save(any(Major.class));
        }

        @Test
        @DisplayName("Ném ngoại lệ khi tên ngành đã tồn tại trong cùng khoa")
        void addMajor_nameExistInFaculty() {
            when(facultyRepository.findByPublicId(publicId)).thenReturn(Optional.of(faculty));
            when(majorRepository.existsByNameAndFaculty("Kỹ thuật phần mềm", faculty)).thenReturn(true);

            MajorCreationRequest request = MajorCreationRequest.builder()
                    .name("Kỹ thuật phần mềm")
                    .build();

            assertThatThrownBy(() -> facultyService.addMajor(publicId.toString(), request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAJOR_NAME_EXIST_IN_FACULTY);
        }
    }
}
