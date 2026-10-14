---
name: create-feature-module
description: >-
  Sử dụng skill này khi user yêu cầu tạo một feature/module mới trong hệ thống GPMS.
  Ví dụ: "tạo module quản lý đề tài", "thêm feature hội đồng", "xây dựng chức năng chấm điểm".
  Skill này hướng dẫn tạo đầy đủ 6 layer theo kiến trúc chuẩn của dự án.
---

# Tạo Feature Module Mới cho TLU GPMS

## Quy trình tạo (theo đúng thứ tự)

### Bước 1: Tạo Entity
- Đường dẫn: `features/<feature_name>/entity/<EntityName>.java`
- Template:
```java

@Table(name = "<table_name>")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class <EntityName> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin từ bên ngoài
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    UUID publicId = UuidCreator.getTimeOrderedEpoch();

    // === Các field nghiệp vụ ở đây ===

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.publicId = UuidCreator.getTimeOrderedEpoch();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### Bước 2: Tạo Repository
- Đường dẫn: `features/<feature_name>/repository/<EntityName>Repository.java`
- Note:
 + Với các Field không có quá nhiều điều kiện lọc phức tạp và thao tác khoảng 1-2 entity có thể dùng  custom query(@Query) bằng JPQL với Entity rồi mapping qua mapstruct   
 +  Với các Field có số lượng > 5 và có nhiều điều kiện lọc phức tạp thì bắt buộc sử dụng DTO Projection , Query dựa trên constructor của DTO rồi viết mapping trong mapstruct 
- Template:
```java
@Repository
public interface <EntityName>Repository extends JpaRepository<<EntityName>, Long> {
    Optional<<EntityName>> findByPublicId(UUID publicId);
}
```

### Bước 3: Tạo DTOs
- Request: `features/<feature_name>/dto/request/<EntityName>Request.java`
- Response: `features/<feature_name>/dto/response/<EntityName>Response.java`
- Response KHÔNG chứa field `id` (Long), chỉ expose `publicId` (UUID)
- **Validation trong Request**: Thuộc tính `message` PHẢI là **KEY** của `ErrorCode` (ví dụ: `@NotBlank(message = "NAME_EMPTY")`), message tiếng Việt định nghĩa trong `ErrorCode.java`

### Bước 4: Tạo Mapper (MapStruct)
- Đường dẫn: `features/<feature_name>/mapper/<EntityName>Mapping.java`
- Template:
```java
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface <EntityName>Mapping {
    <EntityName> toEntity(<EntityName>Request request);
    void toUpdate(@MappingTarget <EntityName> entity, <EntityName>Request request);
    <EntityName>Response toResponse(<EntityName> entity);
    List<<EntityName>Response> toListResponse(List<<EntityName>> entities);
}
```

### Bước 5: Tạo Service Interface + Implementation
- Interface: `features/<feature_name>/service/<EntityName>Service.java`
- Implementation: `features/<feature_name>/service/impl/<EntityName>ServiceImpl.java`
- Phân quyền: `@PreAuthorize("hasRole('ROLE_NAME')")`
- Dùng `@RequiredArgsConstructor`, `@Service`, `@Transactional`
- Lấy user hiện tại: inject `AuthenticationService`, gọi `getAccountInfo()`
- Throw lỗi: `throw new AppException(ErrorCode.XXX)`

### Bước 6: Tạo Controller
- Đường dẫn: `features/<feature_name>/controller/<EntityName>Controller.java`
- Return luôn là `APIResponse<T>`


### Bước 7: Cập nhật ErrorCode (nếu cần)
- Thêm error code mới vào `configuration/exception/ErrorCode.java`
- Tuân theo format và range code đã quy định

### Bước 8: Viết Test
- Unit test cho Service: `src/test/java/.../service/impl/<EntityName>ServiceImplTest.java`
- Integration test: `src/test/java/.../controller/<EntityName>ControllerIntegrationTest.java`

## Checklist trước khi hoàn thành
- [ ] Entity có `publicId` (UUID) + `createdAt` + `updatedAt`
- [ ] Repository extends `JpaRepository` + có `findByPublicId`
- [ ] DTO Response KHÔNG expose `id` (Long)
- [ ] Mapper dùng `@Mapper(componentModel = "spring")`
- [ ] Service có interface + impl, dùng constructor injection
- [ ] Controller return `APIResponse<T>`, có `@PreAuthorize`
- [ ] ErrorCode được cập nhật nếu có lỗi mới
- [ ] Comment tiếng Việt cho logic quan trọng
