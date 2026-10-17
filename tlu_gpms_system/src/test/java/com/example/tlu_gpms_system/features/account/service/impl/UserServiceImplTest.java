package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Major;
import com.example.tlu_gpms_system.features.account.entity.Role;
import com.example.tlu_gpms_system.features.account.entity.Student;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.MajorRepository;
import com.example.tlu_gpms_system.features.account.repository.RoleRepository;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Lấy danh sách sinh viên phân trang thành công")
    void getAllStudents_success() {
        // Given
        String lastName = "Đức";
        String batch = "K63";
        String majorName = "Công nghệ thông tin";
        Pageable pageable = PageRequest.of(0, 10);

        StudentResponse student = StudentResponse.builder()
                .publicId(UUID.randomUUID())
                .studentCode("2151060001")
                .name("Lê Trung Đức")
                .majorName(majorName)
                .className("63TH1")
                .batch(batch)
                .build();

        Page<StudentResponse> expectedPage = new PageImpl<>(List.of(student), pageable, 1);
        when(studentRepository.findAllStudents(lastName, batch, majorName, pageable)).thenReturn(expectedPage);

        // When
        Page<StudentResponse> result = userService.getAllStudents(lastName, batch, majorName, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Lê Trung Đức");
        assertThat(result.getContent().get(0).getBatch()).isEqualTo("K63");
        verify(studentRepository).findAllStudents(lastName, batch, majorName, pageable);
    }

    @Test
    @DisplayName("Thêm sinh viên thành công với status=true và gán role STUDENT")
    void createStudents_success() {
        // Given
        StudentCreationRequest request = StudentCreationRequest.builder()
                .email("student@tlu.edu.vn")
                .firstName("Đức")
                .lastName("Lê Trung")
                .phone("0987654321")
                .studentCode("2151060001")
                .batch("K63")
                .className("63TH1")
                .majorId(1L)
                .build();

        Role studentRole = new Role();
        studentRole.setName("STUDENT");

        Major major = new Major();
        major.setId(1L);
        major.setName("Công nghệ thông tin");

        Account savedAccount = Account.builder()
                .id(10L)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(true)
                .build();

        when(roleRepository.findById("STUDENT")).thenReturn(Optional.of(studentRole));
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(studentRepository.existsByStudentCode(request.getStudentCode())).thenReturn(false);
        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // When
        userService.createStudents(List.of(request));

        // Then
        verify(accountRepository).save(argThat(account ->
                account.getStatus().equals(true) &&
                account.getEmail().equals("student@tlu.edu.vn") &&
                account.getRoles().contains(studentRole)
        ));
        verify(studentRepository).save(argThat(student ->
                student.getStatus().equals(true) &&
                student.getStudentCode().equals("2151060001") &&
                student.getClassName().equals("63TH1") &&
                student.getBatch().equals("K63")
        ));
    }

    @Test
    @DisplayName("Thêm sinh viên thất bại khi email đã tồn tại")
    void createStudents_emailExist_throwException() {
        // Given
        StudentCreationRequest request = StudentCreationRequest.builder()
                .email("existed@tlu.edu.vn")
                .studentCode("2151060001")
                .majorId(1L)
                .build();

        Role studentRole = new Role();
        studentRole.setName("STUDENT");

        when(roleRepository.findById("STUDENT")).thenReturn(Optional.of(studentRole));
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createStudents(List.of(request)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_EXIST);

        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách giáo viên phân trang thành công, sort status=true lên trước")
    void getAllTeachers_success() {
        // Given
        String lastName = "Nam";
        String organization = "Khoa CNTT";
        String majorName = "Công nghệ thông tin";
        Pageable pageable = PageRequest.of(0, 10);

        TeacherResponse teacher = TeacherResponse.builder()
                .publicId(UUID.randomUUID())
                .name("Trần Văn Nam")
                .majorName(majorName)
                .organization(organization)
                .phone("0912345678")
                .amount(5L)
                .status(true)
                .build();

        Page<TeacherResponse> expectedPage = new PageImpl<>(List.of(teacher), pageable, 1);
        when(teacherRepository.findAllTeachers(lastName, organization, majorName, pageable)).thenReturn(expectedPage);

        // When
        Page<TeacherResponse> result = userService.getAllTeachers(lastName, organization, majorName, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Trần Văn Nam");
        assertThat(result.getContent().get(0).getAmount()).isEqualTo(5L);
        assertThat(result.getContent().get(0).getStatus()).isTrue();
        verify(teacherRepository).findAllTeachers(lastName, organization, majorName, pageable);
    }

    @Test
    @DisplayName("Thêm giáo viên thành công với status=true và gán role SUPERVISOR")
    void createTeachers_success() {
        // Given
        TeacherCreationRequest request = TeacherCreationRequest.builder()
                .email("teacher@tlu.edu.vn")
                .firstName("Nam")
                .lastName("Trần Văn")
                .phone("0912345678")
                .organization("Khoa CNTT")
                .amount(5L)
                .majorId(1L)
                .build();

        Role supervisorRole = new Role();
        supervisorRole.setName("SUPERVISOR");

        Major major = new Major();
        major.setId(1L);
        major.setName("Công nghệ thông tin");

        Account savedAccount = Account.builder()
                .id(20L)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .status(true)
                .build();

        when(roleRepository.findById("SUPERVISOR")).thenReturn(Optional.of(supervisorRole));
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // When
        userService.createTeachers(List.of(request));

        // Then
        verify(accountRepository).save(argThat(account ->
                account.getStatus().equals(true) &&
                account.getEmail().equals("teacher@tlu.edu.vn") &&
                account.getRoles().contains(supervisorRole)
        ));
        verify(teacherRepository).save(argThat(teacher ->
                teacher.getOrganization().equals("Khoa CNTT") &&
                teacher.getAmount().equals(5L) &&
                teacher.getMajor().equals(major)
        ));
    }

    @Test
    @DisplayName("Thêm giáo viên thất bại khi email đã tồn tại")
    void createTeachers_emailExist_throwException() {
        // Given
        TeacherCreationRequest request = TeacherCreationRequest.builder()
                .email("teacher_exist@tlu.edu.vn")
                .organization("Khoa CNTT")
                .amount(5L)
                .majorId(1L)
                .build();

        Role supervisorRole = new Role();
        supervisorRole.setName("SUPERVISOR");

        when(roleRepository.findById("SUPERVISOR")).thenReturn(Optional.of(supervisorRole));
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createTeachers(List.of(request)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_EXIST);

        verify(teacherRepository, never()).save(any());
    }
}
