---
name: debug-and-fix
description: >-
  Sử dụng skill này khi user gặp lỗi, exception, hoặc bug trong hệ thống GPMS.
  Ví dụ: "fix lỗi 500", "sửa lỗi LazyInitializationException", "debug API không trả data",
  "lỗi MapStruct không map được field".
  Skill cung cấp checklist và pattern xử lý các lỗi thường gặp.
---

# Debug & Fix Guide cho TLU GPMS

## Checklist khi gặp lỗi

### 1. Lỗi 500 - Internal Server Error
- [ ] Kiểm tra log console (đã bật `DEBUG` cho Hibernate SQL)
- [ ] Xem `GlobalException` đã bắt loại exception chưa
- [ ] Nếu là `NullPointerException`: kiểm tra field nullable, relationship fetch type
- [ ] Nếu là `LazyInitializationException`: thêm `@Transactional` hoặc dùng `JOIN FETCH`

### 2. Lỗi Hibernate / JPA
- **LazyInitializationException**: 
  - Cách 1: Thêm `@Transactional` vào method service
  - Cách 2: Dùng `@Query("SELECT e FROM Entity e JOIN FETCH e.relation WHERE ...")`
  - KHÔNG dùng `spring.jpa.open-in-view=true` (đã tắt trong dự án)
- **DataIntegrityViolationException**: Kiểm tra unique constraint, not null
- **Column name không match**: Kiểm tra `@Column(name = "...")` so với database

### 3. Lỗi MapStruct
- Field không map được: kiểm tra `@Mapping(target = "...", source = "...")`
- Lombok không sinh getter/setter: đảm bảo `lombok-mapstruct-binding` trong pom.xml
- Build lại project: `mvn clean compile`

### 4. Lỗi Security / Authentication
- 401 Unauthorized: Token hết hạn hoặc invalid → Kiểm tra `CustomJwtDecoder`
- 403 Forbidden: Thiếu role/permission → Kiểm tra `@PreAuthorize`
- Token không đọc được: Kiểm tra `BearerTokenResolver` (Cookie + Header)

### 5. Lỗi SSE / Notification
- Client không nhận event: Kiểm tra emitterMap, timeout (30 phút)
- Connection bị ngắt: heartbeat chạy mỗi 20 giây, kiểm tra cleanup logic

## Pattern xử lý lỗi trong code

```java
// ĐÚNG: Dùng AppException + ErrorCode
throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);

// SAI: Throw RuntimeException trực tiếp
throw new RuntimeException("Not found");  // ← KHÔNG LÀM THẾ NÀY

// ĐÚNG: Optional + orElseThrow
Entity entity = repository.findByPublicId(publicId)
    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

// ĐÚNG: Kiểm tra tồn tại trước khi tạo
if (repository.existsByField(value)) {
    throw new AppException(ErrorCode.RESOURCE_ALREADY_EXISTS);
}
```

## Lệnh hữu ích khi debug
```bash
# Build lại project (fix lỗi MapStruct)
mvn clean compile

# Chạy project
mvn spring-boot:run

# Chạy test
mvn test

# Xem SQL log: đã bật sẵn trong application.properties
# logging.level.org.hibernate.SQL=DEBUG
# logging.level.org.hibernate.orm.jdbc.bind=TRACE
```
