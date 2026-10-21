package com.example.tlu_gpms_system.features.account.service.impl;

import com.example.tlu_gpms_system.common.enums.AccountStatusFilter;
import com.example.tlu_gpms_system.common.enums.RowImportStatus;
import com.example.tlu_gpms_system.common.utils.excel.DuplicateUtils;
import com.example.tlu_gpms_system.common.utils.excel.TextNormalizer;
import com.example.tlu_gpms_system.features.account.dto.request.StudentCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherCreationResult;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentImportRowResult;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherImportRowResult;
import com.example.tlu_gpms_system.features.account.entity.Role;
import com.example.tlu_gpms_system.configuration.exception.AppException;
import com.example.tlu_gpms_system.configuration.exception.ErrorCode;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherAmountRequest;
import com.example.tlu_gpms_system.features.account.dto.request.TeacherCreationRequest;
import com.example.tlu_gpms_system.features.account.dto.response.StudentDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.StudentResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherDetailResponse;
import com.example.tlu_gpms_system.features.account.dto.response.TeacherResponse;
import com.example.tlu_gpms_system.features.account.entity.Account;
import com.example.tlu_gpms_system.features.account.entity.Student;
import com.example.tlu_gpms_system.features.account.entity.Teacher;
import com.example.tlu_gpms_system.features.account.repository.AccountRepository;
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
import com.example.tlu_gpms_system.features.faculty.entity.Major;
import com.example.tlu_gpms_system.features.faculty.repository.MajorRepository;
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
import java.util.Objects;
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

    // Whitelist sort cho danh sách giáo viên (alias t., a., m. trong TeacherRepository), cùng lý do như sinh viên.
    // Chỉ gồm các cột có trên bảng danh sách; họ tên cũng sắp theo Tên rồi đến Họ
    private static final Map<String, List<String>> TEACHER_SORT_FIELDS = Map.of(
            "name", List.of("a.lastName", "a.firstName"),
            "organization", List.of("t.organization"),
            "majorName", List.of("m.name"),
            "amount", List.of("t.amount"));
    // Mặc định: giáo viên đang hoạt động lên trước, sau đó mới thêm lên trước
    private static final Sort TEACHER_DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "a.status", "t.id");
    private static final Sort.Order TEACHER_SORT_TIE_BREAKER = Sort.Order.desc("t.id");

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
                toWhitelistedPageable(pageable, STUDENT_SORT_FIELDS, STUDENT_DEFAULT_SORT, STUDENT_SORT_TIE_BREAKER));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public StudentDetailResponse getStudentDetail(String publicId) {
        return studentRepository.findStudentDetail(parsePublicId(publicId, ErrorCode.STUDENT_NOT_FOUND))
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public Page<TeacherResponse> getAllTeachers(String name, String organizationType, String majorName, Pageable pageable) {
        TeacherOrganization type = TeacherOrganization.fromFilter(organizationType);
        Boolean internal = type == null ? null : type == TeacherOrganization.INTERNAL;
        return teacherRepository.findAllTeachers(blankToNull(TextNormalizer.normalize(name)),
                internal, blankToNull(TextNormalizer.normalize(majorName)),
                toWhitelistedPageable(pageable, TEACHER_SORT_FIELDS, TEACHER_DEFAULT_SORT, TEACHER_SORT_TIE_BREAKER));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public TeacherDetailResponse getTeacherDetail(String publicId) {
        return teacherRepository.findTeacherDetail(parsePublicId(publicId, ErrorCode.TEACHER_NOT_FOUND))
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_NOT_FOUND));
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
            throw new AppException(outcome.error(), outcome.errorMessage());
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
    public TeacherCreationResult createTeacher(TeacherCreationRequest request) {
        RowOutcome outcome = processTeacherRows(List.of(TeacherRowData.fromRequest(request))).get(0);
        if (outcome.status() == RowImportStatus.ERROR) {
            throw new AppException(outcome.error(), outcome.errorMessage());
        }
        if (outcome.status() == RowImportStatus.WARNING) {
            return new TeacherCreationResult(outcome.error().getCode(), outcome.error().getMessage());
        }
        return new TeacherCreationResult(1000, teacherMessage(outcome));
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
        // Email cũ tạo trước khi có luật đuôi email (không phải @tlu.edu.vn / @gmail.com) coi là ngoài trường như luật cũ
        TeacherOrganization organization = Objects.requireNonNullElse(
                TeacherOrganization.ofEmail(TextNormalizer.toLowerKey(account.getEmail())), TeacherOrganization.EXTERNAL);
        account.setStatus(true);
        teacher.setAmount(organization.getDefaultAmount());
        accountRepository.save(account);
        teacherRepository.save(teacher);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public void updateTeacherAmount(String publicId, TeacherAmountRequest request) {
        // Kiểm tra dữ liệu nhập trước, sai thì không truy vấn DB
        String amount = TextNormalizer.normalize(request.getAmount());
        if (amount.isEmpty()) {
            throw new AppException(ErrorCode.AMOUNT_NULL);
        }
        if (!TeacherRowValidator.isValidAmount(amount)) {
            throw new AppException(ErrorCode.AMOUNT_INVALID);
        }
        Teacher teacher = teacherRepository.findByPublicId(parsePublicId(publicId, ErrorCode.TEACHER_NOT_FOUND))
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_NOT_FOUND));
        // Giáo viên bị tắt: kích hoạt lại sẽ đặt lại amount về mặc định, nên không cho sửa trước
        if (!Boolean.TRUE.equals(teacher.getAccount().getStatus())) {
            throw new AppException(ErrorCode.TEACHER_INACTIVE);
        }
        // Không chặn số mới bằng số cũ: vẫn lưu và báo thành công
        teacher.setAmount(Long.valueOf(amount));
        teacherRepository.save(teacher);
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
        markDuplicates(rows, rowErrors, StudentColumn.PHONE, StudentRowData::getPhone,
                ErrorCode.PHONE_DUPLICATED_IN_FILE);

        Set<String> studentCodes = new HashSet<>();
        Set<String> emails = new HashSet<>();
        Set<String> phones = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rowErrors.get(i).isEmpty()) {
                studentCodes.add(rows.get(i).getStudentCode());
                emails.add(rows.get(i).getEmail());
                phones.add(rows.get(i).getPhone());
            }
        }

        // Bước 2: đối chiếu DB, chỉ query 1 lần cho cả file
        Map<String, Student> studentByCode = new HashMap<>();
        Map<String, Account> accountByEmail = new HashMap<>();
        Map<String, List<Account>> accountsByPhone = new HashMap<>();
        Role studentRole = null;
        if (!studentCodes.isEmpty()) {
            // Query 2, Query 3
            studentRepository.findAllByStudentCodeIn(studentCodes)
                    .forEach(student -> studentByCode.put(student.getStudentCode(), student));
            accountRepository.findAllByEmailIn(emails)
                    .forEach(account -> accountByEmail.put(TextNormalizer.toLowerKey(account.getEmail()), account));
            // Query 4: tài khoản đang giữ các SĐT trong file
            accountsByPhone = groupAccountsByPhone(phones);
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
                outcomes.add(rowError(errors, StudentColumn::getLabel));
                continue;
            }
            Major major = majorMap.get(TextNormalizer.toLowerKey(row.getMajor()));
            outcomes.add(reconcileStudent(row, major,
                    studentByCode.get(row.getStudentCode()),
                    accountByEmail.get(row.getEmail()),
                    accountsByPhone, studentRole, accountsToSave, studentsToSave));
        }

        if (!accountsToSave.isEmpty()) {
            accountRepository.saveAll(accountsToSave);
            studentRepository.saveAll(studentsToSave);
        }
        return outcomes;
    }

    // 5 trường hợp loại trừ nhau khi đối chiếu với DB
    private RowOutcome reconcileStudent(StudentRowData row, Major major, Student student, Account account,
                                        Map<String, List<Account>> accountsByPhone, Role studentRole,
                                        List<Account> accountsToSave, List<Student> studentsToSave) {
        // Sinh viên cũ quay lại: mã SV và email thuộc cùng 1 tài khoản, khớp thông tin bất biến -> TH4 / TH5
        boolean sameStudent = student != null && account != null
                && student.getAccount().getId().equals(account.getId())
                && TextNormalizer.normalize(account.getFirstName()).equals(row.getFirstName())
                && TextNormalizer.normalize(account.getLastName()).equals(row.getLastName())
                && TextNormalizer.normalize(student.getBatch()).equalsIgnoreCase(row.getBatch())
                && student.getMajor().getId().equals(major.getId());

        if (!sameStudent) {
            // TH2, TH3: mã SV / email / SĐT trùng với tài khoản đã có -> báo đúng các trường bị trùng
            List<String> duplicatedFields = new ArrayList<>();
            if (student != null) {
                duplicatedFields.add("Mã sinh viên");
            }
            if (account != null) {
                duplicatedFields.add("Email");
            }
            if (isPhoneTakenByOther(row.getPhone(), null, accountsByPhone)) {
                duplicatedFields.add("Số điện thoại");
            }
            if (!duplicatedFields.isEmpty()) {
                return RowOutcome.duplicated(duplicatedFields);
            }

            // TH1: sinh viên hoàn toàn mới
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
        // SĐT sắp ghi vào tài khoản không được thuộc tài khoản khác
        if (isPhoneTakenByOther(row.getPhone(), account, accountsByPhone)) {
            return RowOutcome.duplicated(List.of("Số điện thoại"));
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
        markDuplicates(rows, rowErrors, TeacherColumn.PHONE, TeacherRowData::getPhone,
                ErrorCode.PHONE_DUPLICATED_IN_FILE);

        Set<String> emails = new HashSet<>();
        Set<String> phones = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rowErrors.get(i).isEmpty()) {
                emails.add(rows.get(i).getEmail());
                phones.add(rows.get(i).getPhone());
            }
        }

        // Bước 2: đối chiếu DB theo email, chỉ query 1 lần cho cả file
        Map<String, Account> accountByEmail = new HashMap<>();
        Map<Long, Teacher> teacherByAccountId = new HashMap<>();
        Map<String, List<Account>> accountsByPhone = new HashMap<>();
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
            // Query 4: tài khoản đang giữ các SĐT trong file
            accountsByPhone = groupAccountsByPhone(phones);
            // Query 5: role gán cho giáo viên mới
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
                outcomes.add(rowError(errors, TeacherColumn::getLabel));
                continue;
            }
            Major major = majorMap.get(TextNormalizer.toLowerKey(row.getMajor()));
            Account account = accountByEmail.get(row.getEmail());
            Teacher teacher = account == null ? null : teacherByAccountId.get(account.getId());
            outcomes.add(reconcileTeacher(row, major, account, teacher, accountsByPhone, supervisorRole,
                    accountsToSave, teachersToSave));
        }

        if (!accountsToSave.isEmpty()) {
            accountRepository.saveAll(accountsToSave);
            teacherRepository.saveAll(teachersToSave);
        }
        return outcomes;
    }

    // 5 trường hợp loại trừ nhau khi đối chiếu với DB
    private RowOutcome reconcileTeacher(TeacherRowData row, Major major, Account account, Teacher teacher,
                                        Map<String, List<Account>> accountsByPhone, Role supervisorRole,
                                        List<Account> accountsToSave, List<Teacher> teachersToSave) {
        Long amount = resolveAmount(row);

        // Giáo viên cũ quay lại: email thuộc 1 giáo viên, khớp thông tin bất biến (không phân biệt hoa/thường) -> TH4 / TH5
        boolean sameTeacher = teacher != null
                && TextNormalizer.normalize(account.getFirstName()).equalsIgnoreCase(row.getFirstName())
                && TextNormalizer.normalize(account.getLastName()).equalsIgnoreCase(row.getLastName());

        if (!sameTeacher) {
            // TH2, TH3: email / SĐT trùng với tài khoản đã có -> báo đúng các trường bị trùng
            List<String> duplicatedFields = new ArrayList<>();
            if (account != null) {
                duplicatedFields.add("Email");
            }
            if (isPhoneTakenByOther(row.getPhone(), null, accountsByPhone)) {
                duplicatedFields.add("Số điện thoại");
            }
            if (!duplicatedFields.isEmpty()) {
                return RowOutcome.duplicated(duplicatedFields);
            }

            // TH1: giáo viên hoàn toàn mới
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

        // TH4: đang hoạt động sẵn trong kì này (gồm cả Trưởng bộ môn), không cập nhật gì
        if (Boolean.TRUE.equals(account.getStatus())) {
            return RowOutcome.warning(ErrorCode.TEACHER_ALREADY_ACTIVE, teacher.getAmount());
        }
        // SĐT sắp ghi vào tài khoản không được thuộc tài khoản khác
        if (isPhoneTakenByOther(row.getPhone(), account, accountsByPhone)) {
            return RowOutcome.duplicated(List.of("Số điện thoại"));
        }

        // Thông tin được phép thay đổi theo kì, ghi lại để báo cho TBM biết field nào bị ghi đè
        List<String> changedFields = new ArrayList<>();
        if (!TextNormalizer.normalize(account.getPhone()).equals(row.getPhone())) {
            changedFields.add("Số điện thoại");
        }
        if (!TextNormalizer.normalize(teacher.getOrganization()).equalsIgnoreCase(row.getOrganization())) {
            changedFields.add("Đơn vị công tác");
        }
        // Giáo viên ngoài trường không có ngành (major = null)
        if (!Objects.equals(teacher.getMajor() == null ? null : teacher.getMajor().getId(),
                major == null ? null : major.getId())) {
            changedFields.add("Ngành");
        }
        if (!Objects.equals(teacher.getAmount(), amount)) {
            changedFields.add("Số lượng hướng dẫn");
        }

        // TH5: kích hoạt lại cho kì mới, cập nhật thông tin thay đổi theo kì (role giữ nguyên)
        account.setStatus(true);
        account.setPhone(row.getPhone());
        teacher.setOrganization(row.getOrganization());
        teacher.setMajor(major);
        teacher.setAmount(amount);
        accountsToSave.add(account);
        teachersToSave.add(teacher);
        return RowOutcome.teacherReactivated(amount, changedFields);
    }

    // Nhập thủ công có truyền amount thì dùng, ngược lại gán mặc định theo email (20 / 10).
    // amount đã qua TeacherRowValidator nên chắc chắn là số nguyên >= 0
    private Long resolveAmount(TeacherRowData row) {
        return !row.getAmount().isEmpty()
                ? Long.valueOf(row.getAmount())
                : TeacherOrganization.ofEmail(row.getEmail()).getDefaultAmount();
    }

    // Key là SĐT đã chuẩn hóa. Dùng List vì dữ liệu cũ (trước khi có luật này) có thể đã trùng SĐT giữa nhiều tài khoản
    private Map<String, List<Account>> groupAccountsByPhone(Set<String> phones) {
        Map<String, List<Account>> accountsByPhone = new HashMap<>();
        accountRepository.findAllByPhoneIn(phones).forEach(account -> accountsByPhone
                .computeIfAbsent(TextNormalizer.normalize(account.getPhone()), key -> new ArrayList<>())
                .add(account));
        return accountsByPhone;
    }

    // SĐT bị tài khoản khác giữ (kể cả tài khoản đã tắt, khác role). owner = null khi tạo tài khoản mới
    private boolean isPhoneTakenByOther(String phone, Account owner, Map<String, List<Account>> accountsByPhone) {
        return accountsByPhone.getOrDefault(phone, List.of()).stream()
                .anyMatch(account -> owner == null || !account.getId().equals(owner.getId()));
    }

    // 1 lỗi -> lỗi cụ thể; >= 2 lỗi -> liệt kê các trường sai theo thứ tự cột (errors là EnumMap)
    private <C extends Enum<C>> RowOutcome rowError(Map<C, ErrorCode> errors, Function<C, String> label) {
        if (errors.size() == 1) {
            return RowOutcome.error(errors.values().iterator().next());
        }
        return RowOutcome.invalid(errors.keySet().stream().map(label).toList());
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
                .message(outcome.error() == null ? studentMessage(outcome) : outcome.errorMessage())
                .build();
    }

    // Đổi sort FE gửi lên (name, majorName...) sang đường dẫn JPQL theo whitelist; không gửi sort thì dùng thứ tự mặc định.
    // Dùng chung cho danh sách sinh viên và giáo viên
    private Pageable toWhitelistedPageable(Pageable pageable, Map<String, List<String>> sortFields,
                                           Sort defaultSort, Sort.Order tieBreaker) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), defaultSort);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            List<String> paths = sortFields.get(order.getProperty());
            if (paths == null) {
                throw new AppException(ErrorCode.INVALID_SORT_FIELD);
            }
            paths.forEach(path -> orders.add(new Sort.Order(order.getDirection(), path)));
        }
        orders.add(tieBreaker);
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
        String changes = String.join(", ", outcome.fields());
        if (outcome.reactivated()) {
            return changes.isEmpty()
                    ? "Đã kích hoạt lại tài khoản sinh viên"
                    : "Đã kích hoạt lại và cập nhật: " + changes;
        }
        return changes.isEmpty() ? "Thêm sinh viên thành công" : "Đã cập nhật: " + changes;
    }

    // Message cho dòng giáo viên lưu thành công: thêm mới / kích hoạt lại / kích hoạt lại và cập nhật field nào
    private String teacherMessage(RowOutcome outcome) {
        if (!outcome.reactivated()) {
            return "Thêm giáo viên thành công";
        }
        return outcome.fields().isEmpty()
                ? "Đã kích hoạt lại tài khoản giáo viên"
                : "Đã kích hoạt lại và cập nhật: " + String.join(", ", outcome.fields());
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
                .message(outcome.error() == null ? teacherMessage(outcome) : outcome.errorMessage())
                .build();
    }

    private int count(List<RowOutcome> outcomes, RowImportStatus status) {
        return (int) outcomes.stream().filter(outcome -> outcome.status() == status).count();
    }

    // Kết quả xử lý 1 dòng. error là lỗi (ERROR) hoặc cảnh báo (WARNING); amount chỉ dùng cho giáo viên;
    // reactivated, fields dùng để dựng message: dòng lưu thành công -> các trường được cập nhật,
    // lỗi ACCOUNT_DUPLICATED -> các trường trùng với tài khoản đã có, lỗi ROW_INFO_INVALID -> các trường nhập sai
    private record RowOutcome(RowImportStatus status, ErrorCode error, Long amount,
                              boolean reactivated, List<String> fields) {
        static final RowOutcome SUCCESS = success(null);

        static RowOutcome success(Long amount) {
            return new RowOutcome(RowImportStatus.SUCCESS, null, amount, false, List.of());
        }

        static RowOutcome studentSaved(boolean reactivated, List<String> changedFields) {
            return new RowOutcome(RowImportStatus.SUCCESS, null, null, reactivated, changedFields);
        }

        static RowOutcome teacherReactivated(Long amount, List<String> changedFields) {
            return new RowOutcome(RowImportStatus.SUCCESS, null, amount, true, changedFields);
        }

        static RowOutcome warning(ErrorCode warning, Long amount) {
            return new RowOutcome(RowImportStatus.WARNING, warning, amount, false, List.of());
        }

        static RowOutcome error(ErrorCode error) {
            return new RowOutcome(RowImportStatus.ERROR, error, null, false, List.of());
        }

        static RowOutcome invalid(List<String> invalidFields) {
            return new RowOutcome(RowImportStatus.ERROR, ErrorCode.ROW_INFO_INVALID, null, false, invalidFields);
        }

        static RowOutcome duplicated(List<String> duplicatedFields) {
            return new RowOutcome(RowImportStatus.ERROR, ErrorCode.ACCOUNT_DUPLICATED, null, false, duplicatedFields);
        }

        // Message của lỗi / cảnh báo, VD "Mã sinh viên, Email trùng với tài khoản đã tồn tại trong hệ thống",
        // "Họ, Tên và Số điện thoại không hợp lệ, vui lòng kiểm tra lại"
        String errorMessage() {
            return switch (error) {
                case ACCOUNT_DUPLICATED -> String.join(", ", fields) + " trùng với tài khoản đã tồn tại trong hệ thống";
                case ROW_INFO_INVALID -> String.join(", ", fields.subList(0, fields.size() - 1))
                        + " và " + fields.get(fields.size() - 1) + " không hợp lệ, vui lòng kiểm tra lại";
                default -> error.getMessage();
            };
        }
    }
}
