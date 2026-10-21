package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.common.enums.AccountStatusFilter;
import com.example.tlu_gpms_system.common.enums.RowImportStatus;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherAmountRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportRowResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportRowResult;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Role;
import com.example.tlu_gpms_system.features.account.entity.Student;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.RoleRepository;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import com.example.tlu_gpms_system.testreport.UnitTestCase;
import com.example.tlu_gpms_system.testreport.UnitTestCase.Type;
import com.example.tlu_gpms_system.testreport.UtcReportExtension;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith({MockitoExtension.class, UtcReportExtension.class})
class UserServiceImplTest {

    private static final String[] HEADER = {"Mã sinh viên", "Họ", "Tên", "Email", "Số điện thoại", "Khóa", "Lớp", "Ngành"};
    private static final String[] TEACHER_HEADER = {"Họ và tên đệm", "Tên", "Email", "Số điện thoại", "Đơn vị công tác", "Ngành"};
    private static final String INTERNAL_EMAIL = "annv@tlu.edu.vn";
    private static final String EXTERNAL_EMAIL = "an.nguyen@gmail.com";
    private static final String TLU_NAME = "Trường Đại học Thủy lợi";
    private static final String OTHER_SCHOOL = "Trường Đại học Bách khoa Hà Nội";

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

    private final UUID majorId = UUID.randomUUID();
    private final UUID publicId = UUID.randomUUID();

    private Major major;
    private Role studentRole;
    private Role supervisorRole;

    @BeforeEach
    void setUp() {
        major = Major.builder().id(1L).code("7480103").name("Kỹ thuật phần mềm").build();
        studentRole = new Role();
        studentRole.setName("STUDENT");
        supervisorRole = new Role();
        supervisorRole.setName("SUPERVISOR");
        lenient().when(majorRepository.findAll()).thenReturn(List.of(major));
        lenient().when(roleRepository.findById("STUDENT")).thenReturn(Optional.of(studentRole));
        lenient().when(roleRepository.findById("SUPERVISOR")).thenReturn(Optional.of(supervisorRole));
    }

    // ===================== Helper chung =====================

    private static void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private static MockMultipartFile toFile(XSSFWorkbook workbook, String fileName) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return new MockMultipartFile("file", fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }


    @SuppressWarnings("unchecked")
    private List<Account> capturedAccounts() {
        ArgumentCaptor<List<Account>> captor = ArgumentCaptor.forClass(List.class);
        verify(accountRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // ===================== Helper sinh viên =====================

    // Gọi service rồi lấy Pageable thực sự truyền xuống repository
    private Pageable capturedPageable(String keyword, String batch, UUID major, AccountStatusFilter status,
                                      Pageable pageable) {
        when(studentRepository.findAllStudents(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        userService.getAllStudents(keyword, batch, major, status, pageable);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(studentRepository).findAllStudents(any(), any(), any(), any(), captor.capture());
        return captor.getValue();
    }

    // Gọi service rồi lấy Pageable thực sự truyền xuống repository (danh sách giáo viên)
    private Pageable capturedTeacherPageable(String name, String organizationType, String majorName, Pageable pageable) {
        when(teacherRepository.findAllTeachers(any(), any(), any(), any())).thenReturn(Page.empty());
        userService.getAllTeachers(name, organizationType, majorName, pageable);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(teacherRepository).findAllTeachers(any(), any(), any(), captor.capture());
        return captor.getValue();
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
                .extracting("errorCode")
                .isEqualTo(expected);
    }

    // Lỗi ROW_INFO_INVALID (sai từ 2 trường trở lên): message liệt kê các trường sai
    private void assertInvalid(StudentCreationRequest request, String expectedMessage) {
        assertThatThrownBy(() -> userService.createStudent(request))
                .isInstanceOf(AppException.class)
                .hasMessage(expectedMessage)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROW_INFO_INVALID);
    }

    // Lỗi ACCOUNT_DUPLICATED: message liệt kê các trường bị trùng
    private void assertDuplicated(StudentCreationRequest request, String expectedMessage) {
        assertThatThrownBy(() -> userService.createStudent(request))
                .isInstanceOf(AppException.class)
                .hasMessage(expectedMessage)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_DUPLICATED);
    }

    @SuppressWarnings("unchecked")
    private List<Student> capturedStudents() {
        ArgumentCaptor<List<Student>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private MockMultipartFile buildStudentExcel(Consumer<Sheet> filler) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADER.length; i++) {
                header.createCell(i).setCellValue(HEADER[i]);
            }
            filler.accept(sheet);
            return toFile(workbook, "students.xlsx");
        }
    }

    private String[] validRow(String code, String email) {
        return validRow(code, email, "0987654321");
    }

    // Mỗi SĐT chỉ thuộc 1 tài khoản: file nhiều dòng hợp lệ phải dùng SĐT khác nhau
    private String[] validRow(String code, String email, String phone) {
        return new String[]{code, "Lê Trung", "Đức", email, phone, "K64", "64KTPM1", "Kỹ thuật phần mềm"};
    }

    // ===================== Helper giáo viên =====================

    private TeacherCreationRequest.TeacherCreationRequestBuilder validTeacherRequest() {
        return TeacherCreationRequest.builder()
                .firstName("Nguyễn Văn")
                .lastName("An")
                .email(INTERNAL_EMAIL)
                .phone("0987654321")
                .organization(TLU_NAME)
                .major("Kỹ thuật phần mềm");
    }

    // Giáo viên đã có trong DB với amount = 15 (khác mặc định) để kiểm tra ghi đè / giữ nguyên
    private Teacher existingTeacher(boolean active, Role... roles) {
        Account account = Account.builder()
                .id(30L).email(INTERNAL_EMAIL).firstName("Nguyễn Văn").lastName("An")
                .phone("0911111111").status(active).roles(new HashSet<>(Set.of(roles))).build();
        return Teacher.builder()
                .id(40L).organization(TLU_NAME).amount(15L).major(major).account(account).build();
    }

    private void assertUpdateAmountError(String id, String amount, ErrorCode expected) {
        TeacherAmountRequest request = TeacherAmountRequest.builder().amount(amount).build();
        assertThatThrownBy(() -> userService.updateTeacherAmount(id, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }

    private void givenExistingTeacher(Teacher teacher) {
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(teacher.getAccount()));
        when(teacherRepository.findAllByAccountIdIn(anyCollection())).thenReturn(List.of(teacher));
    }

    private void givenNoAccountInDb() {
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());
    }

    private void assertTeacherResult(TeacherCreationResult result, int code, String message) {
        assertThat(result.getCode()).isEqualTo(code);
        assertThat(result.getMessage()).isEqualTo(message);
    }

    private void assertTeacherError(TeacherCreationRequest request, ErrorCode expected) {
        assertThatThrownBy(() -> userService.createTeacher(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }

    // Lỗi ROW_INFO_INVALID (sai từ 2 trường trở lên): message liệt kê các trường sai
    private void assertTeacherInvalid(TeacherCreationRequest request, String expectedMessage) {
        assertThatThrownBy(() -> userService.createTeacher(request))
                .isInstanceOf(AppException.class)
                .hasMessage(expectedMessage)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROW_INFO_INVALID);
    }

    // Lỗi ACCOUNT_DUPLICATED: message liệt kê các trường bị trùng
    private void assertTeacherDuplicated(TeacherCreationRequest request, String expectedMessage) {
        assertThatThrownBy(() -> userService.createTeacher(request))
                .isInstanceOf(AppException.class)
                .hasMessage(expectedMessage)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_DUPLICATED);
    }

    @SuppressWarnings("unchecked")
    private List<Teacher> capturedTeachers() {
        ArgumentCaptor<List<Teacher>> captor = ArgumentCaptor.forClass(List.class);
        verify(teacherRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private MockMultipartFile buildTeacherExcel(Consumer<Sheet> filler) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row header = sheet.createRow(0);
            for (int i = 0; i < TEACHER_HEADER.length; i++) {
                header.createCell(i).setCellValue(TEACHER_HEADER[i]);
            }
            filler.accept(sheet);
            return toFile(workbook, "teachers.xlsx");
        }
    }

    private String[] internalRow(String email) {
        return internalRow(email, "0987654321");
    }

    // Mỗi SĐT chỉ thuộc 1 tài khoản: file nhiều dòng hợp lệ phải dùng SĐT khác nhau
    private String[] internalRow(String email, String phone) {
        return new String[]{"Nguyễn Văn", "An", email, phone, TLU_NAME, "Kỹ thuật phần mềm"};
    }

    // Giáo viên ngoài trường: không điền ngành
    private String[] externalRow(String email) {
        return externalRow(email, "0912345678");
    }

    private String[] externalRow(String email, String phone) {
        return new String[]{"Trần Thị", "Bình", email, phone, OTHER_SCHOOL, ""};
    }

    // ===== getAllStudents =====

    @Test
    @UnitTestCase(id = "UTC001", method = "getAllStudents", type = Type.NORMAL,
            precondition = "studentRepository.findAllStudents(\"Lê Trung Đức\", \"K63\", majorId, true, pageable) trả về 1 sinh viên",
            input = "keyword = \"  Lê   Trung Đức \"; batch = \" k63 \"; majorId = <uuid ngành>; status = ACTIVE; pageable = page 0, size 15",
            returns = "Page có 1 sinh viên tên \"Lê Trung Đức\" (keyword được trim + gộp khoảng trắng, khóa viết hoa, ACTIVE -> true)")
    void UTC001_getAllStudents_messyFilters_normalizedBeforeQuery() {
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
    @UnitTestCase(id = "UTC002", method = "getAllStudents", type = Type.BOUNDARY,
            precondition = "studentRepository.findAllStudents trả về trang rỗng",
            input = "keyword = \"   \"; batch = \"\"; majorId = null; status = ACTIVE; pageable = page 0, size 15",
            returns = "Gọi studentRepository.findAllStudents(null, null, null, true, pageable) - chuỗi rỗng/toàn khoảng trắng không lọc")
    void UTC002_getAllStudents_blankFilters_passNullToRepository() {
        capturedPageable("   ", "", null, AccountStatusFilter.ACTIVE, PageRequest.of(0, 15));

        verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), eq(true), any());
    }

    @Test
    @UnitTestCase(id = "UTC003", method = "getAllStudents", type = Type.NORMAL,
            precondition = "studentRepository.findAllStudents trả về trang rỗng",
            input = "Gọi 3 lần với status = INACTIVE; status = ALL; status = null",
            returns = "Tham số status truyền xuống repository lần lượt: false; null; true (mặc định ACTIVE)")
    void UTC003_getAllStudents_statusFilters_mappedToBoolean() {
        when(studentRepository.findAllStudents(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        userService.getAllStudents(null, null, null, AccountStatusFilter.INACTIVE, PageRequest.of(0, 15));
        userService.getAllStudents(null, null, null, AccountStatusFilter.ALL, PageRequest.of(0, 15));
        userService.getAllStudents(null, null, null, null, PageRequest.of(0, 15));

        verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), eq(false), any());
        verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), isNull(), any());
        verify(studentRepository).findAllStudents(isNull(), isNull(), isNull(), eq(true), any());
    }

    @Test
    @UnitTestCase(id = "UTC004", method = "getAllStudents", type = Type.NORMAL,
            precondition = "studentRepository.findAllStudents trả về trang rỗng",
            input = "status = ALL; pageable = page 2, size 15, không có sort",
            returns = "Pageable truyền xuống: page 2, size 15, sort a.status DESC, s.status DESC, s.id DESC")
    void UTC004_getAllStudents_noSort_defaultActiveFirstNewestFirst() {
        Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ALL, PageRequest.of(2, 15));

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(15);
        assertThat(result.getSort()).containsExactly(
                Sort.Order.desc("a.status"), Sort.Order.desc("s.status"), Sort.Order.desc("s.id"));
    }

    @Test
    @UnitTestCase(id = "UTC005", method = "getAllStudents", type = Type.NORMAL,
            precondition = "studentRepository.findAllStudents trả về trang rỗng",
            input = "status = ACTIVE; pageable = page 0, size 15, sort = name ASC",
            returns = "Sort truyền xuống: a.lastName ASC, a.firstName ASC, s.id DESC")
    void UTC005_getAllStudents_sortByName_sortLastNameThenFirstName() {
        Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ACTIVE,
                PageRequest.of(0, 15, Sort.by("name")));

        assertThat(result.getSort()).containsExactly(
                Sort.Order.asc("a.lastName"), Sort.Order.asc("a.firstName"), Sort.Order.desc("s.id"));
    }

    @Test
    @UnitTestCase(id = "UTC006", method = "getAllStudents", type = Type.NORMAL,
            precondition = "studentRepository.findAllStudents trả về trang rỗng",
            input = "status = ACTIVE; pageable = page 0, size 15, sort = className DESC, studentCode ASC",
            returns = "Sort truyền xuống: s.className DESC, s.studentCode ASC, s.id DESC")
    void UTC006_getAllStudents_multipleSortFields_mappedInOrder() {
        Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ACTIVE,
                PageRequest.of(0, 15, Sort.by(Sort.Order.desc("className"), Sort.Order.asc("studentCode"))));

        assertThat(result.getSort()).containsExactly(
                Sort.Order.desc("s.className"), Sort.Order.asc("s.studentCode"), Sort.Order.desc("s.id"));
    }

    @Test
    @UnitTestCase(id = "UTC007", method = "getAllStudents", type = Type.NORMAL,
            precondition = "studentRepository.findAllStudents trả về trang rỗng",
            input = "status = ACTIVE; pageable = page 0, size 15, sort = majorName DESC",
            returns = "Sort truyền xuống: m.name DESC, s.id DESC")
    void UTC007_getAllStudents_sortByMajorName_mappedToMajorTable() {
        Pageable result = capturedPageable(null, null, null, AccountStatusFilter.ACTIVE,
                PageRequest.of(0, 15, Sort.by(Sort.Order.desc("majorName"))));

        assertThat(result.getSort()).containsExactly(Sort.Order.desc("m.name"), Sort.Order.desc("s.id"));
    }

    @Test
    @UnitTestCase(id = "UTC008", method = "getAllStudents", type = Type.ABNORMAL,
            input = "status = ACTIVE; sort lần lượt = id, password, a.status, status, email, batch",
            exception = "AppException: INVALID_SORT_FIELD (\"Trường sắp xếp không hợp lệ\"); không gọi studentRepository")
    void UTC008_getAllStudents_sortFieldNotWhitelisted_throwAppException() {
        for (String field : List.of("id", "password", "a.status", "status", "email", "batch")) {
            assertThatThrownBy(() -> userService.getAllStudents(null, null, null, AccountStatusFilter.ACTIVE,
                    PageRequest.of(0, 15, Sort.by(field))))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_SORT_FIELD);
        }
        verifyNoInteractions(studentRepository);
    }

    // ===== getStudentDetail =====

    @Test
    @UnitTestCase(id = "UTC009", method = "getStudentDetail", type = Type.NORMAL,
            precondition = "studentRepository.findStudentDetail(publicId) trả về sinh viên 2251172489",
            input = "publicId = <uuid sinh viên>",
            returns = "StudentDetailResponse có email = \"2251172489@gmail.com\", phone = \"0911111111\", batch = \"K63\"")
    void UTC009_getStudentDetail_found_returnFullDetail() {
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
    @UnitTestCase(id = "UTC010", method = "getStudentDetail", type = Type.ABNORMAL,
            precondition = "studentRepository.findStudentDetail(publicId) trả về Optional.empty()",
            input = "publicId = <uuid không có trong DB>",
            exception = "AppException: STUDENT_NOT_FOUND (\"Sinh viên không tồn tại\")")
    void UTC010_getStudentDetail_notFound_throwAppException() {
        when(studentRepository.findStudentDetail(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getStudentDetail(publicId.toString()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
    }

    @Test
    @UnitTestCase(id = "UTC011", method = "getStudentDetail", type = Type.ABNORMAL,
            input = "publicId = \"abc-123\"",
            exception = "AppException: STUDENT_NOT_FOUND (\"Sinh viên không tồn tại\"); không gọi studentRepository")
    void UTC011_getStudentDetail_malformedPublicId_throwAppException() {
        assertThatThrownBy(() -> userService.getStudentDetail("abc-123"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
        verifyNoInteractions(studentRepository);
    }

    // ===== getAllTeachers =====

    @Test
    @UnitTestCase(id = "UTC012", method = "getAllTeachers", type = Type.NORMAL,
            precondition = "teacherRepository.findAllTeachers(\"Trần Văn Nam\", true, \"Công nghệ thông tin\", pageable) trả về 1 giáo viên",
            input = "name = \"  Trần   Văn Nam \"; organizationType = \" Internal\"; majorName = \"Công nghệ thông tin \"; pageable = page 0, size 10",
            returns = "Page có 1 giáo viên: name = \"Trần Văn Nam\", amount = 5, status = true (bộ lọc được trim + gộp khoảng trắng; internal -> lọc trong trường)")
    void UTC012_getAllTeachers_validFilters_returnPage() {
        String organization = TLU_NAME;
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
        when(teacherRepository.findAllTeachers(eq("Trần Văn Nam"), eq(true), eq(majorName), any()))
                .thenReturn(expectedPage);

        Page<TeacherResponse> result = userService.getAllTeachers("  Trần   Văn Nam ", " Internal",
                "Công nghệ thông tin ", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Trần Văn Nam");
        assertThat(result.getContent().get(0).getAmount()).isEqualTo(5L);
        assertThat(result.getContent().get(0).getStatus()).isTrue();
    }

    @Test
    @UnitTestCase(id = "UTC101", method = "getAllTeachers", type = Type.BOUNDARY,
            precondition = "teacherRepository.findAllTeachers trả về trang rỗng",
            input = "name = \"   \"; organizationType = \"\"; majorName = null; pageable = page 0, size 10",
            returns = "Gọi teacherRepository.findAllTeachers(null, null, null, pageable) - chuỗi rỗng/toàn khoảng trắng không lọc")
    void UTC101_getAllTeachers_blankFilters_passNullToRepository() {
        capturedTeacherPageable("   ", "", null, PageRequest.of(0, 10));

        verify(teacherRepository).findAllTeachers(isNull(), isNull(), isNull(), any());
    }

    @Test
    @UnitTestCase(id = "UTC102", method = "getAllTeachers", type = Type.NORMAL,
            precondition = "teacherRepository.findAllTeachers trả về trang rỗng",
            input = "pageable = page 2, size 10, không có sort",
            returns = "Pageable truyền xuống: page 2, size 10, sort a.status DESC, t.id DESC")
    void UTC102_getAllTeachers_noSort_defaultActiveFirstNewestFirst() {
        Pageable result = capturedTeacherPageable(null, null, null, PageRequest.of(2, 10));

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getSort()).containsExactly(Sort.Order.desc("a.status"), Sort.Order.desc("t.id"));
    }

    @Test
    @UnitTestCase(id = "UTC103", method = "getAllTeachers", type = Type.NORMAL,
            precondition = "teacherRepository.findAllTeachers trả về trang rỗng",
            input = "pageable = page 0, size 10, sort = name ASC, amount DESC",
            returns = "Sort truyền xuống: a.lastName ASC, a.firstName ASC, t.amount DESC, t.id DESC")
    void UTC103_getAllTeachers_sortByNameAndAmount_mappedInOrder() {
        Pageable result = capturedTeacherPageable(null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.desc("amount"))));

        assertThat(result.getSort()).containsExactly(Sort.Order.asc("a.lastName"), Sort.Order.asc("a.firstName"),
                Sort.Order.desc("t.amount"), Sort.Order.desc("t.id"));
    }

    @Test
    @UnitTestCase(id = "UTC104", method = "getAllTeachers", type = Type.NORMAL,
            precondition = "teacherRepository.findAllTeachers trả về trang rỗng",
            input = "pageable = page 0, size 10, sort = majorName DESC, organization ASC",
            returns = "Sort truyền xuống: m.name DESC, t.organization ASC, t.id DESC")
    void UTC104_getAllTeachers_sortByMajorAndOrganization_mappedToColumns() {
        Pageable result = capturedTeacherPageable(null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("majorName"), Sort.Order.asc("organization"))));

        assertThat(result.getSort()).containsExactly(Sort.Order.desc("m.name"), Sort.Order.asc("t.organization"),
                Sort.Order.desc("t.id"));
    }

    @Test
    @UnitTestCase(id = "UTC105", method = "getAllTeachers", type = Type.ABNORMAL,
            input = "sort lần lượt = id, password, a.status, status, email, phone",
            exception = "AppException: INVALID_SORT_FIELD (\"Trường sắp xếp không hợp lệ\"); không gọi teacherRepository")
    void UTC105_getAllTeachers_sortFieldNotWhitelisted_throwAppException() {
        for (String field : List.of("id", "password", "a.status", "status", "email", "phone")) {
            assertThatThrownBy(() -> userService.getAllTeachers(null, null, null,
                    PageRequest.of(0, 10, Sort.by(field))))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_SORT_FIELD);
        }
        verifyNoInteractions(teacherRepository);
    }

    @Test
    @UnitTestCase(id = "UTC114", method = "getAllTeachers", type = Type.NORMAL,
            precondition = "teacherRepository.findAllTeachers trả về trang rỗng",
            input = "organizationType = \"external\"; pageable = page 0, size 10",
            returns = "Gọi teacherRepository.findAllTeachers(null, false, null, pageable) - chỉ lấy giáo viên ngoài trường")
    void UTC114_getAllTeachers_externalType_passInternalFalse() {
        capturedTeacherPageable(null, "external", null, PageRequest.of(0, 10));

        verify(teacherRepository).findAllTeachers(isNull(), eq(false), isNull(), any());
    }

    @Test
    @UnitTestCase(id = "UTC115", method = "getAllTeachers", type = Type.ABNORMAL,
            input = "organizationType lần lượt = \"Trường Đại học Thủy lợi\", \"abc\", \"in\"",
            exception = "AppException: INVALID_REQUEST (\"Yêu cầu không hợp lệ\"); không gọi teacherRepository")
    void UTC115_getAllTeachers_unknownOrganizationType_throwAppException() {
        for (String type : List.of(TLU_NAME, "abc", "in")) {
            assertThatThrownBy(() -> userService.getAllTeachers(null, type, null, PageRequest.of(0, 10)))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
        verifyNoInteractions(teacherRepository);
    }

    // ===== getTeacherDetail =====

    @Test
    @UnitTestCase(id = "UTC098", method = "getTeacherDetail", type = Type.NORMAL,
            precondition = "teacherRepository.findTeacherDetail(publicId) trả về giáo viên annv@tlu.edu.vn",
            input = "publicId = <uuid giáo viên>",
            returns = "TeacherDetailResponse có email = \"annv@tlu.edu.vn\", phone = \"0912345678\", organization = \"Trường Đại học Thủy lợi\", amount = 20")
    void UTC098_getTeacherDetail_found_returnFullDetail() {
        TeacherDetailResponse detail = TeacherDetailResponse.builder()
                .publicId(publicId).name("Nguyễn Văn An").email("annv@tlu.edu.vn").phone("0912345678")
                .organization(TLU_NAME).majorName("Kỹ thuật phần mềm").amount(20L).status(true).build();
        when(teacherRepository.findTeacherDetail(publicId)).thenReturn(Optional.of(detail));

        TeacherDetailResponse result = userService.getTeacherDetail(publicId.toString());

        assertThat(result.getEmail()).isEqualTo("annv@tlu.edu.vn");
        assertThat(result.getPhone()).isEqualTo("0912345678");
        assertThat(result.getOrganization()).isEqualTo(TLU_NAME);
        assertThat(result.getAmount()).isEqualTo(20L);
    }

    @Test
    @UnitTestCase(id = "UTC099", method = "getTeacherDetail", type = Type.ABNORMAL,
            precondition = "teacherRepository.findTeacherDetail(publicId) trả về Optional.empty()",
            input = "publicId = <uuid không có trong DB>",
            exception = "AppException: TEACHER_NOT_FOUND (\"Giáo viên không tồn tại\")")
    void UTC099_getTeacherDetail_notFound_throwAppException() {
        when(teacherRepository.findTeacherDetail(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getTeacherDetail(publicId.toString()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEACHER_NOT_FOUND);
    }

    @Test
    @UnitTestCase(id = "UTC100", method = "getTeacherDetail", type = Type.ABNORMAL,
            input = "publicId = \"khong-phai-uuid\"",
            exception = "AppException: TEACHER_NOT_FOUND (\"Giáo viên không tồn tại\"); không gọi teacherRepository")
    void UTC100_getTeacherDetail_malformedPublicId_throwAppException() {
        assertThatThrownBy(() -> userService.getTeacherDetail("khong-phai-uuid"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEACHER_NOT_FOUND);
        verifyNoInteractions(teacherRepository);
    }

    // ===== createStudent =====

    @Test
    @UnitTestCase(id = "UTC013", method = "createStudent", type = Type.ABNORMAL,
            input = "request = {studentCode: \"2251172489\", phone: \"987654321\", các trường khác hợp lệ}",
            exception = "AppException: PHONE_INVALID (\"Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0\"); không truy vấn DB")
    void UTC013_createStudent_oneFormatError_throwSpecificError() {
        assertError(validRequest().phone("987654321").build(), ErrorCode.PHONE_INVALID);
        verify(studentRepository, never()).findAllByStudentCodeIn(any());
    }

    @Test
    @UnitTestCase(id = "UTC014", method = "createStudent", type = Type.ABNORMAL,
            input = "request = {phone: \"123\", batch: \"64\", các trường khác hợp lệ}",
            exception = "AppException: ROW_INFO_INVALID (\"Số điện thoại và Khóa không hợp lệ, vui lòng kiểm tra lại\")")
    void UTC014_createStudent_twoFormatErrors_listInvalidFields() {
        assertInvalid(validRequest().phone("123").batch("64").build(),
                "Số điện thoại và Khóa không hợp lệ, vui lòng kiểm tra lại");
    }

    @Test
    @UnitTestCase(id = "UTC120", method = "createStudent", type = Type.ABNORMAL,
            input = "request = {lastName: \"Đức2\", phone: \"123\", batch: \"64\", các trường khác hợp lệ}",
            exception = "AppException: ROW_INFO_INVALID (\"Tên, Số điện thoại và Khóa không hợp lệ, vui lòng kiểm tra lại\"); các trường xếp theo thứ tự cột")
    void UTC120_createStudent_threeFormatErrors_listInvalidFieldsInColumnOrder() {
        assertInvalid(validRequest().batch("64").phone("123").lastName("Đức2").build(),
                "Tên, Số điện thoại và Khóa không hợp lệ, vui lòng kiểm tra lại");
    }

    @Test
    @UnitTestCase(id = "UTC015", method = "createStudent", type = Type.ABNORMAL,
            input = "request = {studentCode: \"   \", các trường khác hợp lệ}",
            exception = "AppException: STUDENT_CODE_EMPTY (\"Mã sinh viên không được để trống\") - chỉ 1 lỗi trống, không sinh thêm lỗi định dạng")
    void UTC015_createStudent_blankStudentCode_throwEmptyError() {
        assertError(validRequest().studentCode("   ").build(), ErrorCode.STUDENT_CODE_EMPTY);
    }

    @Test
    @UnitTestCase(id = "UTC016", method = "createStudent", type = Type.ABNORMAL,
            input = "request = {lastName: \"Đức2\", các trường khác hợp lệ}",
            exception = "AppException: LAST_NAME_INVALID (\"Tên chỉ được chứa chữ cái\")")
    void UTC016_createStudent_lastNameHasDigit_throwAppException() {
        assertError(validRequest().lastName("Đức2").build(), ErrorCode.LAST_NAME_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC017", method = "createStudent", type = Type.ABNORMAL,
            precondition = "majorRepository.findAll trả về 1 ngành \"Kỹ thuật phần mềm\" (7480103)",
            input = "request = {major: \"Ngành không tồn tại\", các trường khác hợp lệ}",
            exception = "AppException: MAJOR_INVALID (\"Ngành học không tồn tại trong hệ thống\")")
    void UTC017_createStudent_majorUnknown_throwAppException() {
        assertError(validRequest().major("Ngành không tồn tại").build(), ErrorCode.MAJOR_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC018", method = "createStudent", type = Type.ABNORMAL,
            input = "request.email lần lượt = \"duc@e.tlu.edu.vn\", \"duc@tlu.edu.vn\", \"duc@sub.gmail.com\", \"duc@gmail.com.vn\"",
            exception = "AppException: STUDENT_EMAIL_DOMAIN_INVALID (\"Email sinh viên phải có đuôi @gmail.com\"); không truy vấn DB")
    void UTC018_createStudent_emailNotGmail_throwDomainError() {
        for (String email : List.of("duc@e.tlu.edu.vn", "duc@tlu.edu.vn", "duc@sub.gmail.com", "duc@gmail.com.vn")) {
            assertError(validRequest().email(email).build(), ErrorCode.STUDENT_EMAIL_DOMAIN_INVALID);
        }
        verify(studentRepository, never()).findAllByStudentCodeIn(any());
    }

    @Test
    @UnitTestCase(id = "UTC019", method = "createStudent", type = Type.ABNORMAL,
            input = "request = {email: \"duc@gmail\", các trường khác hợp lệ}",
            exception = "AppException: EMAIL_INVALID (\"Email không đúng định dạng\") - không báo thêm lỗi đuôi email")
    void UTC019_createStudent_emailMalformed_throwFormatErrorOnly() {
        assertError(validRequest().email("duc@gmail").build(), ErrorCode.EMAIL_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC020", method = "createStudent", type = Type.NORMAL,
            precondition = "Chưa có mã SV và email trong DB; roleRepository.findById(\"STUDENT\") trả về role STUDENT",
            input = "request = {studentCode: \"2251172489\", firstName: \"Lê Trung\", lastName: \"Đức\", email: \"duc@gmail.com\", phone: \"0987654321\", batch: \"K64\", className: \"64KTPM1\", major: \"Kỹ thuật phần mềm\"}",
            returns = "code = 1000; lưu Account (status = true, role STUDENT) và Student (status = true, đúng ngành, gắn Account vừa tạo)",
            log = "Thêm sinh viên thành công")
    void UTC020_createStudent_newStudent_createAccountAndStudent() {
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
    @UnitTestCase(id = "UTC021", method = "createStudent", type = Type.NORMAL,
            precondition = "Chưa có mã SV và email trong DB",
            input = "request = {firstName: \"  Lê<zero-width>   Trung \", email: \" DUC@GMAIL.COM \", batch: \"k64\", major: \"7480103\", các trường khác hợp lệ}",
            returns = "Account lưu firstName = \"Lê Trung\", email = \"duc@gmail.com\"; Student lưu batch = \"K64\" (ngành tra theo mã)")
    void UTC021_createStudent_messyInput_normalizedBeforeSave() {
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
    @UnitTestCase(id = "UTC022", method = "createStudent", type = Type.ABNORMAL,
            precondition = "Không có mã SV 2251172489 trong DB; accountRepository.findAllByEmailIn trả về tài khoản khác (id 99) có email duc@gmail.com",
            input = "request = {studentCode: \"2251172489\", email: \"duc@gmail.com\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Email trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC022_createStudent_emailBelongsToOtherAccount_throwDuplicated() {
        Account other = Account.builder().id(99L).email("duc@gmail.com").build();
        when(studentRepository.findAllByStudentCodeIn(anyCollection())).thenReturn(List.of());
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));

        assertDuplicated(validRequest().build(), "Email trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC023", method = "createStudent", type = Type.ABNORMAL,
            precondition = "Mã SV 2251172489 đã có trong DB với email duc@gmail.com; không có email khac@gmail.com",
            input = "request = {studentCode: \"2251172489\", email: \"khac@gmail.com\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Mã sinh viên trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC023_createStudent_codeExistsWithOtherEmail_throwDuplicated() {
        when(studentRepository.findAllByStudentCodeIn(anyCollection()))
                .thenReturn(List.of(existingStudent(false, false)));
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());

        assertDuplicated(validRequest().email("khac@gmail.com").build(),
                "Mã sinh viên trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC084", method = "createStudent", type = Type.ABNORMAL,
            precondition = "Mã SV 2251172489 đã có trong DB, thuộc tài khoản id 10 (duc@gmail.com); email lan@gmail.com thuộc tài khoản khác (id 99)",
            input = "request = {studentCode: \"2251172489\", email: \"lan@gmail.com\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Mã sinh viên, Email trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC084_createStudent_codeAndEmailBelongToDifferentAccounts_throwDuplicated() {
        Account other = Account.builder().id(99L).email("lan@gmail.com").build();
        when(studentRepository.findAllByStudentCodeIn(anyCollection()))
                .thenReturn(List.of(existingStudent(true, true)));
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));

        assertDuplicated(validRequest().email("lan@gmail.com").build(),
                "Mã sinh viên, Email trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC024", method = "createStudent", type = Type.ABNORMAL,
            precondition = "DB có sinh viên 2251172489 / duc@gmail.com, khóa K64, đang bị tắt",
            input = "request = {studentCode: \"2251172489\", email: \"duc@gmail.com\", batch: \"K65\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Mã sinh viên, Email trùng với tài khoản đã tồn tại trong hệ thống\")")
    void UTC024_createStudent_batchMismatch_throwDuplicated() {
        givenExisting(existingStudent(false, false));

        assertDuplicated(validRequest().batch("K65").build(),
                "Mã sinh viên, Email trùng với tài khoản đã tồn tại trong hệ thống");
    }

    @Test
    @UnitTestCase(id = "UTC116", method = "createStudent", type = Type.ABNORMAL,
            precondition = "Mã SV 2251172489 đã có trong DB với email duc@gmail.com; accountRepository.findAllByPhoneIn trả về tài khoản id 99 giữ SĐT 0987654321",
            input = "request = {studentCode: \"2251172489\", email: \"khac@gmail.com\", phone: \"0987654321\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Mã sinh viên, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC116_createStudent_codeAndPhoneDuplicated_throwDuplicated() {
        Account phoneOwner = Account.builder().id(99L).phone("0987654321").build();
        when(studentRepository.findAllByStudentCodeIn(anyCollection()))
                .thenReturn(List.of(existingStudent(false, false)));
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of());
        when(accountRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(phoneOwner));

        assertDuplicated(validRequest().email("khac@gmail.com").build(),
                "Mã sinh viên, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC117", method = "createStudent", type = Type.ABNORMAL,
            precondition = "Không có mã SV 2251172489 trong DB; tài khoản id 99 có email duc@gmail.com và SĐT 0987654321",
            input = "request = {studentCode: \"2251172489\", email: \"duc@gmail.com\", phone: \"0987654321\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC117_createStudent_emailAndPhoneDuplicated_throwDuplicated() {
        Account other = Account.builder().id(99L).email("duc@gmail.com").phone("0987654321").build();
        when(studentRepository.findAllByStudentCodeIn(anyCollection())).thenReturn(List.of());
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));
        when(accountRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(other));

        assertDuplicated(validRequest().build(),
                "Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC118", method = "createStudent", type = Type.ABNORMAL,
            precondition = "Mã SV 2251172489 thuộc tài khoản id 10 (duc@gmail.com); email lan@gmail.com thuộc tài khoản id 99; SĐT 0987654321 thuộc tài khoản id 99",
            input = "request = {studentCode: \"2251172489\", email: \"lan@gmail.com\", phone: \"0987654321\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Mã sinh viên, Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC118_createStudent_codeEmailAndPhoneDuplicated_throwDuplicated() {
        Account other = Account.builder().id(99L).email("lan@gmail.com").phone("0987654321").build();
        when(studentRepository.findAllByStudentCodeIn(anyCollection()))
                .thenReturn(List.of(existingStudent(true, true)));
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));
        when(accountRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(other));

        assertDuplicated(validRequest().email("lan@gmail.com").build(),
                "Mã sinh viên, Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC025", method = "createStudent", type = Type.NORMAL,
            precondition = "DB có sinh viên 2251172489 đang hoạt động, lớp 63KTPM1, SĐT 0911111111",
            input = "request = {studentCode: \"2251172489\", email: \"duc@gmail.com\", className: \"63KTPM1\", phone: \"0911111111\", khớp hết DB}",
            returns = "code = 1101; không gọi accountRepository.saveAll và studentRepository.saveAll",
            log = "Sinh viên đã có trong hệ thống, thông tin không thay đổi")
    void UTC025_createStudent_activeAndSameData_warnNoChange() {
        givenExisting(existingStudent(true, true));

        StudentCreationResult result = userService.createStudent(sameAsExistingRequest().build());

        assertResult(result, 1101, ErrorCode.STUDENT_NO_CHANGE.getMessage());
        verify(accountRepository, never()).saveAll(any());
        verify(studentRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC026", method = "createStudent", type = Type.BOUNDARY,
            precondition = "DB có sinh viên 2251172489 đang hoạt động, lớp 63KTPM1, SĐT 0911111111",
            input = "request = {className: \"63ktpm1\" (chỉ khác hoa/thường), các trường khác khớp DB}",
            returns = "code = 1101; không gọi studentRepository.saveAll",
            log = "Sinh viên đã có trong hệ thống, thông tin không thay đổi")
    void UTC026_createStudent_classDiffersOnlyInCase_warnNoChange() {
        givenExisting(existingStudent(true, true));

        StudentCreationResult result = userService.createStudent(sameAsExistingRequest().className("63ktpm1").build());

        assertResult(result, 1101, ErrorCode.STUDENT_NO_CHANGE.getMessage());
        verify(studentRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC027", method = "createStudent", type = Type.NORMAL,
            precondition = "DB có sinh viên 2251172489 đang hoạt động, lớp 63KTPM1, SĐT 0911111111",
            input = "request = {className: \"64KTPM1\", các trường khác khớp DB}",
            returns = "code = 1000; Student được lưu với lớp 64KTPM1, SĐT giữ nguyên 0911111111",
            log = "Đã cập nhật: Lớp")
    void UTC027_createStudent_activeAndClassChanged_updateClass() {
        Student student = existingStudent(true, true);
        givenExisting(student);

        StudentCreationResult result = userService.createStudent(sameAsExistingRequest().className("64KTPM1").build());

        assertResult(result, 1000, "Đã cập nhật: Lớp");
        assertThat(student.getClassName()).isEqualTo("64KTPM1");
        assertThat(student.getAccount().getPhone()).isEqualTo("0911111111");
        assertThat(capturedStudents()).containsExactly(student);
    }

    @Test
    @UnitTestCase(id = "UTC028", method = "createStudent", type = Type.NORMAL,
            precondition = "DB có sinh viên 2251172489 đang hoạt động, lớp 63KTPM1, SĐT 0911111111",
            input = "request = {className: \"64KTPM1\", phone: \"0987654321\", các trường khác khớp DB}",
            returns = "code = 1000; lưu lớp 64KTPM1 và SĐT 0987654321",
            log = "Đã cập nhật: Lớp, Số điện thoại")
    void UTC028_createStudent_activeAndClassPhoneChanged_listBothFields() {
        Student student = existingStudent(true, true);
        givenExisting(student);

        StudentCreationResult result = userService.createStudent(validRequest().build());

        assertResult(result, 1000, "Đã cập nhật: Lớp, Số điện thoại");
        assertThat(student.getClassName()).isEqualTo("64KTPM1");
        assertThat(student.getAccount().getPhone()).isEqualTo("0987654321");
        assertThat(capturedAccounts()).containsExactly(student.getAccount());
    }

    @Test
    @UnitTestCase(id = "UTC029", method = "createStudent", type = Type.NORMAL,
            precondition = "DB có sinh viên 2251172489 đã bị tắt (Account và Student status = false), lớp 63KTPM1, SĐT 0911111111",
            input = "request = {className: \"63KTPM1\", phone: \"0911111111\", khớp hết DB}",
            returns = "code = 1000; Account và Student chuyển status = true, lưu Student",
            log = "Đã kích hoạt lại tài khoản sinh viên")
    void UTC029_createStudent_inactiveAndSameData_reactivate() {
        Student student = existingStudent(false, false);
        givenExisting(student);

        StudentCreationResult result = userService.createStudent(sameAsExistingRequest().build());

        assertResult(result, 1000, "Đã kích hoạt lại tài khoản sinh viên");
        assertThat(student.getAccount().getStatus()).isTrue();
        assertThat(student.getStatus()).isTrue();
        assertThat(capturedStudents()).containsExactly(student);
    }

    @Test
    @UnitTestCase(id = "UTC030", method = "createStudent", type = Type.NORMAL,
            precondition = "DB có sinh viên 2251172489, Account bị tắt, lớp 63KTPM1, SĐT 0911111111",
            input = "request = {className: \"64KTPM1\", phone: \"0987654321\", các trường khác khớp DB}",
            returns = "code = 1000; Account và Student status = true, lớp 64KTPM1, SĐT 0987654321",
            log = "Đã kích hoạt lại và cập nhật: Lớp, Số điện thoại")
    void UTC030_createStudent_inactiveAndChanged_reactivateAndUpdate() {
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

    // ===== activateStudent =====

    @Test
    @UnitTestCase(id = "UTC031", method = "activateStudent", type = Type.NORMAL,
            precondition = "studentRepository.findByPublicId trả về sinh viên đã tắt (Account và Student status = false), lớp 63KTPM1, SĐT 0911111111",
            input = "publicId = <uuid sinh viên>",
            returns = "true; Account và Student status = true, lớp/SĐT giữ nguyên; gọi accountRepository.save và studentRepository.save")
    void UTC031_activateStudent_inactive_activateAndReturnTrue() {
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
    @UnitTestCase(id = "UTC032", method = "activateStudent", type = Type.NORMAL,
            precondition = "studentRepository.findByPublicId trả về sinh viên có Account tắt, Student vẫn bật",
            input = "publicId = <uuid sinh viên>",
            returns = "true; Account status = true")
    void UTC032_activateStudent_onlyAccountInactive_activateAndReturnTrue() {
        Student student = existingStudent(false, true);
        when(studentRepository.findByPublicId(publicId)).thenReturn(Optional.of(student));

        assertThat(userService.activateStudent(publicId.toString())).isTrue();
        assertThat(student.getAccount().getStatus()).isTrue();
    }

    @Test
    @UnitTestCase(id = "UTC033", method = "activateStudent", type = Type.NORMAL,
            precondition = "studentRepository.findByPublicId trả về sinh viên đang hoạt động",
            input = "publicId = <uuid sinh viên>",
            returns = "false; không gọi accountRepository.save và studentRepository.save")
    void UTC033_activateStudent_alreadyActive_returnFalse() {
        when(studentRepository.findByPublicId(publicId)).thenReturn(Optional.of(existingStudent(true, true)));

        assertThat(userService.activateStudent(publicId.toString())).isFalse();
        verify(accountRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    @UnitTestCase(id = "UTC034", method = "activateStudent", type = Type.ABNORMAL,
            precondition = "studentRepository.findByPublicId trả về Optional.empty()",
            input = "publicId = <uuid không có trong DB>",
            exception = "AppException: STUDENT_NOT_FOUND (\"Sinh viên không tồn tại\")")
    void UTC034_activateStudent_notFound_throwAppException() {
        when(studentRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateStudent(publicId.toString()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
    }

    @Test
    @UnitTestCase(id = "UTC035", method = "activateStudent", type = Type.ABNORMAL,
            input = "publicId = \"khong-phai-uuid\"",
            exception = "AppException: STUDENT_NOT_FOUND (\"Sinh viên không tồn tại\"); không gọi studentRepository")
    void UTC035_activateStudent_malformedPublicId_throwAppException() {
        assertThatThrownBy(() -> userService.activateStudent("khong-phai-uuid"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
        verifyNoInteractions(studentRepository);
    }

    // ===== importStudents =====

    @Test
    @UnitTestCase(id = "UTC036", method = "importStudents", type = Type.NORMAL,
            precondition = "DB chưa có mã SV và email nào trong file",
            input = "file students.xlsx: dòng 2 hợp lệ (2251172489); dòng 3, 4 trùng mã 2251172518; dòng 5 trống; dòng 6 email \"sai-email\"",
            returns = "totalRows = 4, successCount = 1, errorCount = 3; kết quả các dòng 2, 3, 4, 6; chỉ lưu 1 sinh viên",
            log = "Dòng 2: Thêm sinh viên thành công; dòng 3, 4: Mã sinh viên bị trùng với dòng khác trong file; dòng 6: Email không đúng định dạng")
    void UTC036_importStudents_mixedRows_savePartially() throws IOException {
        givenNothingInDb();
        MockMultipartFile file = buildStudentExcel(sheet -> {
            writeRow(sheet, 1, validRow("2251172489", "a@gmail.com", "0987654321"));
            writeRow(sheet, 2, validRow("2251172518", "b@gmail.com", "0987654322"));
            writeRow(sheet, 3, validRow("2251172518", "c@gmail.com", "0987654323"));
            writeRow(sheet, 4, "", " ", "", "", "", "", "", "");
            writeRow(sheet, 5, "1951060660", "Nguyễn", "Hạnh", "sai-email", "0987654324", "K64", "64KTPM1", "Kỹ thuật phần mềm");
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
    @UnitTestCase(id = "UTC037", method = "importStudents", type = Type.NORMAL,
            precondition = "DB có 2 sinh viên đang hoạt động: 2251172489 (lớp 63KTPM1) và 2251172518 (lớp 63KTPM2)",
            input = "file students.xlsx: dòng 2 khớp y hệt 2251172489; dòng 3 là 2251172518 đổi lớp sang 64KTPM1",
            returns = "Dòng 2 WARNING, dòng 3 SUCCESS; chỉ lưu sinh viên 2251172518 với lớp 64KTPM1",
            log = "Dòng 2: Sinh viên đã có trong hệ thống, thông tin không thay đổi; dòng 3: Đã cập nhật: Lớp")
    void UTC037_importStudents_activeSameAndChanged_behaveLikeManual() throws IOException {
        Student duc = existingStudent(true, true);
        Account anAccount = Account.builder()
                .id(11L).email("an@gmail.com").firstName("Nguyễn Văn").lastName("An")
                .phone("0922222222").status(true).build();
        Student an = Student.builder()
                .id(21L).studentCode("2251172518").batch("K64").className("63KTPM2")
                .status(true).major(major).account(anAccount).build();
        when(studentRepository.findAllByStudentCodeIn(anyCollection())).thenReturn(List.of(duc, an));
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(duc.getAccount(), anAccount));
        MockMultipartFile file = buildStudentExcel(sheet -> {
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
    @UnitTestCase(id = "UTC038", method = "importStudents", type = Type.BOUNDARY,
            input = "file students.xlsx: ô Mã SV kiểu số 2251172489; ô Email là công thức lỗi =1/0",
            returns = "studentCode = \"2251172489\" (không thành 2.251172489E9); dòng có status ERROR",
            log = "Email không đúng định dạng")
    void UTC038_importStudents_numericAndFormulaErrorCells_readAsText() throws IOException {
        MockMultipartFile file = buildStudentExcel(sheet -> {
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
    @UnitTestCase(id = "UTC039", method = "importStudents", type = Type.ABNORMAL,
            input = "file students.xlsx: cột C tiêu đề \"Tên gọi\" thay vì \"Tên\"",
            exception = "AppException: FILE_HEADER_INVALID (\"Dòng tiêu đề không đúng với file mẫu\")")
    void UTC039_importStudents_wrongHeader_throwAppException() throws IOException {
        MockMultipartFile file = buildStudentExcel(sheet -> {
            sheet.getRow(0).getCell(2).setCellValue("Tên gọi");
            writeRow(sheet, 1, validRow("2251172489", "a@gmail.com"));
        });

        assertThatThrownBy(() -> userService.importStudents(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_HEADER_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC040", method = "importStudents", type = Type.ABNORMAL,
            input = "file students.xlsx: 2 dòng dữ liệu, cột Lớp của dòng 2-3 bị gộp ô",
            exception = "AppException: FILE_MERGED_CELLS (\"File import không được chứa ô bị gộp\")")
    void UTC040_importStudents_mergedCells_throwAppException() throws IOException {
        MockMultipartFile file = buildStudentExcel(sheet -> {
            writeRow(sheet, 1, validRow("2251172489", "a@gmail.com"));
            writeRow(sheet, 2, validRow("2251172518", "b@gmail.com"));
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 6, 6));
        });

        assertThatThrownBy(() -> userService.importStudents(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_MERGED_CELLS);
    }

    @Test
    @UnitTestCase(id = "UTC041", method = "importStudents", type = Type.ABNORMAL,
            input = "file students.xlsx chỉ có dòng tiêu đề",
            exception = "AppException: FILE_NO_DATA (\"File import không có dòng dữ liệu nào\")")
    void UTC041_importStudents_headerOnly_throwAppException() throws IOException {
        MockMultipartFile file = buildStudentExcel(sheet -> { });

        assertThatThrownBy(() -> userService.importStudents(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_NO_DATA);
    }

    @Test
    @UnitTestCase(id = "UTC042", method = "importStudents", type = Type.ABNORMAL,
            input = "file students.xlsx có nội dung text \"không phải excel\"",
            exception = "AppException: FILE_INVALID_FORMAT (\"File import phải có định dạng .xlsx\")")
    void UTC042_importStudents_fakeXlsx_throwAppException() {
        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "text/plain",
                "không phải excel".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> userService.importStudents(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_INVALID_FORMAT);
    }

    // ===== createTeacher =====

    @Test
    @UnitTestCase(id = "UTC043", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"annv@tlu.edu.vn\", phone: \"987654321\", các trường khác hợp lệ}",
            exception = "AppException: PHONE_INVALID (\"Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0\"); không truy vấn DB")
    void UTC043_createTeacher_oneFormatError_throwSpecificError() {
        assertTeacherError(validTeacherRequest().phone("987654321").build(), ErrorCode.PHONE_INVALID);
        verify(accountRepository, never()).findAllByEmailIn(any());
    }

    @Test
    @UnitTestCase(id = "UTC044", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {phone: \"123\", lastName: \"An2\", các trường khác hợp lệ}",
            exception = "AppException: ROW_INFO_INVALID (\"Tên và Số điện thoại không hợp lệ, vui lòng kiểm tra lại\")")
    void UTC044_createTeacher_twoFormatErrors_listInvalidFields() {
        assertTeacherInvalid(validTeacherRequest().phone("123").lastName("An2").build(),
                "Tên và Số điện thoại không hợp lệ, vui lòng kiểm tra lại");
    }

    @Test
    @UnitTestCase(id = "UTC045", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"   \", organization: \"Trường Đại học Thủy lợi\", các trường khác hợp lệ}",
            exception = "AppException: EMAIL_EMPTY (\"Email không được để trống\") - không đối chiếu đơn vị công tác với email")
    void UTC045_createTeacher_blankEmail_throwEmptyError() {
        assertTeacherError(validTeacherRequest().email("   ").build(), ErrorCode.EMAIL_EMPTY);
    }

    @Test
    @UnitTestCase(id = "UTC046", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"sai-email\", organization: \"Trường Đại học Thủy lợi\", các trường khác hợp lệ}",
            exception = "AppException: EMAIL_INVALID (\"Email không đúng định dạng\") - không báo lệch đơn vị công tác")
    void UTC046_createTeacher_emailMalformed_throwFormatErrorOnly() {
        assertTeacherError(validTeacherRequest().email("sai-email").build(), ErrorCode.EMAIL_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC047", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {organization: null, các trường khác hợp lệ}",
            exception = "AppException: ORGANIZATION_EMPTY (\"Đơn vị công tác không được để trống\")")
    void UTC047_createTeacher_organizationNull_throwEmptyError() {
        assertTeacherError(validTeacherRequest().organization(null).build(), ErrorCode.ORGANIZATION_EMPTY);
    }

    @Test
    @UnitTestCase(id = "UTC048", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {organization: \"Trường ĐH #1\" (chứa ký tự # không cho phép), các trường khác hợp lệ}",
            exception = "AppException: ORGANIZATION_INVALID (\"Đơn vị công tác chỉ gồm chữ, số, khoảng trắng và các dấu . , - & ( ) ' / (tối đa 255 ký tự)\")")
    void UTC048_createTeacher_organizationUnknown_throwAppException() {
        assertTeacherError(validTeacherRequest().organization("Trường ĐH #1").build(), ErrorCode.ORGANIZATION_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC049", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"an.nguyen@gmail.com\", organization: \"Trường Đại học Thủy lợi\", major: \"\", các trường khác hợp lệ}",
            exception = "AppException: ORGANIZATION_NOT_MATCH_EMAIL (\"Đơn vị công tác không khớp với email (email @tlu.edu.vn phải là Trường Đại học Thủy lợi, email @gmail.com phải là trường khác)\")")
    void UTC049_createTeacher_gmailWithInternalOrganization_throwAppException() {
        assertTeacherError(validTeacherRequest().email(EXTERNAL_EMAIL).major("").build(), ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL);
    }

    @Test
    @UnitTestCase(id = "UTC050", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"annv@tlu.edu.vn\", organization: \"Trường Đại học Bách khoa Hà Nội\", các trường khác hợp lệ}",
            exception = "AppException: ORGANIZATION_NOT_MATCH_EMAIL (\"Đơn vị công tác không khớp với email (email @tlu.edu.vn phải là Trường Đại học Thủy lợi, email @gmail.com phải là trường khác)\")")
    void UTC050_createTeacher_tluEmailWithExternalOrganization_throwAppException() {
        assertTeacherError(validTeacherRequest().organization(OTHER_SCHOOL).build(), ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL);
    }

    @Test
    @UnitTestCase(id = "UTC051", method = "createTeacher", type = Type.ABNORMAL,
            input = "organization = \"Trường Đại học Bách khoa Hà Nội\"; request.email lần lượt = \"annv@e.tlu.edu.vn\", \"annv@outlook.com\", \"annv@sub.tlu.edu.vn.com\", \"annv@gmail.com.vn\"",
            exception = "AppException: TEACHER_EMAIL_DOMAIN_INVALID (\"Email giáo viên phải có đuôi @tlu.edu.vn (trong trường) hoặc @gmail.com (ngoài trường)\"); không truy vấn DB")
    void UTC051_createTeacher_emailDomainNotAllowed_throwDomainError() {
        for (String email : List.of("annv@e.tlu.edu.vn", "annv@outlook.com", "annv@sub.tlu.edu.vn.com", "annv@gmail.com.vn")) {
            assertTeacherError(validTeacherRequest().email(email).organization(OTHER_SCHOOL).build(),
                    ErrorCode.TEACHER_EMAIL_DOMAIN_INVALID);
        }
        verify(accountRepository, never()).findAllByEmailIn(any());
    }

    @Test
    @UnitTestCase(id = "UTC052", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {amount: -1, các trường khác hợp lệ}",
            exception = "AppException: AMOUNT_INVALID (\"Số lượng sinh viên hướng dẫn phải là số nguyên không âm\"); không truy vấn DB")
    void UTC052_createTeacher_amountNegative_throwAppException() {
        assertTeacherError(validTeacherRequest().amount("-1").build(), ErrorCode.AMOUNT_INVALID);
        verify(accountRepository, never()).findAllByEmailIn(any());
    }

    @Test
    @UnitTestCase(id = "UTC053", method = "createTeacher", type = Type.BOUNDARY,
            precondition = "Email chưa có trong DB",
            input = "request = {amount: 0, các trường khác hợp lệ}",
            returns = "code = 1000; Teacher lưu amount = 0 (giáo viên không nhận hướng dẫn)",
            log = "Thêm giáo viên thành công")
    void UTC053_createTeacher_amountZero_createWithZero() {
        givenNoAccountInDb();

        assertTeacherResult(userService.createTeacher(validTeacherRequest().amount("0").build()), 1000, "Thêm giáo viên thành công");
        assertThat(capturedTeachers().get(0).getAmount()).isZero();
    }

    @Test
    @UnitTestCase(id = "UTC085", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {amount: 5.5, các trường khác hợp lệ}",
            exception = "AppException: AMOUNT_INVALID (\"Số lượng sinh viên hướng dẫn phải là số nguyên không âm\"); không làm tròn, không truy vấn DB")
    void UTC085_createTeacher_amountDecimal_throwAppException() {
        assertTeacherError(validTeacherRequest().amount("5.5").build(), ErrorCode.AMOUNT_INVALID);
        verify(accountRepository, never()).findAllByEmailIn(any());
    }

    @Test
    @UnitTestCase(id = "UTC086", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {amount: \"abc\", các trường khác hợp lệ}",
            exception = "AppException: AMOUNT_INVALID (\"Số lượng sinh viên hướng dẫn phải là số nguyên không âm\")")
    void UTC086_createTeacher_amountNotNumber_throwAppException() {
        assertTeacherError(validTeacherRequest().amount("abc").build(), ErrorCode.AMOUNT_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC087", method = "createTeacher", type = Type.BOUNDARY,
            input = "request = {amount: 5.0, các trường khác hợp lệ}",
            exception = "AppException: AMOUNT_INVALID (\"Số lượng sinh viên hướng dẫn phải là số nguyên không âm\"); số thập phân bị từ chối kể cả khi phần lẻ bằng 0")
    void UTC087_createTeacher_amountDecimalZeroFraction_throwAppException() {
        assertTeacherError(validTeacherRequest().amount("5.0").build(), ErrorCode.AMOUNT_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC088", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"sai-dinh-dang\", amount: 5.5, các trường khác hợp lệ}",
            exception = "AppException: ROW_INFO_INVALID (\"Email và Số lượng hướng dẫn không hợp lệ, vui lòng kiểm tra lại\"); lỗi amount được gom chung với lỗi email")
    void UTC088_createTeacher_amountDecimalAndEmailInvalid_listInvalidFields() {
        assertTeacherInvalid(validTeacherRequest().email("sai-dinh-dang").amount("5.5").build(),
                "Email và Số lượng hướng dẫn không hợp lệ, vui lòng kiểm tra lại");
    }

    @Test
    @UnitTestCase(id = "UTC054", method = "createTeacher", type = Type.ABNORMAL,
            precondition = "majorRepository.findAll trả về 1 ngành \"Kỹ thuật phần mềm\" (7480103)",
            input = "request = {major: \"Ngành không tồn tại\", các trường khác hợp lệ}",
            exception = "AppException: MAJOR_INVALID (\"Ngành học không tồn tại trong hệ thống\")")
    void UTC054_createTeacher_majorUnknown_throwAppException() {
        assertTeacherError(validTeacherRequest().major("Ngành không tồn tại").build(), ErrorCode.MAJOR_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC055", method = "createTeacher", type = Type.NORMAL,
            precondition = "Email chưa có trong DB; roleRepository.findById(\"SUPERVISOR\") trả về role SUPERVISOR",
            input = "request = {firstName: \"Nguyễn Văn\", lastName: \"An\", email: \"annv@tlu.edu.vn\", phone: \"0987654321\", organization: \"Trường Đại học Thủy lợi\", major: \"Kỹ thuật phần mềm\", amount: null}",
            returns = "code = 1000; lưu Account (status = true, role SUPERVISOR) và Teacher (amount = 20, Trường Đại học Thủy lợi, đúng ngành)",
            log = "Thêm giáo viên thành công")
    void UTC055_createTeacher_newInternal_createWithDefaultAmount20() {
        givenNoAccountInDb();

        assertTeacherResult(userService.createTeacher(validTeacherRequest().build()), 1000, "Thêm giáo viên thành công");
        Account account = capturedAccounts().get(0);
        assertThat(account.getStatus()).isTrue();
        assertThat(account.getRoles()).containsExactly(supervisorRole);
        Teacher teacher = capturedTeachers().get(0);
        assertThat(teacher.getAmount()).isEqualTo(20L);
        assertThat(teacher.getOrganization()).isEqualTo(TLU_NAME);
        assertThat(teacher.getMajor()).isSameAs(major);
        assertThat(teacher.getAccount()).isSameAs(account);
    }

    @Test
    @UnitTestCase(id = "UTC056", method = "createTeacher", type = Type.NORMAL,
            precondition = "Email chưa có trong DB",
            input = "request = {email: \" An.Nguyen@GMAIL.com \", organization: \"  Trường Đại học   Bách khoa Hà Nội \", major: null, amount: null, các trường khác hợp lệ}",
            returns = "Account lưu email = \"an.nguyen@gmail.com\"; Teacher lưu amount = 10, organization = \"Trường Đại học Bách khoa Hà Nội\", major = null")
    void UTC056_createTeacher_newExternal_createWithDefaultAmount10() {
        givenNoAccountInDb();

        userService.createTeacher(validTeacherRequest()
                .email(" An.Nguyen@GMAIL.com ")
                .organization("  Trường Đại học   Bách khoa Hà Nội ")
                .major(null)
                .build());

        assertThat(capturedAccounts().get(0).getEmail()).isEqualTo(EXTERNAL_EMAIL);
        Teacher teacher = capturedTeachers().get(0);
        assertThat(teacher.getAmount()).isEqualTo(10L);
        assertThat(teacher.getOrganization()).isEqualTo(OTHER_SCHOOL);
        assertThat(teacher.getMajor()).isNull();
    }

    @Test
    @UnitTestCase(id = "UTC057", method = "createTeacher", type = Type.NORMAL,
            precondition = "Email chưa có trong DB",
            input = "request = {email: \"an.nguyen@gmail.com\", organization: \"Trường Đại học Bách khoa Hà Nội\", major: \"\", amount: 50, các trường khác hợp lệ}",
            returns = "Teacher lưu amount = 50 (giá trị TBM nhập, vượt mặc định 10 vẫn hợp lệ, không giới hạn trên)")
    void UTC057_createTeacher_amountAboveDefault_useInputAmount() {
        givenNoAccountInDb();

        userService.createTeacher(validTeacherRequest()
                .email(EXTERNAL_EMAIL).organization(OTHER_SCHOOL).major("").amount("50").build());

        assertThat(capturedTeachers().get(0).getAmount()).isEqualTo(50L);
    }

    @Test
    @UnitTestCase(id = "UTC058", method = "createTeacher", type = Type.ABNORMAL,
            precondition = "accountRepository.findAllByEmailIn trả về tài khoản id 99 (annv@tlu.edu.vn) không có hồ sơ giáo viên",
            input = "request = {email: \"annv@tlu.edu.vn\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Email trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC058_createTeacher_emailBelongsToNonTeacher_throwDuplicated() {
        Account other = Account.builder().id(99L).email(INTERNAL_EMAIL).build();
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));
        when(teacherRepository.findAllByAccountIdIn(anyCollection())).thenReturn(List.of());

        assertTeacherDuplicated(validTeacherRequest().build(), "Email trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC059", method = "createTeacher", type = Type.ABNORMAL,
            precondition = "DB có giáo viên annv@tlu.edu.vn tên \"Nguyễn Văn An\" đang bị tắt",
            input = "request = {email: \"annv@tlu.edu.vn\", lastName: \"Bình\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Email trùng với tài khoản đã tồn tại trong hệ thống\")")
    void UTC059_createTeacher_nameMismatch_throwDuplicated() {
        givenExistingTeacher(existingTeacher(false, supervisorRole));

        assertTeacherDuplicated(validTeacherRequest().lastName("Bình").build(),
                "Email trùng với tài khoản đã tồn tại trong hệ thống");
    }

    @Test
    @UnitTestCase(id = "UTC119", method = "createTeacher", type = Type.ABNORMAL,
            precondition = "Tài khoản id 99 (annv@tlu.edu.vn, SĐT 0987654321) không có hồ sơ giáo viên",
            input = "request = {email: \"annv@tlu.edu.vn\", phone: \"0987654321\", các trường khác hợp lệ}",
            exception = "AppException: ACCOUNT_DUPLICATED (\"Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống\"); không lưu Account")
    void UTC119_createTeacher_emailAndPhoneDuplicated_throwDuplicated() {
        Account other = Account.builder().id(99L).email(INTERNAL_EMAIL).phone("0987654321").build();
        when(accountRepository.findAllByEmailIn(anyCollection())).thenReturn(List.of(other));
        when(accountRepository.findAllByPhoneIn(anyCollection())).thenReturn(List.of(other));
        when(teacherRepository.findAllByAccountIdIn(anyCollection())).thenReturn(List.of());

        assertTeacherDuplicated(validTeacherRequest().build(),
                "Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống");
        verify(accountRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC060", method = "createTeacher", type = Type.NORMAL,
            precondition = "DB có giáo viên annv@tlu.edu.vn đang hoạt động, amount 15, SĐT 0911111111",
            input = "request = {email: \"annv@tlu.edu.vn\", phone: \"0987654321\", amount: 5, các trường khác khớp DB}",
            returns = "code = 1100; amount giữ 15, SĐT giữ 0911111111; không gọi saveAll",
            log = "Thông tin giáo viên đã có trong hệ thống")
    void UTC060_createTeacher_alreadyActive_warnWithoutUpdate() {
        Teacher teacher = existingTeacher(true, supervisorRole);
        givenExistingTeacher(teacher);

        assertTeacherResult(userService.createTeacher(validTeacherRequest().amount("5").build()),
                ErrorCode.TEACHER_ALREADY_ACTIVE.getCode(), ErrorCode.TEACHER_ALREADY_ACTIVE.getMessage());
        assertThat(teacher.getAmount()).isEqualTo(15L);
        assertThat(teacher.getAccount().getPhone()).isEqualTo("0911111111");
        verify(accountRepository, never()).saveAll(any());
        verify(teacherRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC061", method = "createTeacher", type = Type.NORMAL,
            precondition = "DB có Trưởng bộ môn (role HEAD_OF_DEPARTMENT) email annv@tlu.edu.vn, có hồ sơ giáo viên, đang hoạt động",
            input = "request = {email: \"annv@tlu.edu.vn\", các trường khác hợp lệ}",
            returns = "code = 1100 (TEACHER_ALREADY_ACTIVE)")
    void UTC061_createTeacher_activeHeadOfDepartment_warnAlreadyActive() {
        Role hodRole = new Role();
        hodRole.setName("HEAD_OF_DEPARTMENT");
        givenExistingTeacher(existingTeacher(true, hodRole));

        assertThat(userService.createTeacher(validTeacherRequest().build()).getCode())
                .isEqualTo(ErrorCode.TEACHER_ALREADY_ACTIVE.getCode());
    }

    @Test
    @UnitTestCase(id = "UTC062", method = "createTeacher", type = Type.NORMAL,
            precondition = "DB có giáo viên annv@tlu.edu.vn đã bị tắt, role EXAMINER, amount 15, SĐT 0911111111",
            input = "request = {firstName: \"NGUYỄN văn\" (khác hoa/thường), phone: \"0987654321\", amount: null, các trường khác khớp DB}",
            returns = "code = 1000; Account status = true, SĐT 0987654321, role giữ EXAMINER; amount = 20; lưu Teacher",
            log = "Đã kích hoạt lại và cập nhật: Số điện thoại, Số lượng hướng dẫn")
    void UTC062_createTeacher_inactive_reactivateAndUpdate() {
        Role examinerRole = new Role();
        examinerRole.setName("EXAMINER");
        Teacher teacher = existingTeacher(false, examinerRole);
        givenExistingTeacher(teacher);

        // Họ tên khác hoa/thường vẫn coi là khớp hồ sơ
        TeacherCreationResult result = userService.createTeacher(validTeacherRequest().firstName("NGUYỄN văn").build());

        assertTeacherResult(result, 1000, "Đã kích hoạt lại và cập nhật: Số điện thoại, Số lượng hướng dẫn");
        assertThat(teacher.getAccount().getStatus()).isTrue();
        assertThat(teacher.getAccount().getPhone()).isEqualTo("0987654321");
        assertThat(teacher.getAccount().getRoles()).containsExactly(examinerRole);
        assertThat(teacher.getAmount()).isEqualTo(20L);
        assertThat(capturedTeachers()).containsExactly(teacher);
    }

    @Test
    @UnitTestCase(id = "UTC063", method = "createTeacher", type = Type.NORMAL,
            precondition = "DB có giáo viên annv@tlu.edu.vn đã bị tắt, amount 15, SĐT 0911111111",
            input = "request = {phone: \"0911111111\", amount: 15, các trường khác khớp DB}",
            returns = "code = 1000; Account status = true; lưu Teacher",
            log = "Đã kích hoạt lại tài khoản giáo viên")
    void UTC063_createTeacher_inactiveAndSameInfo_onlyReportReactivated() {
        Teacher teacher = existingTeacher(false, supervisorRole);
        givenExistingTeacher(teacher);

        TeacherCreationResult result = userService.createTeacher(validTeacherRequest().phone("0911111111").amount("15").build());

        assertTeacherResult(result, 1000, "Đã kích hoạt lại tài khoản giáo viên");
        assertThat(teacher.getAccount().getStatus()).isTrue();
        assertThat(capturedTeachers()).containsExactly(teacher);
    }

    @Test
    @UnitTestCase(id = "UTC064", method = "createTeacher", type = Type.NORMAL,
            precondition = "majorRepository.findAll trả về 2 ngành (Kỹ thuật phần mềm, Hệ thống thông tin); DB có giáo viên annv@tlu.edu.vn đã tắt, ngành Kỹ thuật phần mềm",
            input = "request = {phone: \"0911111111\", amount: 15, major: \"Hệ thống thông tin\", các trường khác khớp DB}",
            returns = "code = 1000; Teacher đổi sang ngành Hệ thống thông tin",
            log = "Đã kích hoạt lại và cập nhật: Ngành")
    void UTC064_createTeacher_inactiveAndMajorChanged_reportMajor() {
        Major otherMajor = Major.builder().id(2L).name("Hệ thống thông tin").code("7480104").build();
        when(majorRepository.findAll()).thenReturn(List.of(major, otherMajor));
        Teacher teacher = existingTeacher(false, supervisorRole);
        givenExistingTeacher(teacher);

        TeacherCreationResult result = userService.createTeacher(validTeacherRequest()
                .phone("0911111111").amount("15").major("Hệ thống thông tin").build());

        assertTeacherResult(result, 1000, "Đã kích hoạt lại và cập nhật: Ngành");
        assertThat(teacher.getMajor()).isSameAs(otherMajor);
    }

    @Test
    @UnitTestCase(id = "UTC106", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"an.nguyen@gmail.com\", organization: \"Trường Đại học Bách khoa Hà Nội\", major: \"Kỹ thuật phần mềm\", các trường khác hợp lệ}",
            exception = "AppException: MAJOR_NOT_ALLOWED (\"Giáo viên ngoài trường không được điền ngành\"); không truy vấn DB")
    void UTC106_createTeacher_externalWithMajor_throwMajorNotAllowed() {
        assertTeacherError(validTeacherRequest().email(EXTERNAL_EMAIL).organization(OTHER_SCHOOL).build(),
                ErrorCode.MAJOR_NOT_ALLOWED);
        verify(accountRepository, never()).findAllByEmailIn(any());
    }

    @Test
    @UnitTestCase(id = "UTC107", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"annv@tlu.edu.vn\", organization: \"Trường Đại học Thủy lợi\", major: \"\", các trường khác hợp lệ}",
            exception = "AppException: MAJOR_EMPTY (\"Ngành học không được để trống\")")
    void UTC107_createTeacher_internalWithoutMajor_throwMajorEmpty() {
        assertTeacherError(validTeacherRequest().major("").build(), ErrorCode.MAJOR_EMPTY);
    }

    @Test
    @UnitTestCase(id = "UTC108", method = "createTeacher", type = Type.ABNORMAL,
            input = "request = {email: \"sai-email\", major: \"\", các trường khác hợp lệ}",
            exception = "AppException: EMAIL_INVALID (\"Email không đúng định dạng\"); không báo thêm lỗi ngành trống vì chưa xác định được trong/ngoài trường")
    void UTC108_createTeacher_emailInvalidAndMajorEmpty_onlyReportEmail() {
        assertTeacherError(validTeacherRequest().email("sai-email").major("").build(), ErrorCode.EMAIL_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC110", method = "createTeacher", type = Type.BOUNDARY,
            input = "request = {organization: chuỗi 256 ký tự \"a\", các trường khác hợp lệ}",
            exception = "AppException: ORGANIZATION_INVALID (\"Đơn vị công tác chỉ gồm chữ, số, khoảng trắng và các dấu . , - & ( ) ' / (tối đa 255 ký tự)\")")
    void UTC110_createTeacher_organizationTooLong_throwAppException() {
        assertTeacherError(validTeacherRequest().organization("a".repeat(256)).build(), ErrorCode.ORGANIZATION_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC111", method = "createTeacher", type = Type.NORMAL,
            precondition = "Email chưa có trong DB",
            input = "request = {email: \"an.nguyen@gmail.com\", organization: \"Học viện Công nghệ Bưu chính Viễn thông (PTIT) - Cơ sở Hà Nội\", major: \"\", các trường khác hợp lệ}",
            returns = "code = 1000; Teacher lưu đúng tên trường (giữ dấu ngoặc và gạch ngang), major = null",
            log = "Thêm giáo viên thành công")
    void UTC111_createTeacher_externalOrganizationWithPunctuation_createTeacher() {
        String ptit = "Học viện Công nghệ Bưu chính Viễn thông (PTIT) - Cơ sở Hà Nội";
        givenNoAccountInDb();

        TeacherCreationResult result = userService.createTeacher(validTeacherRequest()
                .email(EXTERNAL_EMAIL).organization(ptit).major("").build());

        assertTeacherResult(result, 1000, "Thêm giáo viên thành công");
        Teacher teacher = capturedTeachers().get(0);
        assertThat(teacher.getOrganization()).isEqualTo(ptit);
        assertThat(teacher.getMajor()).isNull();
    }

    @Test
    @UnitTestCase(id = "UTC112", method = "createTeacher", type = Type.NORMAL,
            precondition = "DB có giáo viên ngoài trường an.nguyen@gmail.com đã bị tắt, Trường Đại học Bách khoa Hà Nội, major = null, amount 15, SĐT 0911111111",
            input = "request = {email: \"an.nguyen@gmail.com\", organization: \"Trường Đại học Bách khoa Hà Nội\", major: null, amount: 15, phone: \"0987654321\"}",
            returns = "code = 1000; Account status = true, SĐT 0987654321; major vẫn null (không lỗi NullPointerException)",
            log = "Đã kích hoạt lại và cập nhật: Số điện thoại")
    void UTC112_createTeacher_inactiveExternalWithoutMajor_reactivateNullSafe() {
        Teacher teacher = existingTeacher(false, supervisorRole);
        teacher.getAccount().setEmail(EXTERNAL_EMAIL);
        teacher.setOrganization(OTHER_SCHOOL);
        teacher.setMajor(null);
        givenExistingTeacher(teacher);

        TeacherCreationResult result = userService.createTeacher(validTeacherRequest()
                .email(EXTERNAL_EMAIL).organization(OTHER_SCHOOL).major(null).amount("15").build());

        assertTeacherResult(result, 1000, "Đã kích hoạt lại và cập nhật: Số điện thoại");
        assertThat(teacher.getAccount().getStatus()).isTrue();
        assertThat(teacher.getAccount().getPhone()).isEqualTo("0987654321");
        assertThat(teacher.getMajor()).isNull();
        assertThat(capturedTeachers()).containsExactly(teacher);
    }

    @Test
    @UnitTestCase(id = "UTC065", method = "createTeacher", type = Type.ABNORMAL,
            precondition = "Email chưa có trong DB; roleRepository.findById(\"SUPERVISOR\") trả về Optional.empty()",
            input = "request = {email: \"annv@tlu.edu.vn\", các trường khác hợp lệ}",
            exception = "AppException: ROLE_NOT_EXIST (\"Role không tồn tại\")")
    void UTC065_createTeacher_supervisorRoleMissing_throwAppException() {
        givenNoAccountInDb();
        when(roleRepository.findById("SUPERVISOR")).thenReturn(Optional.empty());

        assertTeacherError(validTeacherRequest().build(), ErrorCode.ROLE_NOT_EXIST);
    }

    // ===== activateTeacher =====

    @Test
    @UnitTestCase(id = "UTC066", method = "activateTeacher", type = Type.NORMAL,
            precondition = "teacherRepository.findByPublicId trả về giáo viên trong trường đã tắt, amount 15, SĐT 0911111111, role SUPERVISOR",
            input = "publicId = <uuid giáo viên>",
            returns = "true; Account status = true, amount = 20, SĐT/đơn vị/ngành/role giữ nguyên; gọi accountRepository.save và teacherRepository.save")
    void UTC066_activateTeacher_internalInactive_activateAndResetAmount20() {
        Teacher teacher = existingTeacher(false, supervisorRole);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        boolean activated = userService.activateTeacher(publicId.toString());

        assertThat(activated).isTrue();
        assertThat(teacher.getAccount().getStatus()).isTrue();
        assertThat(teacher.getAmount()).isEqualTo(20L);
        assertThat(teacher.getAccount().getPhone()).isEqualTo("0911111111");
        assertThat(teacher.getOrganization()).isEqualTo(TLU_NAME);
        assertThat(teacher.getMajor()).isSameAs(major);
        assertThat(teacher.getAccount().getRoles()).containsExactly(supervisorRole);
        verify(accountRepository).save(teacher.getAccount());
        verify(teacherRepository).save(teacher);
    }

    @Test
    @UnitTestCase(id = "UTC067", method = "activateTeacher", type = Type.NORMAL,
            precondition = "teacherRepository.findByPublicId trả về giáo viên ngoài trường (an.nguyen@gmail.com) đã tắt, amount 15",
            input = "publicId = <uuid giáo viên>",
            returns = "true; amount = 10")
    void UTC067_activateTeacher_externalInactive_resetAmount10() {
        Teacher teacher = existingTeacher(false, supervisorRole);
        teacher.getAccount().setEmail(EXTERNAL_EMAIL);
        teacher.setOrganization(OTHER_SCHOOL);
        teacher.setMajor(null);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        assertThat(userService.activateTeacher(publicId.toString())).isTrue();
        assertThat(teacher.getAmount()).isEqualTo(10L);
    }

    @Test
    @UnitTestCase(id = "UTC068", method = "activateTeacher", type = Type.BOUNDARY,
            precondition = "teacherRepository.findByPublicId trả về giáo viên ngoài trường email cũ an.nguyen@outlook.com (tạo trước luật đuôi email) đã tắt",
            input = "publicId = <uuid giáo viên>",
            returns = "true; amount = 10 (coi như ngoài trường)")
    void UTC068_activateTeacher_legacyEmailDomain_treatAsExternal() {
        Teacher teacher = existingTeacher(false, supervisorRole);
        teacher.getAccount().setEmail("an.nguyen@outlook.com");
        teacher.setOrganization(OTHER_SCHOOL);
        teacher.setMajor(null);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        assertThat(userService.activateTeacher(publicId.toString())).isTrue();
        assertThat(teacher.getAmount()).isEqualTo(10L);
    }

    @Test
    @UnitTestCase(id = "UTC069", method = "activateTeacher", type = Type.NORMAL,
            precondition = "teacherRepository.findByPublicId trả về Trưởng bộ môn (role HEAD_OF_DEPARTMENT) đã tắt",
            input = "publicId = <uuid giáo viên>",
            returns = "true; role giữ nguyên HEAD_OF_DEPARTMENT")
    void UTC069_activateTeacher_inactiveHeadOfDepartment_activateKeepRole() {
        Role hodRole = new Role();
        hodRole.setName("HEAD_OF_DEPARTMENT");
        Teacher teacher = existingTeacher(false, hodRole);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        assertThat(userService.activateTeacher(publicId.toString())).isTrue();
        assertThat(teacher.getAccount().getRoles()).containsExactly(hodRole);
    }

    @Test
    @UnitTestCase(id = "UTC070", method = "activateTeacher", type = Type.NORMAL,
            precondition = "teacherRepository.findByPublicId trả về giáo viên đang hoạt động, amount 15",
            input = "publicId = <uuid giáo viên>",
            returns = "false; amount giữ 15; không gọi accountRepository.save và teacherRepository.save")
    void UTC070_activateTeacher_alreadyActive_returnFalse() {
        Teacher teacher = existingTeacher(true, supervisorRole);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        assertThat(userService.activateTeacher(publicId.toString())).isFalse();
        assertThat(teacher.getAmount()).isEqualTo(15L);
        verify(accountRepository, never()).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    @UnitTestCase(id = "UTC071", method = "activateTeacher", type = Type.ABNORMAL,
            precondition = "teacherRepository.findByPublicId trả về Optional.empty()",
            input = "publicId = <uuid không có trong DB>",
            exception = "AppException: TEACHER_NOT_FOUND (\"Giáo viên không tồn tại\")")
    void UTC071_activateTeacher_notFound_throwAppException() {
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateTeacher(publicId.toString()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEACHER_NOT_FOUND);
    }

    @Test
    @UnitTestCase(id = "UTC072", method = "activateTeacher", type = Type.ABNORMAL,
            input = "publicId = \"khong-phai-uuid\"",
            exception = "AppException: TEACHER_NOT_FOUND (\"Giáo viên không tồn tại\"); không gọi teacherRepository")
    void UTC072_activateTeacher_malformedPublicId_throwAppException() {
        assertThatThrownBy(() -> userService.activateTeacher("khong-phai-uuid"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEACHER_NOT_FOUND);
        verifyNoInteractions(teacherRepository);
    }

    // ===== updateTeacherAmount =====

    @Test
    @UnitTestCase(id = "UTC089", method = "updateTeacherAmount", type = Type.NORMAL,
            precondition = "teacherRepository.findByPublicId trả về giáo viên đang hoạt động, amount 15",
            input = "publicId = <uuid giáo viên>, request = {amount: \"30\"}",
            returns = "Teacher lưu amount = 30; gọi teacherRepository.save")
    void UTC089_updateTeacherAmount_activeTeacher_saveNewAmount() {
        Teacher teacher = existingTeacher(true, supervisorRole);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        userService.updateTeacherAmount(publicId.toString(), TeacherAmountRequest.builder().amount("30").build());

        assertThat(teacher.getAmount()).isEqualTo(30L);
        verify(teacherRepository).save(teacher);
    }

    @Test
    @UnitTestCase(id = "UTC090", method = "updateTeacherAmount", type = Type.BOUNDARY,
            precondition = "teacherRepository.findByPublicId trả về giáo viên đang hoạt động, amount 15",
            input = "publicId = <uuid giáo viên>, request = {amount: \" 0 \"}",
            returns = "Teacher lưu amount = 0 (không nhận hướng dẫn)")
    void UTC090_updateTeacherAmount_amountZero_saveZero() {
        Teacher teacher = existingTeacher(true, supervisorRole);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        userService.updateTeacherAmount(publicId.toString(), TeacherAmountRequest.builder().amount(" 0 ").build());

        assertThat(teacher.getAmount()).isZero();
    }

    @Test
    @UnitTestCase(id = "UTC091", method = "updateTeacherAmount", type = Type.BOUNDARY,
            precondition = "teacherRepository.findByPublicId trả về giáo viên đang hoạt động, amount 15",
            input = "publicId = <uuid giáo viên>, request = {amount: \"15\"} (bằng giá trị đang lưu)",
            returns = "Không ném lỗi; amount giữ 15; vẫn gọi teacherRepository.save (báo cập nhật thành công)")
    void UTC091_updateTeacherAmount_sameAmount_stillSuccess() {
        Teacher teacher = existingTeacher(true, supervisorRole);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        userService.updateTeacherAmount(publicId.toString(), TeacherAmountRequest.builder().amount("15").build());

        assertThat(teacher.getAmount()).isEqualTo(15L);
        verify(teacherRepository).save(teacher);
    }

    @Test
    @UnitTestCase(id = "UTC092", method = "updateTeacherAmount", type = Type.ABNORMAL,
            input = "publicId = <uuid giáo viên>, request = {amount: null}",
            exception = "AppException: AMOUNT_NULL (\"Số lượng sinh viên hướng dẫn không được để trống\"); không truy vấn DB")
    void UTC092_updateTeacherAmount_amountNull_throwAppException() {
        assertUpdateAmountError(publicId.toString(), null, ErrorCode.AMOUNT_NULL);
        verify(teacherRepository, never()).findByPublicId(any());
    }

    @Test
    @UnitTestCase(id = "UTC093", method = "updateTeacherAmount", type = Type.ABNORMAL,
            input = "publicId = <uuid giáo viên>, request = {amount: \"-1\"}",
            exception = "AppException: AMOUNT_INVALID (\"Số lượng sinh viên hướng dẫn phải là số nguyên không âm\"); không truy vấn DB")
    void UTC093_updateTeacherAmount_amountNegative_throwAppException() {
        assertUpdateAmountError(publicId.toString(), "-1", ErrorCode.AMOUNT_INVALID);
        verify(teacherRepository, never()).findByPublicId(any());
    }

    @Test
    @UnitTestCase(id = "UTC094", method = "updateTeacherAmount", type = Type.ABNORMAL,
            input = "publicId = <uuid giáo viên>, request = {amount: 5.5}",
            exception = "AppException: AMOUNT_INVALID (\"Số lượng sinh viên hướng dẫn phải là số nguyên không âm\"); không làm tròn")
    void UTC094_updateTeacherAmount_amountDecimal_throwAppException() {
        assertUpdateAmountError(publicId.toString(), "5.5", ErrorCode.AMOUNT_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC095", method = "updateTeacherAmount", type = Type.ABNORMAL,
            precondition = "teacherRepository.findByPublicId trả về giáo viên đã tắt (status = false), amount 15",
            input = "publicId = <uuid giáo viên>, request = {amount: \"30\"}",
            exception = "AppException: TEACHER_INACTIVE (\"Giáo viên không hoạt động trong kì này, không thể cập nhật số lượng hướng dẫn\"); amount giữ 15, không gọi save")
    void UTC095_updateTeacherAmount_inactiveTeacher_throwAppException() {
        Teacher teacher = existingTeacher(false, supervisorRole);
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.of(teacher));

        assertUpdateAmountError(publicId.toString(), "30", ErrorCode.TEACHER_INACTIVE);
        assertThat(teacher.getAmount()).isEqualTo(15L);
        verify(teacherRepository, never()).save(any());
    }

    @Test
    @UnitTestCase(id = "UTC096", method = "updateTeacherAmount", type = Type.ABNORMAL,
            precondition = "teacherRepository.findByPublicId trả về Optional.empty()",
            input = "publicId = <uuid không có trong DB>, request = {amount: \"30\"}",
            exception = "AppException: TEACHER_NOT_FOUND (\"Giáo viên không tồn tại\")")
    void UTC096_updateTeacherAmount_notFound_throwAppException() {
        when(teacherRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        assertUpdateAmountError(publicId.toString(), "30", ErrorCode.TEACHER_NOT_FOUND);
    }

    @Test
    @UnitTestCase(id = "UTC097", method = "updateTeacherAmount", type = Type.ABNORMAL,
            input = "publicId = \"khong-phai-uuid\", request = {amount: \"30\"}",
            exception = "AppException: TEACHER_NOT_FOUND (\"Giáo viên không tồn tại\"); không truy vấn DB")
    void UTC097_updateTeacherAmount_invalidPublicId_throwAppException() {
        assertUpdateAmountError("khong-phai-uuid", "30", ErrorCode.TEACHER_NOT_FOUND);
        verify(teacherRepository, never()).findByPublicId(any());
    }

    // ===== importTeachers =====

    @Test
    @UnitTestCase(id = "UTC073", method = "importTeachers", type = Type.NORMAL,
            precondition = "DB chưa có email nào trong file",
            input = "file teachers.xlsx: dòng 2 trong trường annv@tlu.edu.vn; dòng 3 ngoài trường an.nguyen@gmail.com (ngành trống); dòng 4, 5 trùng email trung@tlu.edu.vn (khác hoa/thường); dòng 6 trống; dòng 7, 8 email \"sai-email\"",
            returns = "totalRows = 6, successCount = 2, warningCount = 0, errorCount = 4; amount dòng 2 = 20, dòng 3 = 10; lưu 2 giáo viên",
            log = "Dòng 2: Thêm giáo viên thành công; dòng 4, 5: Email bị trùng với dòng khác trong file; dòng 7, 8: Email không đúng định dạng")
    void UTC073_importTeachers_mixedRows_savePartially() throws IOException {
        givenNoAccountInDb();
        MockMultipartFile file = buildTeacherExcel(sheet -> {
            writeRow(sheet, 1, internalRow(INTERNAL_EMAIL, "0987654321"));
            writeRow(sheet, 2, externalRow(EXTERNAL_EMAIL, "0912345678"));
            writeRow(sheet, 3, internalRow("trung@tlu.edu.vn", "0987654322"));
            writeRow(sheet, 4, internalRow("TRUNG@tlu.edu.vn", "0987654323"));
            writeRow(sheet, 5, "", " ", "", "", "", "");
            writeRow(sheet, 6, externalRow("sai-email", "0912345679"));
            writeRow(sheet, 7, externalRow("sai-email", "0912345670"));
        });

        TeacherImportResponse response = userService.importTeachers(file);

        assertThat(response.getTotalRows()).isEqualTo(6);
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getWarningCount()).isZero();
        assertThat(response.getErrorCount()).isEqualTo(4);
        List<TeacherImportRowResult> results = response.getRowResults();
        assertThat(results).extracting(TeacherImportRowResult::getRowNumber).containsExactly(2, 3, 4, 5, 7, 8);
        assertThat(results).extracting(TeacherImportRowResult::getAmount).containsExactly(20L, 10L, null, null, null, null);
        assertThat(results.get(0).getMessage()).isEqualTo("Thêm giáo viên thành công");
        assertThat(results.get(2).getMessage()).isEqualTo(ErrorCode.EMAIL_DUPLICATED_IN_FILE.getMessage());
        assertThat(results.get(3).getMessage()).isEqualTo(ErrorCode.EMAIL_DUPLICATED_IN_FILE.getMessage());
        assertThat(results.get(4).getMessage()).isEqualTo(ErrorCode.EMAIL_INVALID.getMessage());
        assertThat(results.get(5).getMessage()).isEqualTo(ErrorCode.EMAIL_INVALID.getMessage());
        assertThat(capturedTeachers()).extracting(Teacher::getAmount).containsExactly(20L, 10L);
    }

    @Test
    @UnitTestCase(id = "UTC074", method = "importTeachers", type = Type.NORMAL,
            precondition = "DB chưa có email nào trong file",
            input = "file teachers.xlsx: dòng 2 annv@tlu.edu.vn + \"trường đại học THỦY LỢI\" + ngành mã 7480103; dòng 3 an.nguyen@gmail.com + \"Trường Đại học Thủy lợi\" + ngành trống; dòng 4 cuong@tlu.edu.vn + \"Khoa CNTT #1\"",
            returns = "Dòng 2 SUCCESS, organization chuẩn hóa \"Trường Đại học Thủy lợi\"; chỉ lưu 1 giáo viên",
            log = "Dòng 3: Đơn vị công tác không khớp với email (...); dòng 4: Đơn vị công tác chỉ gồm chữ, số, khoảng trắng và các dấu (...)")
    void UTC074_importTeachers_organizationVariants_validateAndCanonicalize() throws IOException {
        givenNoAccountInDb();
        MockMultipartFile file = buildTeacherExcel(sheet -> {
            writeRow(sheet, 1, "Nguyễn Văn", "An", INTERNAL_EMAIL, "0987654321", "trường đại học THỦY LỢI", "7480103");
            writeRow(sheet, 2, "Trần Thị", "Bình", EXTERNAL_EMAIL, "0912345678", TLU_NAME, "");
            writeRow(sheet, 3, "Lê", "Cường", "cuong@tlu.edu.vn", "0912345679", "Khoa CNTT #1", "Kỹ thuật phần mềm");
        });

        List<TeacherImportRowResult> results = userService.importTeachers(file).getRowResults();

        assertThat(results.get(0).getStatus()).isEqualTo(RowImportStatus.SUCCESS);
        assertThat(results.get(0).getOrganization()).isEqualTo(TLU_NAME);
        assertThat(results.get(1).getMessage()).isEqualTo(ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL.getMessage());
        assertThat(results.get(2).getMessage()).isEqualTo(ErrorCode.ORGANIZATION_INVALID.getMessage());
        assertThat(capturedTeachers()).extracting(Teacher::getOrganization).containsExactly(TLU_NAME);
    }

    @Test
    @UnitTestCase(id = "UTC075", method = "importTeachers", type = Type.NORMAL,
            precondition = "DB có giáo viên annv@tlu.edu.vn đang hoạt động, amount 15",
            input = "file teachers.xlsx: dòng 2 annv@tlu.edu.vn, Trường Đại học Thủy lợi",
            returns = "warningCount = 1; dòng 2 WARNING, amount = 15 (giá trị trong DB); không gọi teacherRepository.saveAll",
            log = "Thông tin giáo viên đã có trong hệ thống")
    void UTC075_importTeachers_alreadyActive_warnWithDbAmount() throws IOException {
        givenExistingTeacher(existingTeacher(true, supervisorRole));
        MockMultipartFile file = buildTeacherExcel(sheet -> writeRow(sheet, 1, internalRow(INTERNAL_EMAIL)));

        TeacherImportResponse response = userService.importTeachers(file);

        assertThat(response.getWarningCount()).isEqualTo(1);
        TeacherImportRowResult result = response.getRowResults().get(0);
        assertThat(result.getStatus()).isEqualTo(RowImportStatus.WARNING);
        assertThat(result.getMessage()).isEqualTo(ErrorCode.TEACHER_ALREADY_ACTIVE.getMessage());
        assertThat(result.getAmount()).isEqualTo(15L);
        verify(teacherRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC076", method = "importTeachers", type = Type.NORMAL,
            precondition = "DB có giáo viên annv@tlu.edu.vn đã tắt, amount 15, SĐT 0911111111",
            input = "file teachers.xlsx: dòng 2 annv@tlu.edu.vn SĐT 0987654321; dòng 3 email sinh viên 2251172489@e.tlu.edu.vn",
            returns = "Dòng 2 SUCCESS, amount = 20; dòng 3 ERROR",
            log = "Dòng 2: Đã kích hoạt lại và cập nhật: Số điện thoại, Số lượng hướng dẫn; dòng 3: Email giáo viên phải có đuôi @tlu.edu.vn (trong trường) hoặc @gmail.com (ngoài trường)")
    void UTC076_importTeachers_inactiveAndStudentDomain_reactivateAndRejectDomain() throws IOException {
        givenExistingTeacher(existingTeacher(false, supervisorRole));
        MockMultipartFile file = buildTeacherExcel(sheet -> {
            writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
            writeRow(sheet, 2, externalRow("2251172489@e.tlu.edu.vn"));
        });

        List<TeacherImportRowResult> results = userService.importTeachers(file).getRowResults();

        assertThat(results.get(0).getStatus()).isEqualTo(RowImportStatus.SUCCESS);
        assertThat(results.get(0).getMessage()).isEqualTo("Đã kích hoạt lại và cập nhật: Số điện thoại, Số lượng hướng dẫn");
        assertThat(results.get(0).getAmount()).isEqualTo(20L);
        assertThat(results.get(1).getStatus()).isEqualTo(RowImportStatus.ERROR);
        assertThat(results.get(1).getMessage()).isEqualTo(ErrorCode.TEACHER_EMAIL_DOMAIN_INVALID.getMessage());
    }

    @Test
    @UnitTestCase(id = "UTC077", method = "importTeachers", type = Type.BOUNDARY,
            input = "file teachers.xlsx: ô Email của dòng 2 là công thức lỗi =1/0",
            returns = "Dòng 2 status ERROR",
            log = "Email không đúng định dạng")
    void UTC077_importTeachers_formulaErrorCell_readAsInvalid() throws IOException {
        MockMultipartFile file = buildTeacherExcel(sheet -> {
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
    @UnitTestCase(id = "UTC078", method = "importTeachers", type = Type.NORMAL,
            precondition = "DB chưa có email annv@tlu.edu.vn; có file mẫu /templates/teacher_import_template.xlsx",
            input = "File mẫu teacher_import_template.xlsx, điền 1 dòng annv@tlu.edu.vn vào sheet đầu tiên",
            returns = "successCount = 1 (tiêu đề file mẫu được chấp nhận)")
    void UTC078_importTeachers_officialTemplate_acceptHeader() throws IOException {
        givenNoAccountInDb();
        MockMultipartFile file;
        try (InputStream template = getClass().getResourceAsStream("/templates/teacher_import_template.xlsx");
             XSSFWorkbook workbook = new XSSFWorkbook(template)) {
            writeRow(workbook.getSheetAt(0), 1, internalRow(INTERNAL_EMAIL));
            file = toFile(workbook, "teachers.xlsx");
        }

        TeacherImportResponse response = userService.importTeachers(file);

        assertThat(response.getSuccessCount()).isEqualTo(1);
    }

    @Test
    @UnitTestCase(id = "UTC079", method = "importTeachers", type = Type.ABNORMAL,
            input = "file teachers.xlsx: cột E tiêu đề \"Đơn vị\" thay vì \"Đơn vị công tác\"",
            exception = "AppException: FILE_HEADER_INVALID (\"Dòng tiêu đề không đúng với file mẫu\")")
    void UTC079_importTeachers_wrongHeader_throwAppException() throws IOException {
        MockMultipartFile file = buildTeacherExcel(sheet -> {
            sheet.getRow(0).getCell(4).setCellValue("Đơn vị");
            writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
        });

        assertThatThrownBy(() -> userService.importTeachers(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_HEADER_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC080", method = "importTeachers", type = Type.ABNORMAL,
            input = "file teachers.xlsx: thêm cột G \"Số lượng sinh viên\" ngoài file mẫu",
            exception = "AppException: FILE_HEADER_INVALID (\"Dòng tiêu đề không đúng với file mẫu\")")
    void UTC080_importTeachers_extraAmountColumn_throwAppException() throws IOException {
        MockMultipartFile file = buildTeacherExcel(sheet -> {
            sheet.getRow(0).createCell(6).setCellValue("Số lượng sinh viên");
            writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
        });

        assertThatThrownBy(() -> userService.importTeachers(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_HEADER_INVALID);
    }

    @Test
    @UnitTestCase(id = "UTC081", method = "importTeachers", type = Type.ABNORMAL,
            input = "file teachers.xlsx: 2 dòng dữ liệu, cột Ngành của dòng 2-3 bị gộp ô",
            exception = "AppException: FILE_MERGED_CELLS (\"File import không được chứa ô bị gộp\")")
    void UTC081_importTeachers_mergedCells_throwAppException() throws IOException {
        MockMultipartFile file = buildTeacherExcel(sheet -> {
            writeRow(sheet, 1, internalRow(INTERNAL_EMAIL));
            writeRow(sheet, 2, externalRow(EXTERNAL_EMAIL));
            sheet.addMergedRegion(new CellRangeAddress(1, 2, 5, 5));
        });

        assertThatThrownBy(() -> userService.importTeachers(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_MERGED_CELLS);
    }

    @Test
    @UnitTestCase(id = "UTC082", method = "importTeachers", type = Type.ABNORMAL,
            input = "file teachers.xlsx chỉ có dòng tiêu đề",
            exception = "AppException: FILE_NO_DATA (\"File import không có dòng dữ liệu nào\")")
    void UTC082_importTeachers_headerOnly_throwAppException() throws IOException {
        MockMultipartFile file = buildTeacherExcel(sheet -> { });

        assertThatThrownBy(() -> userService.importTeachers(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_NO_DATA);
    }

    @Test
    @UnitTestCase(id = "UTC083", method = "importTeachers", type = Type.ABNORMAL,
            input = "file teachers.xlsx có nội dung text \"không phải excel\"",
            exception = "AppException: FILE_INVALID_FORMAT (\"File import phải có định dạng .xlsx\")")
    void UTC083_importTeachers_fakeXlsx_throwAppException() {
        MockMultipartFile file = new MockMultipartFile("file", "teachers.xlsx", "text/plain",
                "không phải excel".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> userService.importTeachers(file))

                .isInstanceOf(AppException.class)

                .extracting("errorCode")

                .isEqualTo(ErrorCode.FILE_INVALID_FORMAT);
    }

    @Test
    @UnitTestCase(id = "UTC109", method = "importTeachers", type = Type.ABNORMAL,
            input = "file teachers.xlsx: dòng 2 annv@tlu.edu.vn + \"Trường Đại học Bách khoa Hà Nội\" + ngành Kỹ thuật phần mềm",
            returns = "errorCount = 1; dòng 2 ERROR; không gọi teacherRepository.saveAll",
            log = "Đơn vị công tác không khớp với email (email @tlu.edu.vn phải là Trường Đại học Thủy lợi, email @gmail.com phải là trường khác)")
    void UTC109_importTeachers_tluEmailWithExternalOrganization_rejectRow() throws IOException {
        MockMultipartFile file = buildTeacherExcel(sheet ->
                writeRow(sheet, 1, "Nguyễn Văn", "An", INTERNAL_EMAIL, "0987654321", OTHER_SCHOOL, "Kỹ thuật phần mềm"));

        TeacherImportResponse response = userService.importTeachers(file);

        assertThat(response.getErrorCount()).isEqualTo(1);
        TeacherImportRowResult result = response.getRowResults().get(0);
        assertThat(result.getStatus()).isEqualTo(RowImportStatus.ERROR);
        assertThat(result.getMessage()).isEqualTo(ErrorCode.ORGANIZATION_NOT_MATCH_EMAIL.getMessage());
        verify(teacherRepository, never()).saveAll(any());
    }

    @Test
    @UnitTestCase(id = "UTC113", method = "importTeachers", type = Type.NORMAL,
            precondition = "DB có giáo viên ngoài trường an.nguyen@gmail.com tên \"Trần Thị Bình\" đã bị tắt, Trường Đại học Bách khoa Hà Nội, major = null, amount 10, SĐT 0912345678",
            input = "file teachers.xlsx: dòng 2 Trần Thị Bình, an.nguyen@gmail.com, 0912345678, Trường Đại học Bách khoa Hà Nội, ngành trống",
            returns = "Dòng 2 SUCCESS; Account status = true; major vẫn null (không lỗi NullPointerException)",
            log = "Đã kích hoạt lại tài khoản giáo viên")
    void UTC113_importTeachers_inactiveExternalWithoutMajor_reactivateNullSafe() throws IOException {
        Teacher teacher = existingTeacher(false, supervisorRole);
        Account account = teacher.getAccount();
        account.setEmail(EXTERNAL_EMAIL);
        account.setFirstName("Trần Thị");
        account.setLastName("Bình");
        account.setPhone("0912345678");
        teacher.setOrganization(OTHER_SCHOOL);
        teacher.setMajor(null);
        teacher.setAmount(10L);
        givenExistingTeacher(teacher);
        MockMultipartFile file = buildTeacherExcel(sheet -> writeRow(sheet, 1, externalRow(EXTERNAL_EMAIL)));

        TeacherImportRowResult result = userService.importTeachers(file).getRowResults().get(0);

        assertThat(result.getStatus()).isEqualTo(RowImportStatus.SUCCESS);
        assertThat(result.getMessage()).isEqualTo("Đã kích hoạt lại tài khoản giáo viên");
        assertThat(account.getStatus()).isTrue();
        assertThat(teacher.getMajor()).isNull();
        assertThat(capturedTeachers()).containsExactly(teacher);
    }
}
