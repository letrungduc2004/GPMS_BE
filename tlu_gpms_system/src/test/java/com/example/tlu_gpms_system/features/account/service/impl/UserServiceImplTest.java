package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.common.enums.AccountStatusFilter;
import com.example.tlu_gpms_system.common.enums.RowImportStatus;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportRowResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportRowResult;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String[] HEADER = {"Mã sinh viên", "Họ", "Tên", "Email", "Số điện thoại", "Khóa", "Lớp", "Ngành"};

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

    @Nested
    @DisplayName("Danh sách sinh viên (GET /student)")
    class StudentListTests {

        private final UUID majorId = UUID.randomUUID();

        // Gọi service rồi lấy Pageable thực sự truyền xuống repository
        private Pageable capturedPageable(String keyword, String batch, UUID major, AccountStatusFilter status,
                                          Pageable pageable) {
            when(studentRepository.findAllStudents(any(), any(), any(), any(), any())).thenReturn(Page.empty());
            userService.getAllStudents(keyword, batch, major, status, pageable);
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(studentRepository).findAllStudents(any(), any(), any(), any(), captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("Chuẩn hóa tham số: keyword trim + gộp khoảng trắng, khóa viết hoa, majorId giữ nguyên, ACTIVE -> true")
        void normalizesFilters() {
            StudentResponse student = StudentResponse.builder()
                    .studentCode("2251172489").name("Lê Trung Đức").majorName("Kỹ thuật phần mềm")
                    .className("63KTPM1").status(true).build();
            when(studentRepository.findAllStudents(eq("Lê Trung Đức"), eq("K63"), eq(majorId),
                    eq(true), any())).thenReturn(new PageImpl<>(List.of(student)));

            Page<StudentResponse> result = userService.getAllStudents("  Lê   Trung Đức ", " k63 ", majorId,
                    AccountStatusFilter.ACTIVE, PageRequest.of(0, 15));

            assertThat(result.getContent()).singleElement()
                    .satisfies(s -> assertThat(s.getName()).isEqualTo("Lê Trung Đức"));
        }

        @Test
        @DisplayName("keyword, batch rỗng hoặc toàn khoảng trắng -> null (không lọc)")
        void blankFiltersBecomeNull() {
            capturedPageable("   ", "", null, AccountStatusFilter.ACTIVE, PageRequest.of(0, 15));

            verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), eq(true), any());
        }

        @Test
        @DisplayName("status: INACTIVE -> false, ALL -> null, không truyền -> mặc định ACTIVE (true)")
        void mapsStatusFilter() {
            when(studentRepository.findAllStudents(any(), any(), any(), any(), any())).thenReturn(Page.empty());

            userService.getAllStudents(null, null, null, AccountStatusFilter.INACTIVE, PageRequest.of(0, 15));
            userService.getAllStudents(null, null, null, AccountStatusFilter.ALL, PageRequest.of(0, 15));
            userService.getAllStudents(null, null, null, null, PageRequest.of(0, 15));

            verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), eq(false), any());
            verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), isNull(), any());
            verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), eq(true), any());
        }

        @Test
        @DisplayName("Không gửi sort -> đang hoạt động lên trước, mới thêm lên trước; giữ nguyên page/size")
        void defaultSort() {
            Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ALL, PageRequest.of(2, 15));

            assertThat(result.getPageNumber()).isEqualTo(2);
            assertThat(result.getPageSize()).isEqualTo(15);
            assertThat(result.getSort()).containsExactly(
                    Sort.Order.desc("a.status"), Sort.Order.desc("s.status"), Sort.Order.desc("s.id"));
        }

        @Test
        @DisplayName("sort=name,asc -> sắp theo Tên rồi Họ, chốt thêm s.id để phân trang ổn định")
        void sortByName() {
            Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ACTIVE,
                    PageRequest.of(0, 15, Sort.by("name")));

            assertThat(result.getSort()).containsExactly(
                    Sort.Order.asc("a.lastName"), Sort.Order.asc("a.firstName"), Sort.Order.desc("s.id"));
        }

        @Test
        @DisplayName("Nhiều sort: className,desc rồi studentCode,asc -> đổi đúng đường dẫn JPQL theo thứ tự")
        void sortByMultipleFields() {
            Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ACTIVE,
                    PageRequest.of(0, 15, Sort.by(Sort.Order.desc("className"), Sort.Order.asc("studentCode"))));

            assertThat(result.getSort()).containsExactly(
                    Sort.Order.desc("s.className"), Sort.Order.asc("s.studentCode"), Sort.Order.desc("s.id"));
        }

        @Test
        @DisplayName("sort=majorName,desc -> m.name")
        void sortByMajorName() {
            Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ACTIVE,
                    PageRequest.of(0, 15, Sort.by(Sort.Order.desc("majorName"))));

            assertThat(result.getSort()).containsExactly(Sort.Order.desc("m.name"), Sort.Order.desc("s.id"));
        }

        @Test
        @DisplayName("sort ngoài whitelist (id, password, a.status; email, batch chỉ có ở popup) -> INVALID_SORT_FIELD, không gọi DB")
        void rejectsUnknownSortField() {
            for (String field : List.of("id", "password", "a.status", "status", "email", "batch")) {
                assertThatThrownBy(() -> userService.getAllStudents(null, null, null, AccountStatusFilter.ACTIVE,
                        PageRequest.of(0, 15, Sort.by(field))))
                        .isInstanceOf(AppException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SORT_FIELD);
            }
            verifyNoInteractions(studentRepository);
        }
    }

    @Nested
    @DisplayName("Popup chi tiết sinh viên (GET /student/{publicId})")
    class StudentDetailTests {

        @Test
        @DisplayName("Tìm thấy -> trả đủ email, SĐT, khóa")
        void found() {
            UUID publicId = UUID.randomUUID();
            StudentDetailResponse detail = StudentDetailResponse.builder()
                    .publicId(publicId).studentCode("2251172489").name("Lê Trung Đức")
                    .email("2251172489@gmail.com").phone("0911111111").majorName("Kỹ thuật phần mềm")
                    .className("63KTPM1").batch("K63").status(true).build();
            when(studentRepository.findStudentDetail(publicId)).thenReturn(Optional.of(detail));

            StudentDetailResponse result = userService.getStudentDetail(publicId.toString());

            assertThat(result.getPhone()).isEqualTo("0911111111");
            assertThat(result.getEmail()).isEqualTo("2251172489@gmail.com");
            assertThat(result.getBatch()).isEqualTo("K63");
        }

        @Test
        @DisplayName("Không có publicId trong DB -> STUDENT_NOT_FOUND")
        void notFound() {
            UUID publicId = UUID.randomUUID();
            when(studentRepository.findStudentDetail(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getStudentDetail(publicId.toString()))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STUDENT_NOT_FOUND);
        }

        @Test
        @DisplayName("publicId sai định dạng UUID -> STUDENT_NOT_FOUND, không gọi DB")
        void invalidPublicId() {
            assertThatThrownBy(() -> userService.getStudentDetail("abc-123"))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STUDENT_NOT_FOUND);
            verifyNoInteractions(studentRepository);
        }
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

    // ===================== Thêm sinh viên (import Excel + nhập thủ công) =====================

    private Major major;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        major = Major.builder().id(1L).code("7480103").name("Kỹ thuật phần mềm").build();
        studentRole = new Role();
        studentRole.setName("STUDENT");
        lenient().when(majorRepository.findAll()).thenReturn(List.of(major));
        lenient().when(roleRepository.findById("STUDENT")).thenReturn(Optional.of(studentRole));
    }

    private StudentCreationRequest.StudentCreationRequestBuilder validRequest() {
        return StudentCreationRequest.builder()
                .studentCode("2251172489")
                .firstName("Lê Trung")
                .lastName("Đức")
                .email("duc@gmail.com")
                .phone("0987654321")
                .batch("K64")
                .className("64KTPM1")
                .major("Kỹ thuật phần mềm");
    }

    private Student existingStudent(boolean accountActive, boolean studentActive) {
        Account account = Account.builder()
                .id(10L).email("duc@gmail.com").firstName("Lê Trung").lastName("Đức")
                .phone("0911111111").status(accountActive).build();
        return Student.builder()
                .id(20L).studentCode("2251172489").batch("K64").className("63KTPM1")
                .status(studentActive).major(major).account(account).build();
    }

    // Khớp y hệt existingStudent (lớp 63KTPM1, SĐT 0911111111)
    private StudentCreationRequest.StudentCreationRequestBuilder sameAsExistingRequest() {
        return validRequest().className("63KTPM1").phone("0911111111");
    }

    private void assertResult(StudentCreationResult result, int code, String message) {
        assertThat(result.getCode()).isEqualTo(code);
        assertThat(result.getMessage()).isEqualTo(message);
    }

    private void givenExisting(Student student) {
        when(studentRepository.findAllByStudentCodeIn(anyCollection())).thenReturn(List.of(student));
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(student.getAccount()));
    }

    private void givenNothingInDb() {
        when(studentRepository.findAllByStudentCodeIn(anyCollection())).thenReturn(List.of());
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());
    }

    private void assertError(StudentCreationRequest request, ErrorCode expected) {
        assertThatThrownBy(() -> userService.createStudent(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", expected);
    }

    @SuppressWarnings("unchecked")
    private List<Account> capturedAccounts() {
        ArgumentCaptor<List<Account>> captor = ArgumentCaptor.forClass(List.class);
        verify(accountRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<Student> capturedStudents() {
        ArgumentCaptor<List<Student>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private static void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    @Nested
    @DisplayName("Nhập thủ công - Bước 1: kiểm tra định dạng")
    class ManualFormatTests {

        @Test
        @DisplayName("Có đúng 1 lỗi -> trả lỗi cụ thể đó, không truy vấn DB")
        void createStudent_whenOneError_shouldThrowSpecificError() {
            assertError(validRequest().phone("987654321").build(), ErrorCode.PHONE_INVALID);
            verify(studentRepository, never()).findAllByStudentCodeIn(any());
        }

        @Test
        @DisplayName("Có từ 2 lỗi trở lên -> trả thông báo chung")
        void createStudent_whenManyErrors_shouldThrowGenericError() {
            assertError(validRequest().phone("123").batch("64").build(), ErrorCode.STUDENT_INFO_INVALID);
        }

        @Test
        @DisplayName("Ô trống chỉ sinh 1 lỗi trống, không sinh thêm lỗi định dạng")
        void createStudent_whenEmpty_shouldOnlyReportEmpty() {
            assertError(validRequest().studentCode("   ").build(), ErrorCode.STUDENT_CODE_EMPTY);
        }

        @Test
        @DisplayName("Tên chứa số -> lỗi định dạng tên")
        void createStudent_whenLastNameHasDigit_shouldThrow() {
            assertError(validRequest().lastName("Đức2").build(), ErrorCode.LAST_NAME_INVALID);
        }

        @Test
        @DisplayName("Ngành không có trong DB -> lỗi danh mục")
        void createStudent_whenMajorUnknown_shouldThrow() {
            assertError(validRequest().major("Ngành không tồn tại").build(), ErrorCode.MAJOR_INVALID);
        }

        @Test
        @DisplayName("Email đúng định dạng nhưng không có đuôi @gmail.com -> lỗi đuôi email, không truy vấn DB")
        void createStudent_whenEmailNotGmail_shouldThrowDomainError() {
            for (String email : List.of("duc@e.tlu.edu.vn", "duc@tlu.edu.vn", "duc@sub.gmail.com", "duc@gmail.com.vn")) {
                assertError(validRequest().email(email).build(), ErrorCode.STUDENT_EMAIL_DOMAIN_INVALID);
            }
            verify(studentRepository, never()).findAllByStudentCodeIn(any());
        }

        @Test
        @DisplayName("Email sai định dạng -> chỉ báo lỗi định dạng, không báo thêm lỗi đuôi")
        void createStudent_whenEmailMalformed_shouldOnlyReportFormat() {
            assertError(validRequest().email("duc@gmail").build(), ErrorCode.EMAIL_INVALID);
        }
    }

    @Nested
    @DisplayName("Nhập thủ công - Bước 2: đối chiếu DB")
    class ManualReconcileTests {

        @Test
        @DisplayName("TH1: sinh viên mới -> tạo Account + Student, status=true, role STUDENT")
        void createStudent_whenNew_shouldCreate() {
            givenNothingInDb();

            StudentCreationResult result = userService.createStudent(validRequest().build());

            assertResult(result, 1000, "Thêm sinh viên thành công");
            Account account = capturedAccounts().get(0);
            assertThat(account.getStatus()).isTrue();
            assertThat(account.getRoles()).containsExactly(studentRole);
            Student student = capturedStudents().get(0);
            assertThat(student.getStatus()).isTrue();
            assertThat(student.getMajor()).isSameAs(major);
            assertThat(student.getAccount()).isSameAs(account);
        }

        @Test
        @DisplayName("TH1: dữ liệu được chuẩn hóa trước khi lưu, ngành tra theo mã không phân biệt hoa/thường")
        void createStudent_whenMessyInput_shouldNormalize() {
            givenNothingInDb();

            userService.createStudent(validRequest()
                    .firstName("  Lê​   Trung ")
                    .email(" DUC@GMAIL.COM ")
                    .batch("k64")
                    .major("7480103")
                    .build());

            Account account = capturedAccounts().get(0);
            assertThat(account.getFirstName()).isEqualTo("Lê Trung");
            assertThat(account.getEmail()).isEqualTo("duc@gmail.com");
            assertThat(capturedStudents().get(0).getBatch()).isEqualTo("K64");
        }

        @Test
        @DisplayName("TH2: email đã thuộc tài khoản khác -> 409")
        void createStudent_whenEmailBelongsToOther_shouldThrowConflict() {
            Account other = Account.builder().id(99L).email("duc@gmail.com").build();
            when(studentRepository.findAllByStudentCodeIn(anyCollection())).thenReturn(List.of());
            when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));

            assertError(validRequest().build(), ErrorCode.STUDENT_IDENTITY_CONFLICT);
            verify(accountRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("TH2: mã SV đã tồn tại nhưng email khác -> 409")
        void createStudent_whenCodeExistsWithOtherEmail_shouldThrowConflict() {
            when(studentRepository.findAllByStudentCodeIn(anyCollection()))
                    .thenReturn(List.of(existingStudent(false, false)));
            when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

            assertError(validRequest().email("khac@gmail.com").build(), ErrorCode.STUDENT_IDENTITY_CONFLICT);
        }

        @Test
        @DisplayName("TH3: khớp mã SV + email nhưng lệch khóa -> 409")
        void createStudent_whenBatchMismatch_shouldThrowMismatch() {
            givenExisting(existingStudent(false, false));

            assertError(validRequest().batch("K65").build(), ErrorCode.STUDENT_PROFILE_MISMATCH);
        }

        @Test
        @DisplayName("TH4: đang hoạt động, nhập y hệt thông tin cũ -> vàng 1101, không lưu lại")
        void createStudent_whenActiveAndSameData_shouldWarnNoChange() {
            givenExisting(existingStudent(true, true));

            StudentCreationResult result = userService.createStudent(sameAsExistingRequest().build());

            assertResult(result, 1101, ErrorCode.STUDENT_NO_CHANGE.getMessage());
            verify(accountRepository, never()).saveAll(any());
            verify(studentRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("TH4: lớp chỉ khác hoa/thường -> coi như không thay đổi")
        void createStudent_whenActiveAndClassDiffersOnlyInCase_shouldWarnNoChange() {
            givenExisting(existingStudent(true, true));

            StudentCreationResult result = userService.createStudent(sameAsExistingRequest().className("63ktpm1").build());

            assertResult(result, 1101, ErrorCode.STUDENT_NO_CHANGE.getMessage());
            verify(studentRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("TH4: đang hoạt động, đổi lớp -> cập nhật lớp, báo xanh liệt kê Lớp")
        void createStudent_whenActiveAndClassChanged_shouldUpdateClass() {
            Student student = existingStudent(true, true);
            givenExisting(student);

            StudentCreationResult result = userService.createStudent(sameAsExistingRequest().className("64KTPM1").build());

            assertResult(result, 1000, "Đã cập nhật: Lớp");
            assertThat(student.getClassName()).isEqualTo("64KTPM1");
            assertThat(student.getAccount().getPhone()).isEqualTo("0911111111");
            assertThat(capturedStudents()).containsExactly(student);
        }

        @Test
        @DisplayName("TH4: đang hoạt động, đổi cả lớp và SĐT -> liệt kê đủ 2 field")
        void createStudent_whenActiveAndClassAndPhoneChanged_shouldListBoth() {
            Student student = existingStudent(true, true);
            givenExisting(student);

            StudentCreationResult result = userService.createStudent(validRequest().build());

            assertResult(result, 1000, "Đã cập nhật: Lớp, Số điện thoại");
            assertThat(student.getClassName()).isEqualTo("64KTPM1");
            assertThat(student.getAccount().getPhone()).isEqualTo("0987654321");
            assertThat(capturedAccounts()).containsExactly(student.getAccount());
        }

        @Test
        @DisplayName("TH5: bị thu hồi từ kì trước, nhập y hệt -> kích hoạt lại")
        void createStudent_whenInactiveAndSameData_shouldReactivate() {
            Student student = existingStudent(false, false);
            givenExisting(student);

            StudentCreationResult result = userService.createStudent(sameAsExistingRequest().build());

            assertResult(result, 1000, "Đã kích hoạt lại tài khoản sinh viên");
            assertThat(student.getAccount().getStatus()).isTrue();
            assertThat(student.getStatus()).isTrue();
            assertThat(capturedStudents()).containsExactly(student);
        }

        @Test
        @DisplayName("TH5: bị thu hồi từ kì trước, đổi lớp và SĐT -> kích hoạt lại và cập nhật")
        void createStudent_whenInactiveAndChanged_shouldReactivateAndUpdate() {
            Student student = existingStudent(false, true);
            givenExisting(student);

            StudentCreationResult result = userService.createStudent(validRequest().build());

            assertResult(result, 1000, "Đã kích hoạt lại và cập nhật: Lớp, Số điện thoại");
            assertThat(student.getAccount().getStatus()).isTrue();
            assertThat(student.getStatus()).isTrue();
            assertThat(student.getAccount().getPhone()).isEqualTo("0987654321");
            assertThat(student.getClassName()).isEqualTo("64KTPM1");
            assertThat(capturedStudents()).containsExactly(student);
        }
    }

    @Nested
    @DisplayName("Kích hoạt từ danh sách (cách 3)")
    class ActivateTests {

        private final UUID publicId = UUID.randomUUID();

        @Test
        @DisplayName("Sinh viên đã tắt -> bật Account + Student, giữ nguyên lớp/SĐT")
        void activateStudent_whenInactive_shouldActivate() {
            Student student = existingStudent(false, false);
            when(studentRepository.findByPublicId(publicId)).thenReturn(Optional.of(student));

            boolean activated = userService.activateStudent(publicId.toString());

            assertThat(activated).isTrue();
            assertThat(student.getStatus()).isTrue();
            assertThat(student.getAccount().getStatus()).isTrue();
            assertThat(student.getClassName()).isEqualTo("63KTPM1");
            assertThat(student.getAccount().getPhone()).isEqualTo("0911111111");
            verify(accountRepository).save(student.getAccount());
            verify(studentRepository).save(student);
        }

        @Test
        @DisplayName("Chỉ Account bị tắt -> vẫn kích hoạt")
        void activateStudent_whenOnlyAccountInactive_shouldActivate() {
            Student student = existingStudent(false, true);
            when(studentRepository.findByPublicId(publicId)).thenReturn(Optional.of(student));

            assertThat(userService.activateStudent(publicId.toString())).isTrue();
            assertThat(student.getAccount().getStatus()).isTrue();
        }

        @Test
        @DisplayName("Đang hoạt động sẵn -> trả false, không lưu")
        void activateStudent_whenAlreadyActive_shouldReturnFalse() {
            when(studentRepository.findByPublicId(publicId)).thenReturn(Optional.of(existingStudent(true, true)));

            assertThat(userService.activateStudent(publicId.toString())).isFalse();
            verify(accountRepository, never()).save(any());
            verify(studentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Không tìm thấy publicId -> 404 STUDENT_NOT_FOUND")
        void activateStudent_whenNotFound_shouldThrow() {
            when(studentRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.activateStudent(publicId.toString()))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STUDENT_NOT_FOUND);
        }

        @Test
        @DisplayName("publicId sai định dạng UUID -> 404 STUDENT_NOT_FOUND, không query DB")
        void activateStudent_whenPublicIdMalformed_shouldThrowNotFound() {
            assertThatThrownBy(() -> userService.activateStudent("khong-phai-uuid"))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STUDENT_NOT_FOUND);
            verifyNoInteractions(studentRepository);
        }
    }

    @Nested
    @DisplayName("Import Excel")
    class ExcelTests {

        private MockMultipartFile excel(Consumer<Sheet> filler) throws IOException {
            try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet();
                Row header = sheet.createRow(0);
                for (int i = 0; i < HEADER.length; i++) {
                    header.createCell(i).setCellValue(HEADER[i]);
                }
                filler.accept(sheet);
                workbook.write(out);
                return new MockMultipartFile("file", "students.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
            }
        }

        private String[] validRow(String code, String email) {
            return new String[]{code, "Lê Trung", "Đức", email, "0987654321", "K64", "64KTPM1", "Kỹ thuật phần mềm"};
        }

        @Test
        @DisplayName("Lưu một phần: dòng hợp lệ được lưu, dòng trùng/lỗi báo đỏ, dòng trống bị bỏ qua")
        void importStudents_whenMixedRows_shouldSavePartially() throws IOException {
            givenNothingInDb();
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, validRow("2251172489", "a@gmail.com"));
                writeRow(sheet, 2, validRow("2251172518", "b@gmail.com"));
                writeRow(sheet, 3, validRow("2251172518", "c@gmail.com"));
                writeRow(sheet, 4, "", " ", "", "", "", "", "", "");
                writeRow(sheet, 5, "1951060660", "Nguyễn", "Hạnh", "sai-email", "0987654321", "K64", "64KTPM1", "Kỹ thuật phần mềm");
            });

            StudentImportResponse response = userService.importStudents(file);

            assertThat(response.getTotalRows()).isEqualTo(4);
            assertThat(response.getSuccessCount()).isEqualTo(1);
            assertThat(response.getErrorCount()).isEqualTo(3);
            List<StudentImportRowResult> results = response.getRowResults();
            assertThat(results).extracting(StudentImportRowResult::getRowNumber).containsExactly(2, 3, 4, 6);
            assertThat(results.get(0).getStatus()).isEqualTo(RowImportStatus.SUCCESS);
            assertThat(results.get(0).getMessage()).isEqualTo("Thêm sinh viên thành công");
            // Cả 2 dòng trùng mã SV đều bị gắn lỗi
            assertThat(results.get(1).getMessage()).isEqualTo(ErrorCode.STUDENT_CODE_DUPLICATED_IN_FILE.getMessage());
            assertThat(results.get(2).getMessage()).isEqualTo(ErrorCode.STUDENT_CODE_DUPLICATED_IN_FILE.getMessage());
            assertThat(results.get(3).getMessage()).isEqualTo(ErrorCode.EMAIL_INVALID.getMessage());
            assertThat(capturedStudents()).hasSize(1);
        }

        @Test
        @DisplayName("Sinh viên đang hoạt động: y hệt -> vàng 1101; đổi lớp -> xanh 'Đã cập nhật: Lớp' và lưu lớp mới (giống nhập thủ công)")
        void importStudents_whenActiveSameAndChanged_shouldBehaveLikeManual() throws IOException {
            Student duc = existingStudent(true, true);
            Account anAccount = Account.builder()
                    .id(11L).email("an@gmail.com").firstName("Nguyễn Văn").lastName("An")
                    .phone("0922222222").status(true).build();
            Student an = Student.builder()
                    .id(21L).studentCode("2251172518").batch("K64").className("63KTPM2")
                    .status(true).major(major).account(anAccount).build();
            when(studentRepository.findAllByStudentCodeIn(anyCollection())).thenReturn(List.of(duc, an));
            when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(duc.getAccount(), anAccount));
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, "2251172489", "Lê Trung", "Đức", "duc@gmail.com", "0911111111", "K64", "63KTPM1", "Kỹ thuật phần mềm");
                writeRow(sheet, 2, "2251172518", "Nguyễn Văn", "An", "an@gmail.com", "0922222222", "K64", "64KTPM1", "Kỹ thuật phần mềm");
            });

            StudentImportResponse response = userService.importStudents(file);

            List<StudentImportRowResult> results = response.getRowResults();
            assertThat(results.get(0).getStatus()).isEqualTo(RowImportStatus.WARNING);
            assertThat(results.get(0).getMessage()).isEqualTo(ErrorCode.STUDENT_NO_CHANGE.getMessage());
            assertThat(results.get(1).getStatus()).isEqualTo(RowImportStatus.SUCCESS);
            assertThat(results.get(1).getMessage()).isEqualTo("Đã cập nhật: Lớp");
            assertThat(capturedStudents()).containsExactly(an);
            assertThat(an.getClassName()).isEqualTo("64KTPM1");
            assertThat(duc.getClassName()).isEqualTo("63KTPM1");
        }

        @Test
        @DisplayName("Ô số giữ nguyên mã SV, ô lỗi công thức bị coi là sai định dạng")
        void importStudents_whenNumericAndFormulaErrorCells_shouldReadAsText() throws IOException {
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, validRow("", "a@gmail.com"));
                sheet.getRow(1).getCell(0).setCellValue(2251172489d);
                Cell email = sheet.getRow(1).getCell(3);
                email.setCellFormula("1/0");
                sheet.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(email);
            });

            StudentImportRowResult result = userService.importStudents(file).getRowResults().get(0);

            assertThat(result.getStudentCode()).isEqualTo("2251172489");
            assertThat(result.getStatus()).isEqualTo(RowImportStatus.ERROR);
            assertThat(result.getMessage()).isEqualTo(ErrorCode.EMAIL_INVALID.getMessage());
        }

        @Test
        @DisplayName("Sai tiêu đề -> từ chối cả file")
        void importStudents_whenHeaderWrong_shouldReject() throws IOException {
            MockMultipartFile file = excel(sheet -> {
                sheet.getRow(0).getCell(2).setCellValue("Tên gọi");
                writeRow(sheet, 1, validRow("2251172489", "a@gmail.com"));
            });

            assertThatThrownBy(() -> userService.importStudents(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_HEADER_INVALID);
        }

        @Test
        @DisplayName("Có ô bị gộp -> từ chối cả file")
        void importStudents_whenMergedCells_shouldReject() throws IOException {
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, validRow("2251172489", "a@gmail.com"));
                writeRow(sheet, 2, validRow("2251172518", "b@gmail.com"));
                sheet.addMergedRegion(new CellRangeAddress(1, 2, 6, 6));
            });

            assertThatThrownBy(() -> userService.importStudents(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_MERGED_CELLS);
        }

        @Test
        @DisplayName("Chỉ có tiêu đề -> từ chối cả file")
        void importStudents_whenNoData_shouldReject() throws IOException {
            MockMultipartFile file = excel(sheet -> { });

            assertThatThrownBy(() -> userService.importStudents(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_NO_DATA);
        }

        @Test
        @DisplayName("Đuôi .xlsx nhưng nội dung không phải Excel -> từ chối cả file")
        void importStudents_whenFakeXlsx_shouldReject() {
            MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "text/plain",
                    "không phải excel".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> userService.importStudents(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_INVALID_FORMAT);
        }
    }

    // ===================== Thêm giáo viên (import Excel + nhập thủ công) =====================

    private static final String[] TEACHER_HEADER = {"Họ và tên đệm", "Tên", "Email", "Số điện thoại", "Đơn vị công tác", "Ngành"};
    private static final String INTERNAL_EMAIL = "annv@tlu.edu.vn";
    private static final String EXTERNAL_EMAIL = "an.nguyen@gmail.com";

    private Role supervisorRole;

    @BeforeEach
    void setUpTeacher() {
        supervisorRole = new Role();
        supervisorRole.setName("SUPERVISOR");
        lenient().when(roleRepository.findById("SUPERVISOR")).thenReturn(Optional.of(supervisorRole));
    }

    private TeacherCreationRequest.TeacherCreationRequestBuilder validTeacherRequest() {
        return TeacherCreationRequest.builder()
                .firstName("Nguyễn Văn")
                .lastName("An")
                .email(INTERNAL_EMAIL)
                .phone("0987654321")
                .organization("Trong trường")
                .major("Kỹ thuật phần mềm");
    }

    // Giáo viên đã có trong DB với amount = 15 (khác mặc định) để kiểm tra ghi đè / giữ nguyên
    private Teacher existingTeacher(boolean active, Role... roles) {
        Account account = Account.builder()
                .id(30L).email(INTERNAL_EMAIL).firstName("Nguyễn Văn").lastName("An")
                .phone("0911111111").status(active).roles(new HashSet<>(Set.of(roles))).build();
        return Teacher.builder()
                .id(40L).organization("Trong trường").amount(15L).major(major).account(account).build();
    }

    private void givenExistingTeacher(Teacher teacher) {
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(teacher.getAccount()));
        when(teacherRepository.findAllByAccountIdIn(anyCollection())).thenReturn(List.of(teacher));
    }

    private void givenNoAccountInDb() {
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());
    }

    private void assertTeacherError(TeacherCreationRequest request, ErrorCode expected) {
        assertThatThrownBy(() -> userService.createTeacher(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", expected);
    }

    @SuppressWarnings("unchecked")
    private List<Teacher> capturedTeachers() {
        ArgumentCaptor<List<Teacher>> captor = ArgumentCaptor.forClass(List.class);
        verify(teacherRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("Giáo viên - Nhập thủ công - Bước 1: kiểm tra định dạng")
    class TeacherManualFormatTests {

        @Test
        @DisplayName("Có đúng 1 lỗi -> trả lỗi cụ thể đó, không truy vấn DB")
        void createTeacher_whenOneError_shouldThrowSpecificError() {
            assertTeacherError(validTeacherRequest().phone("987654321").build(), ErrorCode.PHONE_INVALID);
            verify(accountRepository, never()).findAllByEmailIn(any());
        }

        @Test
        @DisplayName("Có từ 2 lỗi trở lên -> trả thông báo chung")
        void createTeacher_whenManyErrors_shouldThrowGenericError() {
            assertTeacherError(validTeacherRequest().phone("123").lastName("An2").build(), ErrorCode.TEACHER_INFO_INVALID);
        }

        @Test
        @DisplayName("Email trống chỉ sinh 1 lỗi trống, không đối chiếu đơn vị công tác với email")
        void createTeacher_whenEmailEmpty_shouldOnlyReportEmpty() {
            assertTeacherError(validTeacherRequest().email("   ").build(), ErrorCode.EMAIL_EMPTY);
        }

        @Test
        @DisplayName("Email sai định dạng -> chỉ báo lỗi email, không báo lệch đơn vị công tác")
        void createTeacher_whenEmailInvalid_shouldNotCheckOrganization() {
            assertTeacherError(validTeacherRequest().email("sai-email").build(), ErrorCode.EMAIL_INVALID);
        }

        @Test
        @DisplayName("Đơn vị công tác trống -> lỗi trống")
        void createTeacher_whenOrganizationEmpty_shouldThrow() {
            assertTeacherError(validTeacherRequest().organization(null).build(), ErrorCode.ORGANIZATION_EMPTY);
        }

        @Test
        @DisplayName("Đơn vị công tác ngoài 2 giá trị cho phép -> lỗi định dạng")
        void createTeacher_whenOrganizationUnknown_shouldThrow() {
            assertTeacherError(validTeacherRequest().organization("Khoa CNTT").build(), ErrorCode.ORGANIZATION_INVALID);
        }

        @Test
        @DisplayName("Email ngoài trường nhưng chọn Trong trường -> lỗi không khớp email")
        void createTeacher_whenExternalEmailButInternalOrganization_shouldThrow() {
            assertTeacherError(validTeacherRequest().email(EXTERNAL_EMAIL).build(), ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL);
        }

        @Test
        @DisplayName("Email @tlu.edu.vn nhưng chọn Ngoài trường -> lỗi không khớp email")
        void createTeacher_whenInternalEmailButExternalOrganization_shouldThrow() {
            assertTeacherError(validTeacherRequest().organization("Ngoài trường").build(), ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL);
        }

        @Test
        @DisplayName("amount = 0 -> lỗi số lượng sinh viên hướng dẫn")
        void createTeacher_whenAmountZero_shouldThrow() {
            assertTeacherError(validTeacherRequest().amount(0L).build(), ErrorCode.AMOUNT_INVALID);
        }

        @Test
        @DisplayName("amount = 21 -> lỗi số lượng sinh viên hướng dẫn")
        void createTeacher_whenAmountOverMax_shouldThrow() {
            assertTeacherError(validTeacherRequest().amount(21L).build(), ErrorCode.AMOUNT_INVALID);
        }

        @Test
        @DisplayName("Ngành không có trong DB -> lỗi danh mục")
        void createTeacher_whenMajorUnknown_shouldThrow() {
            assertTeacherError(validTeacherRequest().major("Ngành không tồn tại").build(), ErrorCode.MAJOR_INVALID);
        }
    }

    @Nested
    @DisplayName("Giáo viên - Nhập thủ công - Bước 2: đối chiếu DB")
    class TeacherManualReconcileTests {

        @Test
        @DisplayName("TH1: giáo viên trong trường mới -> tạo Account + Teacher, status=true, role SUPERVISOR, amount mặc định 20")
        void createTeacher_whenNewInternal_shouldCreateWithDefaultAmount20() {
            givenNoAccountInDb();

            RowImportStatus status = userService.createTeacher(validTeacherRequest().build());

            assertThat(status).isEqualTo(RowImportStatus.SUCCESS);
            Account account = capturedAccounts().get(0);
            assertThat(account.getStatus()).isTrue();
            assertThat(account.getRoles()).containsExactly(supervisorRole);
            Teacher teacher = capturedTeachers().get(0);
            assertThat(teacher.getAmount()).isEqualTo(20L);
            assertThat(teacher.getOrganization()).isEqualTo("Trong trường");
            assertThat(teacher.getMajor()).isSameAs(major);
            assertThat(teacher.getAccount()).isSameAs(account);
        }

        @Test
        @DisplayName("TH1: giáo viên ngoài trường -> amount mặc định 10, đơn vị công tác được chuẩn hóa, email viết thường")
        void createTeacher_whenNewExternal_shouldCreateWithDefaultAmount10() {
            givenNoAccountInDb();

            userService.createTeacher(validTeacherRequest()
                    .email(" An.Nguyen@GMAIL.com ")
                    .organization("  ngoài TRƯỜNG ")
                    .build());

            assertThat(capturedAccounts().get(0).getEmail()).isEqualTo(EXTERNAL_EMAIL);
            Teacher teacher = capturedTeachers().get(0);
            assertThat(teacher.getAmount()).isEqualTo(10L);
            assertThat(teacher.getOrganization()).isEqualTo("Ngoài trường");
        }

        @Test
        @DisplayName("TH1: TBM tự nhập amount -> dùng giá trị nhập (giáo viên ngoài trường vẫn được tối đa 20)")
        void createTeacher_whenAmountProvided_shouldUseIt() {
            givenNoAccountInDb();

            userService.createTeacher(validTeacherRequest()
                    .email(EXTERNAL_EMAIL).organization("Ngoài trường").amount(20L).build());

            assertThat(capturedTeachers().get(0).getAmount()).isEqualTo(20L);
        }

        @Test
        @DisplayName("TH2: email đã thuộc tài khoản không phải giáo viên -> 409")
        void createTeacher_whenEmailBelongsToNonTeacher_shouldThrowConflict() {
            Account other = Account.builder().id(99L).email(INTERNAL_EMAIL).build();
            when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));
            when(teacherRepository.findAllByAccountIdIn(anyCollection())).thenReturn(List.of());

            assertTeacherError(validTeacherRequest().build(), ErrorCode.TEACHER_IDENTITY_CONFLICT);
            verify(accountRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("TH3: khớp email nhưng lệch họ tên -> 409")
        void createTeacher_whenNameMismatch_shouldThrowMismatch() {
            givenExistingTeacher(existingTeacher(false, supervisorRole));

            assertTeacherError(validTeacherRequest().lastName("Bình").build(), ErrorCode.TEACHER_PROFILE_MISMATCH);
        }

        @Test
        @DisplayName("TH4: đang hoạt động, kể cả nhập amount khác DB -> WARNING, không cập nhật gì")
        void createTeacher_whenAlreadyActive_shouldWarnWithoutUpdate() {
            Teacher teacher = existingTeacher(true, supervisorRole);
            givenExistingTeacher(teacher);

            RowImportStatus status = userService.createTeacher(validTeacherRequest().amount(5L).build());

            assertThat(status).isEqualTo(RowImportStatus.WARNING);
            assertThat(teacher.getAmount()).isEqualTo(15L);
            assertThat(teacher.getAccount().getPhone()).isEqualTo("0911111111");
            verify(accountRepository, never()).saveAll(any());
            verify(teacherRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("TH4: Trưởng bộ môn (có hồ sơ giáo viên, status=true) -> WARNING")
        void createTeacher_whenHeadOfDepartment_shouldWarn() {
            Role hodRole = new Role();
            hodRole.setName("HEAD_OF_DEPARTMENT");
            givenExistingTeacher(existingTeacher(true, hodRole));

            assertThat(userService.createTeacher(validTeacherRequest().build())).isEqualTo(RowImportStatus.WARNING);
        }

        @Test
        @DisplayName("TH5: bị thu hồi từ kì trước -> kích hoạt lại, ghi đè amount về mặc định, giữ nguyên role")
        void createTeacher_whenInactive_shouldReactivate() {
            Role examinerRole = new Role();
            examinerRole.setName("EXAMINER");
            Teacher teacher = existingTeacher(false, examinerRole);
            givenExistingTeacher(teacher);

            // Họ tên khác hoa/thường vẫn coi là khớp hồ sơ
            RowImportStatus status = userService.createTeacher(validTeacherRequest().firstName("NGUYỄN văn").build());

            assertThat(status).isEqualTo(RowImportStatus.SUCCESS);
            assertThat(teacher.getAccount().getStatus()).isTrue();
            assertThat(teacher.getAccount().getPhone()).isEqualTo("0987654321");
            assertThat(teacher.getAccount().getRoles()).containsExactly(examinerRole);
            assertThat(teacher.getAmount()).isEqualTo(20L);
            assertThat(capturedTeachers()).containsExactly(teacher);
        }

        @Test
        @DisplayName("Chưa khai báo role SUPERVISOR trong DB -> lỗi role không tồn tại")
        void createTeacher_whenSupervisorRoleMissing_shouldThrow() {
            givenNoAccountInDb();
            when(roleRepository.findById("SUPERVISOR")).thenReturn(Optional.empty());

            assertTeacherError(validTeacherRequest().build(), ErrorCode.ROLE_NOT_EXIST);
        }
    }

    @Nested
    @DisplayName("Giáo viên - Kích hoạt từ danh sách (cách 3)")
    class TeacherActivateTests {

        private final UUID publicId = UUID.randomUUID();

        @Test
        @DisplayName("Trong trường đã tắt -> bật Account, đặt lại amount = 20, giữ nguyên SĐT/đơn vị/ngành/role")
        void activateTeacher_whenInternalInactive_shouldActivateAndResetAmount() {
            Teacher teacher = existingTeacher(false, supervisorRole);
            when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

            boolean activated = userService.activateTeacher(publicId.toString());

            assertThat(activated).isTrue();
            assertThat(teacher.getAccount().getStatus()).isTrue();
            assertThat(teacher.getAmount()).isEqualTo(20L);
            assertThat(teacher.getAccount().getPhone()).isEqualTo("0911111111");
            assertThat(teacher.getOrganization()).isEqualTo("Trong trường");
            assertThat(teacher.getMajor()).isSameAs(major);
            assertThat(teacher.getAccount().getRoles()).containsExactly(supervisorRole);
            verify(accountRepository).save(teacher.getAccount());
            verify(teacherRepository).save(teacher);
        }

        @Test
        @DisplayName("Ngoài trường đã tắt -> đặt lại amount = 10")
        void activateTeacher_whenExternalInactive_shouldResetAmountToTen() {
            Teacher teacher = existingTeacher(false, supervisorRole);
            teacher.getAccount().setEmail(EXTERNAL_EMAIL);
            teacher.setOrganization("Ngoài trường");
            when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

            assertThat(userService.activateTeacher(publicId.toString())).isTrue();
            assertThat(teacher.getAmount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Trưởng bộ môn đã tắt -> kích hoạt như giáo viên thường, role giữ nguyên")
        void activateTeacher_whenHeadOfDepartmentInactive_shouldActivate() {
            Role hodRole = new Role();
            hodRole.setName("HEAD_OF_DEPARTMENT");
            Teacher teacher = existingTeacher(false, hodRole);
            when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

            assertThat(userService.activateTeacher(publicId.toString())).isTrue();
            assertThat(teacher.getAccount().getRoles()).containsExactly(hodRole);
        }

        @Test
        @DisplayName("Đang hoạt động sẵn -> trả false, không đổi amount, không lưu")
        void activateTeacher_whenAlreadyActive_shouldReturnFalse() {
            Teacher teacher = existingTeacher(true, supervisorRole);
            when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

            assertThat(userService.activateTeacher(publicId.toString())).isFalse();
            assertThat(teacher.getAmount()).isEqualTo(15L);
            verify(accountRepository, never()).save(any());
            verify(teacherRepository, never()).save(any());
        }

        @Test
        @DisplayName("Không tìm thấy publicId -> 404 TEACHER_NOT_FOUND")
        void activateTeacher_whenNotFound_shouldThrow() {
            when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.activateTeacher(publicId.toString()))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEACHER_NOT_FOUND);
        }

        @Test
        @DisplayName("publicId sai định dạng UUID -> 404 TEACHER_NOT_FOUND, không query DB")
        void activateTeacher_whenPublicIdMalformed_shouldThrowNotFound() {
            assertThatThrownBy(() -> userService.activateTeacher("khong-phai-uuid"))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEACHER_NOT_FOUND);
            verifyNoInteractions(teacherRepository);
        }
    }

    @Nested
    @DisplayName("Giáo viên - Import Excel")
    class TeacherExcelTests {

        private MockMultipartFile excel(Consumer<Sheet> filler) throws IOException {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet();
                Row header = sheet.createRow(0);
                for (int i = 0; i < TEACHER_HEADER.length; i++) {
                    header.createCell(i).setCellValue(TEACHER_HEADER[i]);
                }
                filler.accept(sheet);
                return toFile(workbook);
            }
        }

        private MockMultipartFile toFile(XSSFWorkbook workbook) throws IOException {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return new MockMultipartFile("file", "teachers.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
            }
        }

        private String[] internalRow(String email) {
            return new String[]{"Nguyễn Văn", "An", email, "0987654321", "Trong trường", "Kỹ thuật phần mềm"};
        }

        private String[] externalRow(String email) {
            return new String[]{"Trần Thị", "Bình", email, "0912345678", "Ngoài trường", "Kỹ thuật phần mềm"};
        }

        @Test
        @DisplayName("Lưu một phần: amount gán theo email, dòng trùng email báo đỏ, email sai không bị tính trùng, dòng trống bỏ qua")
        void importTeachers_whenMixedRows_shouldSavePartially() throws IOException {
            givenNoAccountInDb();
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
                writeRow(sheet, 2, externalRow(EXTERNAL_EMAIL));
                writeRow(sheet, 3, internalRow("trung@tlu.edu.vn"));
                writeRow(sheet, 4, internalRow("TRUNG@tlu.edu.vn"));
                writeRow(sheet, 5, "", " ", "", "", "", "");
                writeRow(sheet, 6, externalRow("sai-email"));
                writeRow(sheet, 7, externalRow("sai-email"));
            });

            TeacherImportResponse response = userService.importTeachers(file);

            assertThat(response.getTotalRows()).isEqualTo(6);
            assertThat(response.getSuccessCount()).isEqualTo(2);
            assertThat(response.getWarningCount()).isZero();
            assertThat(response.getErrorCount()).isEqualTo(4);
            List<TeacherImportRowResult> results = response.getRowResults();
            assertThat(results).extracting(TeacherImportRowResult::getRowNumber).containsExactly(2, 3, 4, 5, 7, 8);
            assertThat(results).extracting(TeacherImportRowResult::getAmount).containsExactly(20L, 10L, null, null, null, null);
            assertThat(results.get(0).getMessage()).isNull();
            assertThat(results.get(2).getMessage()).isEqualTo(ErrorCode.EMAIL_DUPLICATED_IN_FILE.getMessage());
            assertThat(results.get(3).getMessage()).isEqualTo(ErrorCode.EMAIL_DUPLICATED_IN_FILE.getMessage());
            assertThat(results.get(4).getMessage()).isEqualTo(ErrorCode.EMAIL_INVALID.getMessage());
            assertThat(results.get(5).getMessage()).isEqualTo(ErrorCode.EMAIL_INVALID.getMessage());
            assertThat(capturedTeachers()).extracting(Teacher::getAmount).containsExactly(20L, 10L);
        }

        @Test
        @DisplayName("Lệch đơn vị công tác với email, đơn vị gõ khác hoa/thường vẫn được chuẩn hóa")
        void importTeachers_whenOrganizationVariants_shouldValidateAndCanonicalize() throws IOException {
            givenNoAccountInDb();
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, "Nguyễn Văn", "An", INTERNAL_EMAIL, "0987654321", "trong TRƯỜNG", "7480103");
                writeRow(sheet, 2, "Trần Thị", "Bình", EXTERNAL_EMAIL, "0912345678", "Trong trường", "Kỹ thuật phần mềm");
                writeRow(sheet, 3, "Lê", "Cường", "cuong@tlu.edu.vn", "0912345679", "Khoa CNTT", "Kỹ thuật phần mềm");
            });

            List<TeacherImportRowResult> results = userService.importTeachers(file).getRowResults();

            assertThat(results.get(0).getStatus()).isEqualTo(RowImportStatus.SUCCESS);
            assertThat(results.get(0).getOrganization()).isEqualTo("Trong trường");
            assertThat(results.get(1).getMessage()).isEqualTo(ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL.getMessage());
            assertThat(results.get(2).getMessage()).isEqualTo(ErrorCode.ORGANIZATION_INVALID.getMessage());
            assertThat(capturedTeachers()).extracting(Teacher::getOrganization).containsExactly("Trong trường");
        }

        @Test
        @DisplayName("Giáo viên đang hoạt động -> dòng vàng, trả amount hiện tại trong DB")
        void importTeachers_whenAlreadyActive_shouldWarn() throws IOException {
            givenExistingTeacher(existingTeacher(true, supervisorRole));
            MockMultipartFile file = excel(sheet -> writeRow(sheet, 1, internalRow(INTERNAL_EMAIL)));

            TeacherImportResponse response = userService.importTeachers(file);

            assertThat(response.getWarningCount()).isEqualTo(1);
            TeacherImportRowResult result = response.getRowResults().get(0);
            assertThat(result.getStatus()).isEqualTo(RowImportStatus.WARNING);
            assertThat(result.getMessage()).isEqualTo(ErrorCode.TEACHER_ALREADY_ACTIVE.getMessage());
            assertThat(result.getAmount()).isEqualTo(15L);
            verify(teacherRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Ô lỗi công thức bị coi là sai định dạng")
        void importTeachers_whenFormulaErrorCell_shouldReadAsInvalid() throws IOException {
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, internalRow(""));
                Cell email = sheet.getRow(1).getCell(2);
                email.setCellFormula("1/0");
                sheet.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(email);
            });

            TeacherImportRowResult result = userService.importTeachers(file).getRowResults().get(0);

            assertThat(result.getStatus()).isEqualTo(RowImportStatus.ERROR);
            assertThat(result.getMessage()).isEqualTo(ErrorCode.EMAIL_INVALID.getMessage());
        }

        @Test
        @DisplayName("File mẫu teacher_import_template.xlsx đọc được: điền 1 dòng vào sheet đầu tiên -> thành công")
        void importTeachers_whenUsingTemplate_shouldAcceptHeader() throws IOException {
            givenNoAccountInDb();
            MockMultipartFile file;
            try (InputStream template = getClass().getResourceAsStream("/templates/teacher_import_template.xlsx");
                 XSSFWorkbook workbook = new XSSFWorkbook(template)) {
                writeRow(workbook.getSheetAt(0), 1, internalRow(INTERNAL_EMAIL));
                file = toFile(workbook);
            }

            TeacherImportResponse response = userService.importTeachers(file);

            assertThat(response.getSuccessCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Sai tiêu đề -> từ chối cả file")
        void importTeachers_whenHeaderWrong_shouldReject() throws IOException {
            MockMultipartFile file = excel(sheet -> {
                sheet.getRow(0).getCell(4).setCellValue("Đơn vị");
                writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
            });

            assertThatThrownBy(() -> userService.importTeachers(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_HEADER_INVALID);
        }

        @Test
        @DisplayName("Tự thêm cột số lượng sinh viên hướng dẫn -> từ chối cả file")
        void importTeachers_whenExtraAmountColumn_shouldReject() throws IOException {
            MockMultipartFile file = excel(sheet -> {
                sheet.getRow(0).createCell(6).setCellValue("Số lượng sinh viên");
                writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
            });

            assertThatThrownBy(() -> userService.importTeachers(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_HEADER_INVALID);
        }

        @Test
        @DisplayName("Có ô bị gộp -> từ chối cả file")
        void importTeachers_whenMergedCells_shouldReject() throws IOException {
            MockMultipartFile file = excel(sheet -> {
                writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
                writeRow(sheet, 2, externalRow(EXTERNAL_EMAIL));
                sheet.addMergedRegion(new CellRangeAddress(1, 2, 5, 5));
            });

            assertThatThrownBy(() -> userService.importTeachers(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_MERGED_CELLS);
        }

        @Test
        @DisplayName("Chỉ có tiêu đề -> từ chối cả file")
        void importTeachers_whenNoData_shouldReject() throws IOException {
            MockMultipartFile file = excel(sheet -> { });

            assertThatThrownBy(() -> userService.importTeachers(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_NO_DATA);
        }

        @Test
        @DisplayName("Đuôi .xlsx nhưng nội dung không phải Excel -> từ chối cả file")
        void importTeachers_whenFakeXlsx_shouldReject() {
            MockMultipartFile file = new MockMultipartFile("file", "teachers.xlsx", "text/plain",
                    "không phải excel".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> userService.importTeachers(file))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_INVALID_FORMAT);
        }
    }
}
