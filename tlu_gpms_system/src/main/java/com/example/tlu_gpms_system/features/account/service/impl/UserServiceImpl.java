package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.common.enums.AccountStatusFilter;
import com.example.tlu_gpms_system.common.enums.RowImportStatus;
import com.example.tlu_gpms_system.common.utils.excel.DuplicateUtils;
import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportRowResult;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportRowResult;
import com.example.tlu_gpms_system.features.account.entity.Role;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Major;
import com.example.tlu_gpms_system.features.account.entity.Student;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
import com.example.tlu_gpms_system.features.account.repository.MajorRepository;
import com.example.tlu_gpms_system.features.account.repository.RoleRepository;
import com.example.tlu_gpms_system.features.account.repository.StudentRepository;
import com.example.tlu_gpms_system.features.account.repository.TeacherRepository;
import com.example.tlu_gpms_system.features.account.service.UserService;
import com.example.tlu_gpms_system.features.account.service.helper.StudentColumn;
import com.example.tlu_gpms_system.features.account.service.helper.StudentExcelReader;
import com.example.tlu_gpms_system.features.account.service.helper.StudentRowData;
import com.example.tlu_gpms_system.features.account.service.helper.StudentRowValidator;
import com.example.tlu_gpms_system.features.account.service.helper.TeacherColumn;
import com.example.tlu_gpms_system.features.account.service.helper.TeacherExcelReader;
import com.example.tlu_gpms_system.features.account.service.helper.TeacherOrganization;
import com.example.tlu_gpms_system.features.account.service.helper.TeacherRowData;
import com.example.tlu_gpms_system.features.account.service.helper.TeacherRowValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    // Whitelist sort cho danh sách sinh viên: key FE gửi lên -> đường dẫn JPQL (alias s., a., m. trong StudentRepository).
    // Sort theo field ngoài danh sách sẽ làm Spring ghép sai ORDER BY (lỗi 500) nên phải chặn ở đây.
    // Chỉ gồm các cột có trên bảng danh sách (email, khóa nằm trong popup chi tiết nên không sort)
    // Họ tên sắp theo Tên rồi đến Họ, đúng thói quen tra danh sách tiếng Việt
    private static final Map<String, List<String>> STUDENT_SORT_FIELDS = Map.of(
            "studentCode", List.of("s.studentCode"),
            "name", List.of("a.lastName", "a.firstName"),
            "majorName", List.of("m.name"),
            "className", List.of("s.className"));
    // Mặc định: sinh viên đang hoạt động lên trước (khi lọc ALL), sau đó mới thêm lên trước
    private static final Sort STUDENT_DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "a.status", "s.status", "s.id");
    // Chốt thêm s.id để phân trang ổn định khi nhiều dòng trùng giá trị sort (cùng lớp, cùng khóa...)
    private static final Sort.Order STUDENT_SORT_TIE_BREAKER = Sort.Order.desc("s.id");

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AccountRepository accountRepository;
    private final MajorRepository majorRepository;
    private final RoleRepository roleRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public Page<StudentResponse> getAllStudents(String keyword, String batch, UUID majorId, AccountStatusFilter status,
                                                Pageable pageable) {
        AccountStatusFilter statusFilter = status == null ? AccountStatusFilter.ACTIVE : status;
        return studentRepository.findAllStudents(blankToNull(TextNormalizer.normalize(keyword)),
                blankToNull(TextNormalizer.toUpperKey(batch)), majorId, statusFilter.toActive(),
                toStudentPageable(pageable));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public StudentDetailResponse getStudentDetail(String publicId) {
        return studentRepository.findStudentDetail(parsePublicId(publicId, ErrorCode.STUDENT_NOT_FOUND))
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public Page<TeacherResponse> getAllTeachers(String lastName, String organization, String majorName, Pageable pageable) {
        return teacherRepository.findAllTeachers(lastName, organization, majorName, pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public StudentImportResponse importStudents(MultipartFile file) {
        List<StudentRowData> rows = StudentExcelReader.read(file);
        List<RowOutcome> outcomes = processStudentRows(rows);

        List<StudentImportRowResult> rowResults = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            rowResults.add(toRowResult(rows.get(i), outcomes.get(i)));
        }
        return StudentImportResponse.builder()
                .totalRows(rows.size())
                .successCount(count(outcomes, RowImportStatus.SUCCESS))
                .warningCount(count(outcomes, RowImportStatus.WARNING))
                .errorCount(count(outcomes, RowImportStatus.ERROR))
                .rowResults(rowResults)
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public StudentCreationResult createStudent(StudentCreationRequest request) {
        RowOutcome outcome = processStudentRows(List.of(StudentRowData.fromRequest(request))).get(0);
        if (outcome.status() == RowImportStatus.ERROR) {
            throw new AppException(outcome.error());
        }
        if (outcome.status() == RowImportStatus.WARNING) {
            return new StudentCreationResult(outcome.error().getCode(), outcome.error().getMessage());
        }
        return new StudentCreationResult(1000, studentMessage(outcome));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public boolean activateStudent(String publicId) {
        Student student = studentRepository.findByPublicId(parsePublicId(publicId, ErrorCode.STUDENT_NOT_FOUND))
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
        Account account = student.getAccount();
        if (Boolean.TRUE.equals(account.getStatus()) && Boolean.TRUE.equals(student.getStatus())) {
            return false;
        }
        // Chỉ bật trạng thái, giữ nguyên lớp/SĐT. Muốn đổi thì dùng nhập thủ công (cách 2)
        account.setStatus(true);
        student.setStatus(true);
        accountRepository.save(account);
        studentRepository.save(student);
        return true;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public TeacherImportResponse importTeachers(MultipartFile file) {
        List<TeacherRowData> rows = TeacherExcelReader.read(file);
        List<RowOutcome> outcomes = processTeacherRows(rows);

        List<TeacherImportRowResult> rowResults = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            rowResults.add(toRowResult(rows.get(i), outcomes.get(i)));
        }
        return TeacherImportResponse.builder()
                .totalRows(rows.size())
                .successCount(count(outcomes, RowImportStatus.SUCCESS))
                .warningCount(count(outcomes, RowImportStatus.WARNING))
                .errorCount(count(outcomes, RowImportStatus.ERROR))
                .rowResults(rowResults)
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public RowImportStatus createTeacher(TeacherCreationRequest request) {
        RowOutcome outcome = processTeacherRows(List.of(TeacherRowData.fromRequest(request))).get(0);
        if (outcome.status() == RowImportStatus.ERROR) {
            throw new AppException(outcome.error());
        }
        return outcome.status();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public boolean activateTeacher(String publicId) {
        Teacher teacher = teacherRepository.findByPublicId(parsePublicId(publicId, ErrorCode.TEACHER_NOT_FOUND))
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_NOT_FOUND));
        Account account = teacher.getAccount();
        // Đang hoạt động sẵn (gồm cả Trưởng bộ môn): không cập nhật gì
        if (Boolean.TRUE.equals(account.getStatus())) {
            return false;
        }
        // Bật trạng thái và đặt lại số lượng hướng dẫn mặc định cho kì mới theo email (20 / 10).
        // Giữ nguyên SĐT, đơn vị, ngành và role. Muốn đổi thì dùng nhập thủ công (cách 2)
        account.setStatus(true);
        teacher.setAmount(TeacherOrganization.ofEmail(TextNormalizer.toLowerKey(account.getEmail())).getDefaultAmount());
        accountRepository.save(account);
        teacherRepository.save(teacher);
        return true;
    }

    // Phân loại toàn bộ các dòng trên bộ nhớ, cuối cùng mới lưu các dòng hợp lệ
    private List<RowOutcome> processStudentRows(List<StudentRowData> rows) {
        // Query 1: danh mục ngành
        Map<String, Major> majorMap = buildMajorMap(majorRepository.findAll());

        // Bước 1: Trống -> Định dạng/Danh mục -> Trùng trong file
        List<Map<StudentColumn, ErrorCode>> rowErrors = rows.stream()
                .map(row -> StudentRowValidator.validate(row, majorMap))
                .toList();
        markDuplicates(rows, rowErrors, StudentColumn.STUDENT_CODE, StudentRowData::getStudentCode,
                ErrorCode.STUDENT_CODE_DUPLICATED_IN_FILE);
        markDuplicates(rows, rowErrors, StudentColumn.EMAIL, StudentRowData::getEmail,
                ErrorCode.EMAIL_DUPLICATED_IN_FILE);

        Set<String> studentCodes = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rowErrors.get(i).isEmpty()) {
                studentCodes.add(rows.get(i).getStudentCode());
                emails.add(rows.get(i).getEmail());
            }
        }

        // Bước 2: đối chiếu DB, chỉ query 1 lần cho cả file
        Map<String, Student> studentByCode = new HashMap<>();
        Map<String, Account> accountByEmail = new HashMap<>();
        Role studentRole = null;
        if (!studentCodes.isEmpty()) {
            // Query 2, Query 3
            studentRepository.findAllByStudentCodeIn(studentCodes)
                    .forEach(student -> studentByCode.put(student.getStudentCode(), student));
            accountRepository.findAllByEmailIn(emails)
                    .forEach(account -> accountByEmail.put(TextNormalizer.toLowerKey(account.getEmail()), account));
            studentRole = roleRepository.findById(com.example.tlu_gpms_system.common.enums.Role.STUDENT.name())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));
        }

        List<Account> accountsToSave = new ArrayList<>();
        List<Student> studentsToSave = new ArrayList<>();
        List<RowOutcome> outcomes = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            StudentRowData row = rows.get(i);
            Map<StudentColumn, ErrorCode> errors = rowErrors.get(i);
            if (!errors.isEmpty()) {
                outcomes.add(RowOutcome.error(aggregateErrors(errors, ErrorCode.STUDENT_INFO_INVALID)));
                continue;
            }
            Major major = majorMap.get(TextNormalizer.toLowerKey(row.getMajor()));
            outcomes.add(reconcileStudent(row, major,
                    studentByCode.get(row.getStudentCode()),
                    accountByEmail.get(row.getEmail()),
                    studentRole, accountsToSave, studentsToSave));
        }

        if (!accountsToSave.isEmpty()) {
            accountRepository.saveAll(accountsToSave);
            studentRepository.saveAll(studentsToSave);
        }
        return outcomes;
    }

    // 5 trường hợp loại trừ nhau khi đối chiếu với DB
    private RowOutcome reconcileStudent(StudentRowData row, Major major, Student student, Account account, Role studentRole,
                                        List<Account> accountsToSave, List<Student> studentsToSave) {
        // TH1: sinh viên hoàn toàn mới
        if (student == null && account == null) {
            Account newAccount = Account.builder()
                    .email(row.getEmail())
                    .firstName(row.getFirstName())
                    .lastName(row.getLastName())
                    .phone(row.getPhone())
                    .status(true)
                    .roles(new HashSet<>(Set.of(studentRole)))
                    .build();
            Student newStudent = Student.builder()
                    .studentCode(row.getStudentCode())
                    .batch(row.getBatch())
                    .className(row.getClassName())
                    .status(true)
                    .major(major)
                    .account(newAccount)
                    .build();
            accountsToSave.add(newAccount);
            studentsToSave.add(newStudent);
            return RowOutcome.SUCCESS;
        }

        // TH2: mã SV / email thuộc về tài khoản khác
        if (student == null || account == null || !student.getAccount().getId().equals(account.getId())) {
            return RowOutcome.error(ErrorCode.STUDENT_IDENTITY_CONFLICT);
        }

        // TH3: đúng cặp (mã SV, email) nhưng lệch thông tin bất biến
        boolean sameProfile = TextNormalizer.normalize(account.getFirstName()).equals(row.getFirstName())
                && TextNormalizer.normalize(account.getLastName()).equals(row.getLastName())
                && TextNormalizer.normalize(student.getBatch()).equalsIgnoreCase(row.getBatch())
                && student.getMajor().getId().equals(major.getId());
        if (!sameProfile) {
            return RowOutcome.error(ErrorCode.STUDENT_PROFILE_MISMATCH);
        }

        // Thông tin được phép thay đổi theo kì
        List<String> changedFields = new ArrayList<>();
        if (!TextNormalizer.normalize(student.getClassName()).equalsIgnoreCase(row.getClassName())) {
            changedFields.add("Lớp");
        }
        if (!TextNormalizer.normalize(account.getPhone()).equals(row.getPhone())) {
            changedFields.add("Số điện thoại");
        }

        // TH4: đang hoạt động sẵn trong kì này. Không đổi gì thì cảnh báo, có thay đổi thì cập nhật
        boolean active = Boolean.TRUE.equals(account.getStatus()) && Boolean.TRUE.equals(student.getStatus());
        if (active && changedFields.isEmpty()) {
            return RowOutcome.warning(ErrorCode.STUDENT_NO_CHANGE, null);
        }

        // TH5: kích hoạt lại cho kì mới (hoặc TH4 có thay đổi), cập nhật thông tin thay đổi theo kì
        account.setStatus(true);
        account.setPhone(row.getPhone());
        student.setStatus(true);
        student.setClassName(row.getClassName());
        accountsToSave.add(account);
        studentsToSave.add(student);
        return RowOutcome.studentSaved(!active, changedFields);
    }

    // Phân loại toàn bộ các dòng trên bộ nhớ, cuối cùng mới lưu các dòng hợp lệ
    private List<RowOutcome> processTeacherRows(List<TeacherRowData> rows) {
        // Query 1: danh mục ngành
        Map<String, Major> majorMap = buildMajorMap(majorRepository.findAll());

        // Bước 1: Trống -> Định dạng/Danh mục -> Trùng trong file
        List<Map<TeacherColumn, ErrorCode>> rowErrors = rows.stream()
                .map(row -> TeacherRowValidator.validate(row, majorMap))
                .toList();
        markDuplicates(rows, rowErrors, TeacherColumn.EMAIL, TeacherRowData::getEmail,
                ErrorCode.EMAIL_DUPLICATED_IN_FILE);

        Set<String> emails = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rowErrors.get(i).isEmpty()) {
                emails.add(rows.get(i).getEmail());
            }
        }

        // Bước 2: đối chiếu DB theo email, chỉ query 1 lần cho cả file
        Map<String, Account> accountByEmail = new HashMap<>();
        Map<Long, Teacher> teacherByAccountId = new HashMap<>();
        Role supervisorRole = null;
        if (!emails.isEmpty()) {
            // Query 2: tài khoản theo email
            accountRepository.findAllByEmailIn(emails)
                    .forEach(account -> accountByEmail.put(TextNormalizer.toLowerKey(account.getEmail()), account));
            // Query 3: hồ sơ giáo viên của các tài khoản đó
            if (!accountByEmail.isEmpty()) {
                List<Long> accountIds = accountByEmail.values().stream().map(Account::getId).toList();
                teacherRepository.findAllByAccountIdIn(accountIds)
                        .forEach(teacher -> teacherByAccountId.put(teacher.getAccount().getId(), teacher));
            }
            // Query 4: role gán cho giáo viên mới
            supervisorRole = roleRepository.findById(com.example.tlu_gpms_system.common.enums.Role.SUPERVISOR.name())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));
        }

        List<Account> accountsToSave = new ArrayList<>();
        List<Teacher> teachersToSave = new ArrayList<>();
        List<RowOutcome> outcomes = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            TeacherRowData row = rows.get(i);
            Map<TeacherColumn, ErrorCode> errors = rowErrors.get(i);
            if (!errors.isEmpty()) {
                outcomes.add(RowOutcome.error(aggregateErrors(errors, ErrorCode.TEACHER_INFO_INVALID)));
                continue;
            }
            Major major = majorMap.get(TextNormalizer.toLowerKey(row.getMajor()));
            Account account = accountByEmail.get(row.getEmail());
            Teacher teacher = account == null ? null : teacherByAccountId.get(account.getId());
            outcomes.add(reconcileTeacher(row, major, account, teacher, supervisorRole, accountsToSave, teachersToSave));
        }

        if (!accountsToSave.isEmpty()) {
            accountRepository.saveAll(accountsToSave);
            teacherRepository.saveAll(teachersToSave);
        }
        return outcomes;
    }

    // 5 trường hợp loại trừ nhau khi đối chiếu với DB
    private RowOutcome reconcileTeacher(TeacherRowData row, Major major, Account account, Teacher teacher, Role supervisorRole,
                                        List<Account> accountsToSave, List<Teacher> teachersToSave) {
        Long amount = resolveAmount(row);

        // TH1: giáo viên hoàn toàn mới
        if (account == null) {
            Account newAccount = Account.builder()
                    .email(row.getEmail())
                    .firstName(row.getFirstName())
                    .lastName(row.getLastName())
                    .phone(row.getPhone())
                    .status(true)
                    .roles(new HashSet<>(Set.of(supervisorRole)))
                    .build();
            Teacher newTeacher = Teacher.builder()
                    .organization(row.getOrganization())
                    .amount(amount)
                    .major(major)
                    .account(newAccount)
                    .build();
            accountsToSave.add(newAccount);
            teachersToSave.add(newTeacher);
            return RowOutcome.success(amount);
        }

        // TH2: email thuộc tài khoản không phải giáo viên (sinh viên, admin, ...)
        if (teacher == null) {
            return RowOutcome.error(ErrorCode.TEACHER_IDENTITY_CONFLICT);
        }

        // TH3: lệch thông tin bất biến (không phân biệt hoa/thường)
        boolean sameProfile = TextNormalizer.normalize(account.getFirstName()).equalsIgnoreCase(row.getFirstName())
                && TextNormalizer.normalize(account.getLastName()).equalsIgnoreCase(row.getLastName());
        if (!sameProfile) {
            return RowOutcome.error(ErrorCode.TEACHER_PROFILE_MISMATCH);
        }

        // TH4: đang hoạt động sẵn trong kì này (gồm cả Trưởng bộ môn), không cập nhật gì
        if (Boolean.TRUE.equals(account.getStatus())) {
            return RowOutcome.warning(ErrorCode.TEACHER_ALREADY_ACTIVE, teacher.getAmount());
        }

        // TH5: kích hoạt lại cho kì mới, cập nhật thông tin thay đổi theo kì (role giữ nguyên)
        account.setStatus(true);
        account.setPhone(row.getPhone());
        teacher.setOrganization(row.getOrganization());
        teacher.setMajor(major);
        teacher.setAmount(amount);
        accountsToSave.add(account);
        teachersToSave.add(teacher);
        return RowOutcome.success(amount);
    }

    // Nhập thủ công có truyền amount thì dùng, ngược lại gán mặc định theo email (20 / 10)
    private Long resolveAmount(TeacherRowData row) {
        return row.getAmount() != null
                ? row.getAmount()
                : TeacherOrganization.ofEmail(row.getEmail()).getDefaultAmount();
    }

    // 1 lỗi -> lỗi cụ thể; >= 2 lỗi -> thông báo chung
    private <C extends Enum<C>> ErrorCode aggregateErrors(Map<C, ErrorCode> errors, ErrorCode multipleErrors) {
        return errors.size() == 1 ? errors.values().iterator().next() : multipleErrors;
    }

    // Tất cả các dòng bị trùng đều bị gắn lỗi (không ưu tiên dòng đầu tiên).
    // Ô đã lỗi ở bước trước thì không tính trùng
    private <R, C extends Enum<C>> void markDuplicates(List<R> rows, List<Map<C, ErrorCode>> rowErrors,
                                                       C column, Function<R, String> getter, ErrorCode duplicateError) {
        List<String> keys = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            keys.add(rowErrors.get(i).containsKey(column) ? null : TextNormalizer.toLowerKey(getter.apply(rows.get(i))));
        }
        DuplicateUtils.findDuplicateIndexes(keys).forEach(i -> rowErrors.get(i).put(column, duplicateError));
    }

    // Key là tên ngành hoặc mã ngành (viết thường). Tên ngành được ưu tiên: mã ngành không ghi đè key tên
    private Map<String, Major> buildMajorMap(List<Major> majors) {
        Map<String, Major> majorMap = new HashMap<>();
        for (Major major : majors) {
            majorMap.put(TextNormalizer.toLowerKey(major.getName()), major);
        }
        for (Major major : majors) {
            if (major.getCode() != null && !major.getCode().isBlank()) {
                majorMap.putIfAbsent(TextNormalizer.toLowerKey(major.getCode()), major);
            }
        }
        return majorMap;
    }

    private StudentImportRowResult toRowResult(StudentRowData row, RowOutcome outcome) {
        return StudentImportRowResult.builder()
                .rowNumber(row.getRowNumber())
                .studentCode(row.getStudentCode())
                .firstName(row.getFirstName())
                .lastName(row.getLastName())
                .email(row.getEmail())
                .phone(row.getPhone())
                .batch(row.getBatch())
                .className(row.getClassName())
                .majorName(row.getMajor())
                .status(outcome.status())
                .message(outcome.error() == null ? studentMessage(outcome) : outcome.error().getMessage())
                .build();
    }

    // Đổi sort FE gửi lên (studentCode, name...) sang đường dẫn JPQL; không gửi sort thì dùng thứ tự mặc định
    private Pageable toStudentPageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), STUDENT_DEFAULT_SORT);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            List<String> paths = STUDENT_SORT_FIELDS.get(order.getProperty());
            if (paths == null) {
                throw new AppException(ErrorCode.INVALID_SORT_FIELD);
            }
            paths.forEach(path -> orders.add(new Sort.Order(order.getDirection(), path)));
        }
        orders.add(STUDENT_SORT_TIE_BREAKER);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    // publicId sai định dạng cũng coi như không tìm thấy, tránh rơi vào lỗi 500
    private UUID parsePublicId(String publicId, ErrorCode notFound) {
        try {
            return UUID.fromString(publicId);
        } catch (IllegalArgumentException e) {
            throw new AppException(notFound);
        }
    }

    private String blankToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    // Message cho dòng sinh viên lưu thành công: thêm mới / kích hoạt lại / cập nhật field nào
    private String studentMessage(RowOutcome outcome) {
        String changes = String.join(", ", outcome.changedFields());
        if (outcome.reactivated()) {
            return changes.isEmpty()
                    ? "Đã kích hoạt lại tài khoản sinh viên"
                    : "Đã kích hoạt lại và cập nhật: " + changes;
        }
        return changes.isEmpty() ? "Thêm sinh viên thành công" : "Đã cập nhật: " + changes;
    }

    private TeacherImportRowResult toRowResult(TeacherRowData row, RowOutcome outcome) {
        return TeacherImportRowResult.builder()
                .rowNumber(row.getRowNumber())
                .firstName(row.getFirstName())
                .lastName(row.getLastName())
                .email(row.getEmail())
                .phone(row.getPhone())
                .organization(row.getOrganization())
                .majorName(row.getMajor())
                .amount(outcome.amount())
                .status(outcome.status())
                .message(outcome.error() == null ? null : outcome.error().getMessage())
                .build();
    }

    private int count(List<RowOutcome> outcomes, RowImportStatus status) {
        return (int) outcomes.stream().filter(outcome -> outcome.status() == status).count();
    }

    // Kết quả xử lý 1 dòng. error là lỗi (ERROR) hoặc cảnh báo (WARNING); amount chỉ dùng cho giáo viên;
    // reactivated, changedFields chỉ dùng để dựng message cho dòng sinh viên lưu thành công
    private record RowOutcome(RowImportStatus status, ErrorCode error, Long amount,
                              boolean reactivated, List<String> changedFields) {
        static final RowOutcome SUCCESS = success(null);

        static RowOutcome success(Long amount) {
            return new RowOutcome(RowImportStatus.SUCCESS, null, amount, false, List.of());
        }

        static RowOutcome studentSaved(boolean reactivated, List<String> changedFields) {
            return new RowOutcome(RowImportStatus.SUCCESS, null, null, reactivated, changedFields);
        }

        static RowOutcome warning(ErrorCode warning, Long amount) {
            return new RowOutcome(RowImportStatus.WARNING, warning, amount, false, List.of());
        }

        static RowOutcome error(ErrorCode error) {
            return new RowOutcome(RowImportStatus.ERROR, error, null, false, List.of());
        }
    }
}
