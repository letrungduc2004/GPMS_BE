# TLU GPMS System - Hệ thống Quản lý Đồ án Trường Đại học Thủy Lợi

## Tổng quan dự án
Đây là hệ thống backend quản lý đồ án tốt nghiệp (Graduation Project Management System) của Trường Đại học Thủy Lợi (TLU).
- **Tech Stack**: Spring Boot 3.3.3, Java 17, MySQL (JPA, `ddl-auto=update`, `open-in-view=false`), Spring Security (OAuth2 Resource Server, JWT), Spring AI (starter `spring-ai-starter-model-openai`), Cloudinary, Spring Mail, OpenFeign, Apache POI (đọc Excel), MapStruct, Lombok, uuid-creator
- **Package gốc**: `com.example.tlu_gpms_system`
- **Base URL API**: Chưa có prefix chung, endpoint đặt trực tiếp (ví dụ: `/student`, `/login`)
- **Tài liệu nghiệp vụ chi tiết**: `../readme.md/Project_Context.md`, `../readme.md/work_flow/`, `../readme.md/case_execution/`

## Cấu trúc repo
```
Do_An_Fall26/                      # git root
├── readme.md/                     # tài liệu nghiệp vụ (Project_Context, work_flow, case_execution)
├── test_data/                     # file dữ liệu mẫu để test thủ công
└── tlu_gpms_system/               # project Spring Boot — thư mục mở Claude Code
    ├── CLAUDE.md                  # file này
    ├── .claude/skills/            # skill cho Claude Code
    ├── .agents/                   # bộ skill của Antigravity (cùng quy ước với bộ này)
    ├── DB_UPDATE/DATA_ROLE_PERMISSION.txt
    ├── pom.xml, mvnw.cmd
    └── src/main/java/com/example/tlu_gpms_system/
```
- Mọi đường dẫn trong file này và các skill được tính từ `tlu_gpms_system/`. Riêng đường dẫn rút gọn `features/...`, `common/...`, `configuration/...` được tính từ `src/main/java/com/example/tlu_gpms_system/`.
- Tài liệu nghiệp vụ nằm ngoài project, ở `../readme.md/`.

## Skills (`.claude/skills/`)
| Skill | Dùng khi |
|---|---|
| `gpms-domain-knowledge` | Cần hiểu nghiệp vụ: luồng đồ án, role, trạng thái, ràng buộc |
| `create-feature-module` | Tạo feature/entity MỚI (đủ các layer) |
| `api-crud-pattern` | Thêm CRUD API cho entity ĐÃ CÓ |
| `api-request-template` | User mô tả yêu cầu API, cần chuẩn hóa đặc tả trước khi code |
| `write-tests` | Viết test nói chung (điều phối), integration test tự động MockMvc |
| `unit-test-report-5-1` | Viết/chạy unit test theo chuẩn UTC, xuất file mapping báo cáo 5.1 |
| `manual-test-report-5-2-5-3` | Soạn test case thủ công 5.2 Integration / SecurityTest / 5.3 System, ghi kết quả test tay |
| `debug-and-fix` | Gặp lỗi, exception, bug |
- Nội dung `.claude/` và `.agents/` phải giữ đồng bộ: sửa quy ước ở một bên thì cập nhật bên còn lại.

## Quy tắc làm việc của Agent (BẮT BUỘC)
- Luôn trả lời user bằng **tiếng Việt**.
- Nếu nghiệp vụ chưa rõ (role nào được gọi, trạng thái nào hợp lệ, xóa cứng hay xóa mềm...) → **HỎI user trước khi code**, KHÔNG tự bịa nghiệp vụ.
- Đọc code hiện có của feature liên quan trước khi viết code mới, và viết theo đúng style đang dùng.
- Build và test bằng Maven Wrapper: `.\mvnw.cmd clean compile`, `.\mvnw.cmd test` (PowerShell) hoặc `./mvnw ...` (bash).
- Khi thêm permission/role mới → cập nhật thêm `DB_UPDATE/DATA_ROLE_PERMISSION.txt`.
- Khi xong một tính năng → cập nhật tài liệu trong `../readme.md/case_execution/<tinh_nang>.md` (nếu user yêu cầu hoặc tính năng có luồng nghiệp vụ mới).
- KHÔNG tự `git commit` / `git push` khi user chưa yêu cầu.

## An toàn (BẮT BUỘC)
- KHÔNG đọc, in ra, trích dẫn hay commit giá trị trong `application.properties` (client id, secret, mật khẩu, API key...). Cần cấu hình mới thì dùng placeholder `${ENV_NAME}` và báo user tự điền.
- KHÔNG đọc, in ra hay trích dẫn nội dung `oauth2GpmsConfig.json` (cấu hình OAuth2, có thể chứa secret).
- KHÔNG đổi `spring.jpa.hibernate.ddl-auto` và KHÔNG bật `open-in-view`.
- KHÔNG ghi log dữ liệu cá nhân (email, số điện thoại, token) ở mức INFO.

## Ngôn ngữ
- **Comment trong code**: Tiếng Việt
- **Error message (ErrorCode)**: Tiếng Việt
- **Tên biến, class, method**: Tiếng Anh (theo chuẩn Java)
- **API endpoint URL**: Tiếng Anh, lowercase, **danh từ số ít** theo code hiện có (`/student`, `/teacher`, `/role`, `/notification`), dùng dấu gạch ngang nếu cần (`/my-info`, `/refresh-token`)

## Kiến trúc Feature-based (BẮT BUỘC)
Mỗi feature module PHẢI tuân theo cấu trúc sau:
```
features/<feature_name>/
├── controller/          # REST Controller, dùng @RestController
├── dto/
│   ├── request/         # Request DTOs
│   └── response/        # Response DTOs
├── entity/              # JPA Entity
├── mapper/              # MapStruct Mapper interface
├── repository/          # Spring Data JPA Repository
└── service/
    ├── <Feature>Service.java          # Service interface
    ├── helper/                        # (tùy chọn) class hỗ trợ riêng của feature: đọc file, validate từng dòng...
    └── impl/
        └── <Feature>ServiceImpl.java  # Service implementation
```
- `service/helper/`: class `final`, constructor `private`, method `static`, không phải Spring bean (ví dụ: `StudentExcelReader`, `StudentRowValidator`, `StudentColumn`, `StudentRowData` trong `features/account/service/helper`).

### Quy tắc độc lập giữa các feature (Modular Monolith, BẮT BUỘC)
- Các feature đóng gói độc lập.
- CẤM inject chéo Repository hoặc tự ý `save`/`delete` Entity của feature khác.
- Muốn gọi nghiệp vụ hoặc lấy dữ liệu giữa các feature BẮT BUỘC gọi qua `Service` interface (ví dụ: `AccountService` gọi `SemesterService`, không gọi `SemesterRepository`).
- NGOẠI LỆ: được viết `@Query` có `JOIN` sang entity của feature khác ngay ở tầng Repository, **chỉ để đọc** (Read-only DTO Projection), nhằm tối ưu hiệu năng và tránh N+1 Query. Không dùng ngoại lệ này để ghi dữ liệu.

## Tái sử dụng code (BẮT BUỘC kiểm tra trước khi viết mới)
Trước khi viết hàm tiện ích mới, kiểm tra `common/` đã có chưa:
- `common/enums/Role.java` — danh sách role chuẩn
- `common/enums/` — các enum dùng chung (ví dụ `RowImportStatus`)
- `common/utils/excel/` — nhóm tiện ích dùng chung cho import file (Excel và dữ liệu người dùng nhập):
  + `ExcelUtils` — `openXlsx` (kiểm tra rỗng, đuôi `.xlsx`, magic bytes rồi mở workbook; ném `FILE_EMPTY` / `FILE_INVALID_FORMAT`), `hasMergedCells`, `readRow`, `isBlankRow`, `readCellAsText`
  + `TextNormalizer` — `normalize` (xóa ký tự ẩn, gộp khoảng trắng, trim, NFC; `null` → `""`), `toLowerKey`, `toUpperKey` (key so sánh không phân biệt hoa thường)
  + `DuplicateUtils` — `findDuplicateIndexes` (vị trí mọi phần tử có key bị trùng, bỏ qua key `null`)
- `common/utils/cloudinary/CloudinaryFunction` — upload/xóa file Cloudinary
- `common/utils/mail/MailFuncation` — gửi mail
- Hàm dùng được cho nhiều feature → đặt trong nhóm phù hợp của `common/utils/` (tiện ích liên quan import/xử lý dữ liệu file → `common/utils/excel/`); chỉ dùng cho một feature → đặt trong `features/<feature>/service/helper/`.
- KHÔNG tạo lại các package cũ `common/utils/text/` hay `common/utils/collection/` (đã gộp vào `common/utils/excel/`).

## Convention Entity (BẮT BUỘC)
- Annotation chuẩn: `@Table(name = "...") @Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @FieldDefaults(level = AccessLevel.PRIVATE)`
- KHÔNG dùng `@Data` cho Entity (tránh vấn đề equals/hashCode/toString với quan hệ JPA)
- Primary key: `Long id` với `@GeneratedValue(strategy = GenerationType.IDENTITY)` — chỉ dùng nội bộ
- Public identifier (expose ra API) — **BẮT BUỘC có `@Builder.Default`**, nếu thiếu thì `@Builder` sẽ tạo entity với `publicId = null`:
  ```java
  @Builder.Default
  @Column(name = "public_id", nullable = false, unique = true, updatable = false)
  UUID publicId = UuidCreator.getTimeOrderedEpoch();
  ```
- Mọi field có giá trị khởi tạo (collection `= new ArrayList<>()`, `= new HashSet<>()`, giá trị mặc định) cũng phải có `@Builder.Default`
- KHÔNG gán lại `publicId` trong `@PrePersist`
- Audit fields `createdAt`, `updatedAt` (`LocalDateTime`): BẮT BUỘC với entity **mới**; entity cũ chưa có thì không tự ý thêm
- Mọi column name dùng snake_case trong `@Column(name = "...")`
- Quan hệ `@ManyToOne` / `@OneToOne` mặc định đặt `fetch = FetchType.LAZY`

## Convention DTO
- Đặt tên: Request `<Feature>Request.java` hoặc `<Feature>CreationRequest.java`; Response `<Feature>Response.java`
- Annotation chuẩn cho mọi Request/Response DTO (BẮT BUỘC khai báo đầy đủ):
  ```java
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @FieldDefaults(level = AccessLevel.PRIVATE)
  ```
  + `@NoArgsConstructor` + `@Setter`: Jackson cần để đọc JSON vào Request; `@AllArgsConstructor` cần khi dùng `@Builder` cùng `@NoArgsConstructor`
  + DTO Projection: thêm constructor riêng khớp đúng thứ tự field trong `SELECT new ...(...)` nếu query không lấy đủ field (mẫu `StudentResponse`, `TeacherResponse`)
- **Class có chủ đích riêng** (không phải DTO nhận/trả qua API) được tự ý bỏ bớt annotation không cần thiết, và ghi comment tiếng Việt giải thích chủ đích. Ví dụ:
  + Value object bất biến nội bộ: `StudentRowData`, `TeacherRowData` (`service/helper/`) chỉ dùng `@Getter @Builder @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` — tạo một lần qua `fromCells` / `fromRequest`, không bị sửa sau khi chuẩn hóa, không qua Jackson
  + Wrapper chỉ dùng để trả về: `APIResponse` (`@Getter @Setter @Builder`)
  + Khi bỏ annotation, đảm bảo không làm hỏng chỗ đang dùng (Jackson deserialize, MapStruct, `new ...()` trong query)
- KHÔNG expose `id` (Long) ra response/request, chỉ expose `publicId` (UUID)
- **Validation trong Request DTO (BẮT BUỘC)**:
  + Thuộc tính `message` trong các annotation validate (`@NotBlank`, `@NotNull`, `@Size`, `@Email`...) PHẢI là **KEY** của `ErrorCode` (ví dụ: `@NotBlank(message = "EMAIL_EMPTY")`), KHÔNG ĐƯỢC viết chuỗi tiếng Việt trực tiếp vào annotation.
  + Chuỗi message tiếng Việt, mã code và `HttpStatus` được định nghĩa tập trung trong `ErrorCode.java`.
  + *Cơ chế*: `GlobalException` lấy `defaultMessage` rồi gọi `ErrorCode.valueOf(key)`. Nếu key sai chính tả / chưa khai báo → trả về `VALIDATION_FAIL` (1003, 422) và log "Không tìm thấy ErrorCode tương ứng với key". Thêm validate mới thì PHẢI thêm key tương ứng vào `ErrorCode`.

## Convention Mapper (MapStruct)
- Dùng `@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`
- Interface đặt tên `<Feature>Mapping`
- Dùng `@Mapping` cho field name khác nhau; `@Mapping(target = "id", ignore = true)` và `@Mapping(target = "publicId", ignore = true)` khi map Request → Entity
- Hỗ trợ method `toEntity()`, `toResponse()`, `toListResponse()`, `toUpdate(@MappingTarget ...)`

## Convention Service
- LUÔN tạo interface trước, rồi implementation trong `impl/`
- Inject dependency bằng `@RequiredArgsConstructor` (constructor injection)
- **Transaction**:
  + Method đọc dữ liệu (get/list/search): `@Transactional(readOnly = true)`. Đặt ở **cấp class** ServiceImpl để mọi method đọc tự áp dụng.
  + Mọi method thay đổi dữ liệu (create/update/delete/import/activate...): BẮT BUỘC `@Transactional(rollbackFor = Exception.class)` trên từng method.
  + KHÔNG dùng `@Transactional` trơn cho method ghi: mặc định Spring chỉ rollback với `RuntimeException`/`Error`, checked exception (ví dụ `IOException` khi đọc file) sẽ vẫn commit phần dữ liệu đã ghi.
- Throw exception bằng `throw new AppException(ErrorCode.XXX)`
- Lấy user hiện tại qua `AuthenticationService.getAccountInfo()` (trong Service, không truyền account từ Controller xuống)

## Phân quyền (BẮT BUỘC)
- `@PreAuthorize` và `@PostAuthorize` **LUÔN đặt ở tầng Service Implementation**, trên từng method public. **KHÔNG BAO GIỜ đặt ở Controller.**
- `@EnableMethodSecurity` đã bật trong `SecurityConfiguration`.
- Authority trong JWT (claim `authorities`) gồm `ROLE_<ROLE>` và tên permission, nên:
  + Theo role: `@PreAuthorize("hasRole('HEAD_OF_DEPARTMENT')")` — viết tên role **không có** tiền tố `ROLE_`
  + Nhiều role: `@PreAuthorize("hasAnyRole('ADMIN', 'HEAD_OF_DEPARTMENT')")`
  + Theo permission: `@PreAuthorize("hasAuthority('<PERMISSION_NAME>')")`
- Tên role PHẢI lấy từ `common/enums/Role.java` (xem mục Roles bên dưới), KHÔNG tự đặt tên khác.
- `@PostAuthorize`: dùng khi cần kiểm tra trên **đối tượng trả về**, ví dụ chỉ chủ sở hữu hoặc ADMIN được xem:
  ```java
  @PostAuthorize("hasRole('ADMIN') or returnObject.ownerAccountId == authentication.name")
  ```
  `authentication.name` là `account.id` dạng String. Logic sở hữu phức tạp (GVHD chỉ thấy SV mình hướng dẫn...) → kiểm tra bằng `getAccountInfo()` trong Service và ném `AppException(ErrorCode.FORBIDDEN)`.
- Unit test Mockito KHÔNG kích hoạt `@PreAuthorize`/`@PostAuthorize`; muốn test phân quyền phải viết integration test.

## Convention Controller
- Dùng `@RestController` + `@RequiredArgsConstructor`
- Return type LUÔN là `APIResponse<T>` (wrapper chung)
- KHÔNG đặt `@PreAuthorize`/`@PostAuthorize`, KHÔNG viết logic nghiệp vụ ở Controller
- Phân trang: `@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable`

## Convention API Response (BẮT BUỘC)
- Wrapper: `APIResponse<T>` với 3 field: `code`, `message`, `data` (`@JsonInclude(NON_NULL)`)
- Mã thành công: dùng `code = 1000`
- API truy vấn (GET): Trả về `APIResponse<T>` chứa `code(1000)` và `data(...)`
- API thay đổi dữ liệu (POST create, PUT update, DELETE): Trả về `APIResponse<Void>` chứa `code(1000)` và `message("...")` tiếng Việt (ví dụ: "Thêm mới thành công", "Cập nhật thành công", "Xóa thành công"), KHÔNG trả về entity trong data
- **Ngoại lệ**:
  + API import/xử lý hàng loạt được trả `data` là kết quả xử lý từng dòng (ví dụ `POST /student/import` trả `StudentImportResponse`, message "Xử lý file import hoàn tất")
  + Kết quả nghiệp vụ dạng cảnh báo (không phải lỗi) dùng mã `11xx` với HTTP 200 (ví dụ `STUDENT_ALREADY_ACTIVE` 1100)
- API lỗi: Bắn `AppException(ErrorCode.XXX)`, xử lý tập trung qua `GlobalException` (@ControllerAdvice)

## Convention Notification
- Khi cần gửi thông báo real-time, inject `NotificationService` trực tiếp
- Gọi `notificationService.pushNotification(message, recipientAccount)`
- SSE (Server-Sent Events) đã được cấu hình sẵn (timeout 30 phút, heartbeat 20 giây)

## Convention ErrorCode
- Khi thêm error mới, thêm vào `ErrorCode` enum với format: `ERROR_NAME(code, "message tiếng Việt", HttpStatus.XXX)`
- Code ranges hiện tại:
  + `1000` = Success; `1001–1007` = lỗi chung (`SYSTEM_ERROR`, `VALIDATION_FAIL`, `RESOURCE_NOT_FOUND`...)
  + `11xx` = cảnh báo nghiệp vụ, HTTP 200 (`STUDENT_ALREADY_ACTIVE` 1100)
  + `2xxx` = Security / Role / Permission (`UNAUTHORIZED` 2000, `FORBIDDEN` 2001...)
  + `3xxx` = Business Logic (`EMAIL_EXIST` 3001 ... `ACCOUNT_DUPLICATED` 3014) — lỗi nghiệp vụ mới lấy số tiếp theo, KHÔNG trùng số đã có
  + `4000` = Field validation, lỗi file import và External Service (Mail, Cloudinary)
- Lỗi không tìm thấy của entity mới: thêm `<ENTITY>_NOT_FOUND` (3xxx, `HttpStatus.NOT_FOUND`) thay vì dùng chung `RESOURCE_NOT_FOUND`
- **Cơ chế Exception trong GlobalException**:
  + `MethodArgumentNotValidException` (cho `@Valid` với DTO đơn lẻ) và `HandlerMethodValidationException` (cho `@Valid` với `List<DTO>`) → map `defaultMessage` sang `ErrorCode.valueOf(key)`
  + `AppException` → trả theo `errorCode`
  + `AccessDeniedException` → `FORBIDDEN` (2001, 403)
  + `MaxUploadSizeExceededException` → `FILE_TOO_LARGE`; `MissingServletRequestPartException` → `FILE_EMPTY`
  + Lỗi request sai: `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException` (body thiếu / JSON hỏng / field sai kiểu), `HttpMediaTypeNotSupportedException`, `MultipartException` → `INVALID_REQUEST` (1006, 400)
  + `HttpRequestMethodNotSupportedException` → `OPERATION_NOT_ALLOWED` (1007, 405); `NoResourceFoundException` / `NoHandlerFoundException` (URL không khớp API nào) → `RESOURCE_NOT_FOUND` (1005, 404)
  + `Exception` còn lại → `SYSTEM_ERROR` (1001, 500)

## Convention Security
- Token: JWT HS256, đọc từ cookie `access_token` hoặc header `Authorization: Bearer ...`
- Subject của JWT = `account.id`; claim `authorities` chứa `ROLE_<ROLE>` + tên permission
- Token blacklist qua `InvalidateToken` entity
- Lấy user hiện tại: `AuthenticationService.getAccountInfo()` (bên trong dùng `authentication.getName()` = `account.id`)

## Roles trong hệ thống
Nguồn chuẩn duy nhất: `src/main/java/com/example/tlu_gpms_system/common/enums/Role.java`
```
ADMIN                  — Quản trị viên hệ thống
HEAD_OF_DEPARTMENT     — Trưởng bộ môn (TBM)
SECRETARY              — Thư ký hội đồng
CHAIRPERSON            — Chủ tịch hội đồng
COUNCIL_MEMBER         — Ủy viên hội đồng
SUPERVISOR             — Giáo viên hướng dẫn (GVHD)
EXAMINER               — Giáo viên phản biện (GVPB)
COMMENTING_LECTURER    — Giáo viên nhận xét (GVNX)
STUDENT                — Sinh viên
```
- KHÔNG có role `TEACHER` / `LECTURER` / `ADVISOR` / `OPPONENT` / `COUNCIL_CHAIR`... Không dùng các tên này trong `@PreAuthorize`.
- Hiện tại khi TBM thêm giáo viên, hệ thống gán role `SUPERVISOR`.
- Cần role mới → hỏi user, rồi thêm vào `Role.java` và `DB_UPDATE/DATA_ROLE_PERMISSION.txt`.

## Convention Testing
- Chi tiết: xem skill `write-tests` (điều phối), `unit-test-report-5-1`, `manual-test-report-5-2-5-3`
- Unit Test (BẮT BUỘC chuẩn UTC): `@ExtendWith({MockitoExtension.class, UtcReportExtension.class})`, mỗi `@Test` gắn `@UnitTestCase` với mã `UTC###` riêng, KHÔNG dùng `@Nested`/`@ParameterizedTest`, assert bằng AssertJ
- Integration Test cho Controller (`@SpringBootTest`, `@AutoConfigureMockMvc`) — cần MySQL đang chạy
- Test class đặt trong `src/test/java` mirror cùng package
- Naming class: `<ClassName>Test.java` cho unit test, `<ClassName>IntegrationTest.java` cho integration test
- Naming method unit test: `UTC004_<method>_<tình huống>_<kết quả>` (ví dụ `UTC012_createTeachers_emailExist_throwAppException`)
- Test fail là kết quả hợp lệ cho báo cáo: KHÔNG sửa assertion, thêm `@Disabled` hay sửa code production để test xanh khi user chưa đồng ý
- Chạy test: `.\mvnw.cmd test "-Dtest=<ClassName>" "-Dutc.round=1" "-Dutc.tester=<Tên>" "-Dutc.outDir=../readme.md/test_report/Unit_Test_Report"`
- Thư mục file Excel báo cáo test (ở git root `readme.md/test_report/`, từ `tlu_gpms_system/` là `../readme.md/test_report/`, có commit):
  + 5.1 Unit Test: `Unit_Test_Report/` (mặc định trong `UtcReportExtension`, không cần truyền `-Dutc.outDir`)
  + 5.2 Integration Test (kể cả sheet SecurityTest): `Integration_Test_Report/` (TSV + xlsx)
  + 5.3 System Test: `System_Test_Report/` (TSV + xlsx)
