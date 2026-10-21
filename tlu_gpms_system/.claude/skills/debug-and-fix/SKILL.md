---
name: debug-and-fix
description: >-
  Sử dụng skill này khi user gặp lỗi, exception, hoặc bug trong hệ thống GPMS.
  Ví dụ: "fix lỗi 500", "sửa lỗi LazyInitializationException", "debug API không trả data",
  "lỗi MapStruct không map được field", "API trả 403", "bị 401 dù đã đăng nhập", "fix bug", "debug error".
  Skill cung cấp quy trình bắt buộc, checklist và pattern xử lý các lỗi thường gặp.
---

# Debug & Fix Guide cho TLU GPMS

## Quy trình bắt buộc
1. **Tái hiện lỗi**: xác định endpoint, request, role của user, response (`code`, `message`, HTTP status) và stack trace.
2. **Đọc log**: tìm exception gốc (`Caused by`) trong console; đối chiếu bảng "Lỗi đặc thù của dự án" bên dưới.
3. **Tìm nguyên nhân gốc** trước khi sửa — không sửa mò, không bọc `try/catch` để nuốt lỗi.
4. **Sửa tối thiểu**: chỉ sửa đúng chỗ gây lỗi, giữ nguyên convention (phân quyền ở ServiceImpl, `AppException` + `ErrorCode`, `@Builder.Default` trên `publicId`).
5. **Viết test hồi quy** tái hiện lỗi (theo skill `write-tests`), chạy `.\mvnw.cmd test -Dtest=<ClassTest>`.
6. **Báo cáo** cho user bằng tiếng Việt: nguyên nhân, file đã sửa, cách kiểm chứng, kết quả test thật.

## Lỗi đặc thù của dự án

| Triệu chứng | Nguyên nhân thường gặp | Cách sửa |
|---|---|---|
| Lỗi validate trả `1003` / 422 "Xác thực thất bại" thay vì message cụ thể | `message` trong annotation validate là chuỗi tiếng Việt hoặc key chưa khai báo trong `ErrorCode` (log: "Không tìm thấy ErrorCode tương ứng với key") | Đổi `message` thành KEY và thêm key vào `ErrorCode` |
| 403 / `2001` dù user có role | Tên role trong `@PreAuthorize` sai so với `common/enums/Role.java` (ví dụ dùng `TEACHER`, `LECTURER`), hoặc dùng `hasAuthority('X')` cho role (authority của role là `ROLE_X`, nên phải viết `hasRole('X')` hoặc `hasAuthority('ROLE_X')`) | Sửa biểu thức trong ServiceImpl; kiểm tra role đã gán cho account trong DB |
| 403 với permission | Permission chưa có trong DB/seed hoặc chưa gán cho role | Kiểm tra `DB_UPDATE/DATA_ROLE_PERMISSION.txt` và bảng role–permission |
| 401 / `2000` dù đã đăng nhập | Thiếu cookie `access_token` (FE không gửi `credentials`), token hết hạn, hoặc token đã bị đưa vào `InvalidateToken` sau logout | Kiểm tra cookie/header, đăng nhập lại; kiểm tra `CustomJwtDecoder` |
| `@PreAuthorize` / `@PostAuthorize` không có tác dụng | Method được gọi nội bộ trong cùng class (`this.method()`) nên không qua proxy, hoặc method không phải `public` | Đặt trên method public của ServiceImpl (không đặt ở Controller) và gọi qua bean |
| `publicId` null / lỗi `Column 'public_id' cannot be null` | Entity tạo bằng `builder()` nhưng field `publicId` thiếu `@Builder.Default` | Thêm `@Builder.Default` trên `publicId` (và các collection có giá trị khởi tạo) |
| `FILE_TOO_LARGE` khi import | File vượt giới hạn upload 50MB | Báo user giảm dung lượng file; không tự tăng giới hạn khi chưa hỏi |
| `FILE_EMPTY` | Request multipart thiếu part `file` | Kiểm tra tên part FE gửi lên khớp `@RequestPart` |
| `400` / `1006` "Yêu cầu không hợp lệ" | Body thiếu, JSON hỏng, field sai kiểu (object / mảng vào field chuỗi), sai `Content-Type`, multipart mất boundary, hoặc tham số query sai kiểu. Các handler này không ghi log | Kiểm tra body và header FE gửi; với `FormData` không tự đặt `Content-Type` |
| `405` / `1007`, `404` / `1005` | Sai HTTP method; URL không khớp API nào (thừa / thiếu dấu `/`, sai đường dẫn) | So lại method và URL với Controller |
| 500 khi gửi `sort` | Field trong `?sort=` không tồn tại trên entity/query (`PropertyReferenceException`) | Dùng đúng tên field; với DTO Projection chỉ sort theo field của entity gốc |
| API chậm, log ra rất nhiều câu SELECT giống nhau | N+1 query do lazy relation bị truy cập trong vòng lặp | Dùng `JOIN FETCH ... IN :list` (mẫu `StudentRepository.findAllByStudentCodeIn`) hoặc DTO Projection |
| Code `1100` trả về với HTTP 200 | Không phải lỗi: là cảnh báo nghiệp vụ (`STUDENT_ALREADY_ACTIVE`) | FE xử lý như cảnh báo |

## Checklist khi gặp lỗi

### 1. Lỗi 500 - Internal Server Error
- [ ] Kiểm tra log console (`SYSTEM_ERROR` 1001 được log với nội dung "Bắt lỗi Controller Advice")
- [ ] `GlobalException` đã bắt riêng: `AppException`, `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `AccessDeniedException`, `MaxUploadSizeExceededException`, `MissingServletRequestPartException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException`, `HttpMediaTypeNotSupportedException`, `MultipartException`, `HttpRequestMethodNotSupportedException`, `NoResourceFoundException` / `NoHandlerFoundException`; mọi exception khác rơi vào `SYSTEM_ERROR` (500) → tìm exception gốc trong log
- [ ] Nếu là `NullPointerException`: kiểm tra field nullable, relationship fetch type, `@Builder.Default`
- [ ] Nếu là `LazyInitializationException`: xem mục 2

### 2. Lỗi Hibernate / JPA
- **LazyInitializationException**:
  - Cách 1: Đảm bảo method service chạy trong transaction (`@Transactional(readOnly = true)` ở class ServiceImpl)
  - Cách 2: Dùng `@Query("SELECT e FROM Entity e JOIN FETCH e.relation WHERE ...")`
  - Cách 3: Dùng DTO Projection, không trả entity ra ngoài service
  - KHÔNG bật `spring.jpa.open-in-view=true` (đã tắt trong dự án)
- **DataIntegrityViolationException**: Kiểm tra unique constraint, not null; kiểm tra trùng trước bằng `existsBy...` và ném `AppException` tương ứng
- **Column name không match**: Kiểm tra `@Column(name = "...")` so với database (`ddl-auto=update` không đổi tên/xóa cột cũ)

### 3. Lỗi MapStruct
- Field không map được: kiểm tra `@Mapping(target = "...", source = "...")`
- Lombok không sinh getter/setter: đảm bảo `lombok-mapstruct-binding` trong `pom.xml`
- `publicId` bị ghi đè khi update: thêm `@Mapping(target = "publicId", ignore = true)` cho `toUpdate`
- Build lại project: `.\mvnw.cmd clean compile`

### 4. Lỗi Security / Authentication
- 401 Unauthorized: Token hết hạn, invalid hoặc đã logout → Kiểm tra `CustomJwtDecoder`, `InvalidateToken`
- 403 Forbidden: Thiếu role/permission → Kiểm tra `@PreAuthorize` / `@PostAuthorize` **trong ServiceImpl** và tên role trong `Role.java`
- Token không đọc được: Kiểm tra `BearerTokenResolver` (cookie `access_token` + header `Authorization`)
- `getAccountInfo()` lỗi: subject JWT phải là `account.id`; account phải đang active

### 5. Lỗi SSE / Notification
- Client không nhận event: Kiểm tra emitterMap, timeout (30 phút)
- Connection bị ngắt: heartbeat chạy mỗi 20 giây, kiểm tra cleanup logic

## Pattern xử lý lỗi trong code

```java
// ĐÚNG: Dùng AppException + ErrorCode cụ thể của entity
throw new AppException(ErrorCode.<ENTITY>_NOT_FOUND);

// SAI: Throw RuntimeException trực tiếp
throw new RuntimeException("Not found");  // ← KHÔNG LÀM THẾ NÀY

// ĐÚNG: Optional + orElseThrow
Entity entity = repository.findByPublicId(publicId)
    .orElseThrow(() -> new AppException(ErrorCode.<ENTITY>_NOT_FOUND));

// ĐÚNG: Kiểm tra tồn tại trước khi tạo
if (repository.existsByEmail(email)) {
    throw new AppException(ErrorCode.EMAIL_EXIST);
}

// ĐÚNG: Kiểm tra sở hữu trong ServiceImpl
if (!owner.getId().equals(authenticationService.getAccountInfo().getId())) {
    throw new AppException(ErrorCode.FORBIDDEN);
}
```

## Lệnh hữu ích khi debug
```bash
# Build lại project (fix lỗi MapStruct)
.\mvnw.cmd clean compile

# Chạy project
.\mvnw.cmd spring-boot:run

# Chạy một test class
.\mvnw.cmd test -Dtest=<ClassName>Test

# Chạy toàn bộ test
.\mvnw.cmd test
```
- SQL log đã bật sẵn (`logging.level.org.hibernate.SQL=DEBUG`). Mức `TRACE` cho `org.hibernate.orm.jdbc.bind` in ra **giá trị tham số** (email, số điện thoại...) → chỉ bật tạm khi debug local, không commit và không dán log đó vào báo cáo.
- KHÔNG in hay trích dẫn giá trị cấu hình trong `application.properties` hay nội dung `oauth2GpmsConfig.json` khi báo cáo lỗi.
