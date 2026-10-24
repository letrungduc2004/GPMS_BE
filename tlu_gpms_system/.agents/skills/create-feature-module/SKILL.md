---
name: create-feature-module
description: >-
  Sử dụng skill này khi user yêu cầu tạo một feature/module MỚI (entity chưa tồn tại) trong hệ thống GPMS.
  Ví dụ: "tạo module quản lý đề tài", "thêm feature hội đồng", "xây dựng chức năng chấm điểm",
  "create new module", "new feature with entity".
  Skill này hướng dẫn tạo đầy đủ các layer theo kiến trúc chuẩn của dự án.
  Nếu entity đã có và chỉ cần thêm API → dùng skill api-crud-pattern.
---

# Tạo Feature Module Mới cho TLU GPMS

## Trước khi code (BẮT BUỘC)
- Đọc skill `gpms-domain-knowledge` và `readme.md/Project_Context.md` để hiểu nghiệp vụ.
- Hỏi user nếu chưa rõ: các field + ràng buộc, quan hệ với entity khác, role/permission được thao tác (tên role lấy từ `common/enums/Role.java`), danh sách trạng thái (enum) nếu có.
- KHÔNG tự bịa enum trạng thái hay luồng duyệt khi chưa được chốt.

## Quy trình tạo (theo đúng thứ tự)

### Bước 1: Tạo Entity
- Đường dẫn: `features/<feature_name>/entity/<EntityName>.java`
- Template:
```java
package com.example.tlu_gpms_system.features.<feature_name>.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

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
    // BẮT BUỘC @Builder.Default, nếu không builder sẽ để publicId = null
    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    UUID publicId = UuidCreator.getTimeOrderedEpoch();

    // === Các field nghiệp vụ ở đây ===

    // Quan hệ: mặc định LAZY
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "<other>_id", nullable = false)
    // <Other> <other>;

    // Collection có giá trị khởi tạo cũng phải có @Builder.Default
    // @Builder.Default
    // @OneToMany(mappedBy = "<entity>")
    // List<<Child>> children = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        // Chỉ set thời gian, KHÔNG gán lại publicId ở đây
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```
- KHÔNG dùng `@Data`. Column name dùng snake_case.

### Bước 2: Tạo Repository
- Đường dẫn: `features/<feature_name>/repository/<EntityName>Repository.java`
- Chọn cách query:
  + **Query đơn giản** (ít field, ít điều kiện lọc, thao tác 1–2 entity): dùng derived query hoặc `@Query` JPQL trả về Entity, rồi map sang Response bằng MapStruct.
  + **Danh sách nhiều field (> 5) hoặc nhiều filter tùy chọn**: BẮT BUỘC dùng **DTO Projection** (`SELECT new <package>.<EntityName>Response(...)`) + `countQuery` riêng khi trả `Page`. Response cần constructor khớp thứ tự field (`@AllArgsConstructor`). Mẫu: `StudentRepository.findAllStudents`.
  + Filter tùy chọn viết theo dạng `(:param IS NULL OR :param = '' OR <điều kiện>)`.
  + **Lấy nhiều entity kèm quan hệ** (import, xử lý hàng loạt): dùng `JOIN FETCH ... WHERE x IN :list` để tránh N+1. Mẫu: `StudentRepository.findAllByStudentCodeIn`.
  + `@Query` được `JOIN` sang entity của feature khác **chỉ để đọc** (DTO Projection, tránh N+1). Ghi dữ liệu của feature khác phải gọi qua `Service` của feature đó.
- Template:
```java
@Repository
public interface <EntityName>Repository extends JpaRepository<<EntityName>, Long> {
    Optional<<EntityName>> findByPublicId(UUID publicId);

    // Ví dụ DTO Projection có phân trang + filter tùy chọn
    @Query(value = """
        SELECT new com.example.tlu_gpms_system.features.<feature_name>.dto.response.<EntityName>Response(
            e.publicId,
            e.name
        )
        FROM <EntityName> e
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """,
    countQuery = """
        SELECT COUNT(e)
        FROM <EntityName> e
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<<EntityName>Response> findAll<EntityName>s(@Param("keyword") String keyword, Pageable pageable);
}
```

### Bước 3: Tạo DTOs
- Request: `features/<feature_name>/dto/request/<EntityName>Request.java`
- Response: `features/<feature_name>/dto/response/<EntityName>Response.java`
- Annotation đầy đủ: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @FieldDefaults(level = AccessLevel.PRIVATE)`; class có chủ đích riêng (value object nội bộ, wrapper chỉ trả về...) được bỏ bớt annotation không cần, kèm comment giải thích (xem mục "Convention DTO" trong `GEMINI.md`)
- Request/Response KHÔNG chứa field `id` (Long), chỉ dùng `publicId` (UUID); tham chiếu entity khác cũng bằng `publicId`
- **Validation trong Request**: Thuộc tính `message` PHẢI là **KEY** của `ErrorCode` (ví dụ: `@NotBlank(message = "NAME_EMPTY")`), message tiếng Việt định nghĩa trong `ErrorCode.java`

### Bước 4: Tạo Mapper (MapStruct)
- Đường dẫn: `features/<feature_name>/mapper/<EntityName>Mapping.java`
- Template:
```java
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface <EntityName>Mapping {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    <EntityName> toEntity(<EntityName>Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    void toUpdate(@MappingTarget <EntityName> entity, <EntityName>Request request);

    <EntityName>Response toResponse(<EntityName> entity);
    List<<EntityName>Response> toListResponse(List<<EntityName>> entities);
}
```

### Bước 5: Tạo Service Interface + Implementation
- Interface: `features/<feature_name>/service/<EntityName>Service.java`
- Implementation: `features/<feature_name>/service/impl/<EntityName>ServiceImpl.java`
- Template chi tiết: xem skill `api-crud-pattern`
- `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)` trên từng method đọc, `@Transactional(rollbackFor = Exception.class)` trên từng method thay đổi dữ liệu; TUYỆT ĐỐI KHÔNG đặt `@Transactional` ở cấp class
- Chỉ inject Repository của chính feature. Cần dữ liệu/nghiệp vụ của feature khác → inject `Service` interface của feature đó; KHÔNG inject Repository hay `save`/`delete` Entity của feature khác
- **Phân quyền đặt ở đây** (KHÔNG ở Controller): `@PreAuthorize("hasRole('<ROLE>')")` / `hasAnyRole(...)` / `hasAuthority('<PERMISSION>')`, và `@PostAuthorize` khi cần kiểm tra trên đối tượng trả về. `<ROLE>` đúng tên trong `Role.java`, không có tiền tố `ROLE_`
- Lấy user hiện tại: inject `AuthenticationService`, gọi `getAccountInfo()`
- Throw lỗi: `throw new AppException(ErrorCode.XXX)`
- Logic phụ trợ riêng feature (đọc Excel, validate từng dòng...) → `service/helper/` (class `final`, constructor `private`, method `static`)
- Trước khi viết hàm tiện ích → kiểm tra `common/utils`: `common/utils/excel/` (`ExcelUtils`, `TextNormalizer`, `DuplicateUtils`), `common/utils/cloudinary/CloudinaryFunction`, `common/utils/mail/MailFuncation` (chi tiết trong `GEMINI.md`, mục "Tái sử dụng code")
- Tính năng import Excel: đọc file bằng `ExcelUtils`, chuẩn hóa từng ô bằng `TextNormalizer`, kiểm tra trùng trong file bằng `DuplicateUtils`; phần riêng của feature (cột, đọc dòng, validate dòng) đặt trong `service/helper/` theo mẫu `features/account/service/helper/Student*`

### Bước 6: Tạo Controller
- Đường dẫn: `features/<feature_name>/controller/<EntityName>Controller.java`
- Return luôn là `APIResponse<T>`; endpoint danh từ số ít (`/topic`)
- KHÔNG đặt `@PreAuthorize`/`@PostAuthorize`, KHÔNG viết logic nghiệp vụ
- TUYỆT ĐỐI KHÔNG dùng `@RequestMapping`; ghi đủ đường dẫn trong từng annotation HTTP method: `@GetMapping("/topic")`, `@PostMapping("/topic")`, `@PutMapping("/topic/{publicId}")`, `@DeleteMapping("/topic/{publicId}")`...
- Danh sách: `@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable`

### Bước 7: Cập nhật ErrorCode và Permission
- Thêm error code mới vào `configuration/exception/ErrorCode.java` theo range đã quy định (xem `GEMINI.md`): `<ENTITY>_NOT_FOUND` (3xxx, NOT_FOUND), key validate (4000, BAD_REQUEST)
- Permission/role mới → thêm vào `DB_UPDATE/DATA_ROLE_PERMISSION.txt` (và `Role.java` nếu là role mới, sau khi hỏi user)

### Bước 8: Viết Test
- Theo skill `write-tests`
- Unit test cho Service: `src/test/java/.../features/<feature_name>/service/impl/<EntityName>ServiceImplTest.java`
- Integration test (khi cần kiểm tra phân quyền/endpoint): `src/test/java/.../features/<feature_name>/controller/<EntityName>ControllerIntegrationTest.java`

### Bước 9: Tài liệu
- Tính năng có luồng nghiệp vụ mới → tạo/cập nhật `readme.md/case_execution/<tinh_nang>.md` (mô tả endpoint, phân quyền, luồng xử lý, ErrorCode)

## Checklist trước khi hoàn thành
- [ ] Entity có `@Builder.Default` trên `publicId` (UUID), có `createdAt` + `updatedAt`, `@PrePersist` không gán lại `publicId`
- [ ] Collection/field có giá trị khởi tạo đều có `@Builder.Default`; quan hệ để `LAZY`
- [ ] Repository extends `JpaRepository` + có `findByPublicId`; danh sách phức tạp dùng DTO Projection + `countQuery`
- [ ] DTO Request/Response KHÔNG expose `id` (Long)
- [ ] DTO khai báo đủ `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @FieldDefaults(level = AccessLevel.PRIVATE)`
- [ ] Mapper dùng `@Mapper(componentModel = "spring")`, ignore `id` và `publicId` khi map từ Request
- [ ] Service có interface + impl, constructor injection, `@Transactional(readOnly = true)` trên từng method đọc, `@Transactional(rollbackFor = Exception.class)` trên từng method ghi, KHÔNG có `@Transactional` ở cấp class
- [ ] Không inject Repository của feature khác; `@Query` JOIN chéo feature chỉ dùng để đọc (DTO Projection)
- [ ] ServiceImpl có `@PreAuthorize` / `@PostAuthorize` trên từng method public, role đúng tên trong `Role.java`
- [ ] Controller trả `APIResponse<T>` và KHÔNG có annotation phân quyền, KHÔNG có `@RequestMapping` (đường dẫn đầy đủ nằm trong `@GetMapping`/`@PostMapping`...)
- [ ] ErrorCode được cập nhật nếu có lỗi mới; permission mới đã có trong `DATA_ROLE_PERMISSION.txt`
- [ ] Comment tiếng Việt cho logic quan trọng
- [ ] `.\mvnw.cmd clean compile` và `.\mvnw.cmd test -Dtest=<EntityName>ServiceImplTest` chạy qua
