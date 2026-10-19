package com.example.tlu_gpms_system.features.faculty.service.impl;

import com.example.tlu_gpms_system.common.enums.ActiveStatus;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import com.example.tlu_gpms_system.features.faculty.dto.request.MajorUpdateRequest;
import com.example.tlu_gpms_system.features.faculty.dto.response.MajorResponse;
import com.example.tlu_gpms_system.features.faculty.entity.Faculty;
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import com.example.tlu_gpms_system.features.faculty.mapper.MajorMapping;
import com.example.tlu_gpms_system.features.faculty.repository.MajorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MajorServiceImplTest {

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private MajorMapping majorMapping;

    @InjectMocks
    private MajorServiceImpl majorService;

    private Faculty faculty;
    private Major major;
    private UUID majorPublicId;

    @BeforeEach
    void setUp() {
        faculty = Faculty.builder()
                .id(1L)
                .name("Công nghệ thông tin")
                .build();

        majorPublicId = UUID.randomUUID();
        major = Major.builder()
                .id(10L)
                .publicId(majorPublicId)
                .name("Kỹ thuật phần mềm")
                .status(ActiveStatus.ACTIVE)
                .faculty(faculty)
                .build();
    }

    @Nested
    @DisplayName("Tests for getById")
    class GetByIdTests {

        @Test
        @DisplayName("Tìm ngành bằng UUID thành công")
        void getById_success() {
            when(majorRepository.findByPublicId(majorPublicId)).thenReturn(Optional.of(major));
            MajorResponse response = MajorResponse.builder()
                    .id(majorPublicId)
                    .name("Kỹ thuật phần mềm")
                    .build();
            when(majorMapping.toResponse(major)).thenReturn(response);

            MajorResponse result = majorService.getById(majorPublicId.toString());

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Kỹ thuật phần mềm");
        }

        @Test
        @DisplayName("Ném ngoại lệ MAJOR_NOT_FOUND khi không tìm thấy")
        void getById_notFound() {
            when(majorRepository.findByPublicId(majorPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> majorService.getById(majorPublicId.toString()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAJOR_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Tests for update")
    class UpdateTests {

        @Test
        @DisplayName("Cập nhật ngành thành công")
        void update_success() {
            when(majorRepository.findByPublicId(majorPublicId)).thenReturn(Optional.of(major));
            when(majorRepository.existsByNameAndFacultyAndIdNot("KTPM Mới", faculty, 10L)).thenReturn(false);

            MajorUpdateRequest request = MajorUpdateRequest.builder()
                    .name("KTPM Mới")
                    .status(ActiveStatus.ACTIVE)
                    .build();

            majorService.update(majorPublicId.toString(), request);

            verify(majorRepository).save(major);
        }

        @Test
        @DisplayName("Ném ngoại lệ khi tên ngành đã tồn tại trong cùng khoa")
        void update_nameExistInFaculty() {
            when(majorRepository.findByPublicId(majorPublicId)).thenReturn(Optional.of(major));
            when(majorRepository.existsByNameAndFacultyAndIdNot("KTPM Đã Có", faculty, 10L)).thenReturn(true);

            MajorUpdateRequest request = MajorUpdateRequest.builder()
                    .name("KTPM Đã Có")
                    .status(ActiveStatus.ACTIVE)
                    .build();

            assertThatThrownBy(() -> majorService.update(majorPublicId.toString(), request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAJOR_NAME_EXIST_IN_FACULTY);
        }
    }

    @Nested
    @DisplayName("Tests for delete")
    class DeleteTests {

        @Test
        @DisplayName("Xóa ngành thành công khi không có sinh viên hoặc giảng viên")
        void delete_success() {
            when(majorRepository.findByPublicId(majorPublicId)).thenReturn(Optional.of(major));
            when(studentRepository.countByMajor(major)).thenReturn(0L);
            when(teacherRepository.countByMajor(major)).thenReturn(0L);

            majorService.delete(majorPublicId.toString());

            verify(majorRepository).delete(major);
        }

        @Test
        @DisplayName("Không thể xóa ngành khi vẫn còn sinh viên")
        void delete_failsWhenStudentsExist() {
            when(majorRepository.findByPublicId(majorPublicId)).thenReturn(Optional.of(major));
            when(studentRepository.countByMajor(major)).thenReturn(5L);

            assertThatThrownBy(() -> majorService.delete(majorPublicId.toString()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAJOR_CANNOT_BE_DELETED);
        }

        @Test
        @DisplayName("Không thể xóa ngành khi vẫn còn giảng viên")
        void delete_failsWhenTeachersExist() {
            when(majorRepository.findByPublicId(majorPublicId)).thenReturn(Optional.of(major));
            when(studentRepository.countByMajor(major)).thenReturn(0L);
            when(teacherRepository.countByMajor(major)).thenReturn(2L);

            assertThatThrownBy(() -> majorService.delete(majorPublicId.toString()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAJOR_CANNOT_BE_DELETED);
        }
    }
}
