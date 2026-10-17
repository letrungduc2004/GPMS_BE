# TLU GPMS System - Hệ thống Quản lý Đồ án Trường Đại học Thủy Lợi

## Tổng quan dự án
Đây là hệ thống backend quản lý đồ án tốt nghiệp (Graduation Project Management System) của Trường Đại học Thủy Lợi (TLU).
- **Tech Stack**: Spring Boot 3.3.3, Java 17, MySQL, Spring Security (OAuth2 Resource Server), Spring AI (Gemini), Cloudinary, MapStruct, Lombok
- **Package gốc**: `com.example.tlu_gpms_system`
- **Base URL API**: Chưa có prefix chung, endpoint đặt trực tiếp (ví dụ: `/student`, `/login`)

## Ngôn ngữ
- **Comment trong code**: Tiếng Việt
- **Error message (ErrorCode)**: Tiếng Việt
- **Tên biến, class, method**: Tiếng Anh (theo chuẩn Java)
- **API endpoint URL**: Tiếng Anh, lowercase, dùng dấu gạch ngang nếu cần

## Kiến trúc Feature-based (BẮT BUỘC)
Mỗi feature module PHẢI tuân theo cấu trúc 6 layer:
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
    ├── <Feature>Service.java      # Service interface
    └── impl/
        └── <Feature>ServiceImpl.java  # Service implementation
```

## Convention Entity (BẮT BUỘC)
- Primary key: `Long id` với `@GeneratedValue(strategy = GenerationType.IDENTITY)` — chỉ dùng nội bộ
- Public identifier: `UUID publicId = UuidCreator.getTimeOrderedEpoch()` — expose ra API
- Dùng `@FieldDefaults(level = AccessLevel.PRIVATE)` thay vì khai báo `private` từng field
- Dùng `@Getter`, `@Setter` (hoặc `@Data`), KHÔNG dùng `@Data` cho Entity (tránh vấn đề equals/hashCode)
- Audit fields: `createdAt`, `updatedAt` (dùng `LocalDateTime`)
- Mọi column name dùng snake_case trong `@Column(name = "...")`

## Convention DTO
- Request DTO: `<Feature>Request.java` — dùng `@Getter`, `@Setter`, `@FieldDefaults(level = AccessLevel.PRIVATE)`
- Response DTO: `<Feature>Response.java` — dùng `@Getter`, `@Setter`, `@Builder`, `@FieldDefaults(level = AccessLevel.PRIVATE)`
- KHÔNG expose `id` (Long) ra response, chỉ expose `publicId` (UUID)
- **Validation trong Request DTO (BẮT BUỘC)**:
  + Thuộc tính `message` trong các annotation validate (`@NotBlank`, `@NotNull`, `@Size`, `@Email`...) PHẢI là **KEY** của `ErrorCode` (ví dụ: `@NotBlank(message = "EMAIL_EMPTY")`), KHÔNG ĐƯỢC viết chuỗi tiếng Việt trực tiếp vào annotation.
  + Chuỗi message tiếng Việt, mã code (ví dụ `4000`), và `HttpStatus` được định nghĩa tập trung trong `ErrorCode.java`.
  + *Cơ chế*: `GlobalException` bắt `MethodArgumentNotValidException` và gọi `ErrorCode.valueOf(exception.getFieldError().getDefaultMessage())` để lấy thông tin lỗi.

## Convention Mapper (MapStruct)
- Dùng `@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`
- Interface đặt tên `<Feature>Mapping`
- Dùng `@Mapping` cho field name khác nhau
- Hỗ trợ method `toEntity()`, `toResponse()`, `toListResponse()`, `toUpdate(@MappingTarget ...)`

## Convention Service
- LUÔN tạo interface trước, rồi implementation trong `impl/`
- Inject dependency bằng `@RequiredArgsConstructor` (constructor injection)
- Phân quyền: LUÔN đặt `@PreAuthorize("hasRole('ROLE_NAME')")` hoặc `@PreAuthorize("hasAuthority('...')")` tại tầng Service Implementation (KHÔNG đặt ở Controller)
- Dùng `@Transactional` cho method có write operation
- Dùng `@Transactional(readOnly = true)` cho method chỉ đọc
- Throw exception bằng `throw new AppException(ErrorCode.XXX)`

## Convention Controller
- Dùng `@RestController` + `@RequiredArgsConstructor`
- Return type LUÔN là `APIResponse<T>` (wrapper chung)
- Lấy thông tin user hiện tại qua `AuthenticationService.getAccountInfo()` (khi cần)

## Convention API Response (BẮT BUỘC)
- Wrapper: `APIResponse<T>` với 3 field: `code`, `message`, `data`
- Mã thành công: LUÔN dùng `code = 1000`
- API truy vấn (GET): Trả về `APIResponse<T>` chứa `code(1000)` và `data(...)`
- API thay đổi dữ liệu (POST create, PUT update, DELETE): Trả về `APIResponse<Void>` chứa `code(1000)` và `message("...")` tiếng Việt (ví dụ: "Thêm mới thành công", "Cập nhật thành công", "Xóa thành công"), KHÔNG trả về entity data trong data
- API lỗi: Bắn `AppException(ErrorCode.XXX)`, xử lý tập trung qua `GlobalException` (@ControllerAdvice)

## Convention Notification
- Khi cần gửi thông báo real-time, inject `NotificationService` trực tiếp
- Gọi `notificationService.pushNotification(message, recipientAccount)`
- SSE (Server-Sent Events) đã được cấu hình sẵn

## Convention ErrorCode
- Khi thêm error mới, thêm vào `ErrorCode` enum với format: `ERROR_NAME(code, "message tiếng Việt", HttpStatus.XXX)`
- Code ranges: 1xxx = Common (1000 = Success, 1001 = System Error,...), 2xxx = Security, 3xxx = Business Logic, 4xxx = External Service & Field Validation
- **Cơ chế Validation Exception trong GlobalException**:
  + Bắt `MethodArgumentNotValidException` (cho `@Valid` với DTO đơn lẻ)
  + Bắt `HandlerMethodValidationException` (cho `@Valid` với `List<DTO>` từ Spring Boot 3 / Spring 6.1+)
  + Cả 2 đều trích xuất `defaultMessage` (key) để map sang `ErrorCode.valueOf(key)` trả về `APIResponse` chuẩn.

## Convention Security
- Token: JWT HS256, lưu trong HttpOnly Cookie hoặc Header Authorization
- Token blacklist qua `InvalidateToken` entity
- Lấy user hiện tại: `SecurityContextHolder` → `authentication.getName()` trả về `account.id`

## Roles trong hệ thống
```
ADMIN                  — Quản trị viên hệ thống
HEAD_OF_DEPARTMENT     — Trưởng bộ môn
TEACHER                — Giảng viên (role nền của giảng viên)
STUDENT                — Sinh viên
```

## Convention Testing
- Viết Unit Test cho Service layer (`@ExtendWith(MockitoExtension.class)`)
- Viết Integration Test cho Controller + Repository (`@SpringBootTest`, `@AutoConfigureMockMvc`)
- Test class đặt trong `src/test/java` mirror cùng package
- Naming: `<ClassName>Test.java` cho unit test, `<ClassName>IntegrationTest.java` cho integration test
