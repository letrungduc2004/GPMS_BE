---
name: api-crud-pattern
description: >-
  Sử dụng skill này khi user yêu cầu tạo CRUD API cho một entity.
  Ví dụ: "tạo API CRUD cho đề tài", "viết REST API quản lý hội đồng", "thêm endpoint create/update/delete".
  Skill cung cấp template chuẩn cho Controller, Service, Repository.
---

# CRUD API Pattern cho TLU GPMS

## Template Controller (CRUD đầy đủ)
```java
@RestController
@RequestMapping("/<endpoint>")
@RequiredArgsConstructor
public class <Entity>Controller {

    private final <Entity>Service service;

    // Lấy danh sách có phân trang
    @GetMapping
    public APIResponse<Page<<Entity>Response>> getAll(Pageable pageable) {
        return APIResponse.<Page<<Entity>Response>>builder()
                .code(1000)
                .data(service.getAll(pageable))
                .build();
    }

    // Lấy chi tiết theo publicId
    @GetMapping("/{publicId}")
    public APIResponse<<Entity>Response> getByPublicId(@PathVariable UUID publicId) {
        return APIResponse.<<Entity>Response>builder()
                .code(1000)
                .data(service.getByPublicId(publicId))
                .build();
    }

    // Tạo mới
    @PostMapping
    public APIResponse<Void> create(@RequestBody @Valid <Entity>Request request) {
        service.create(request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Thêm mới thành công")
                .build();
    }

    // Cập nhật
    @PutMapping("/{publicId}")
    public APIResponse<Void> update(
            @PathVariable UUID publicId,
            @RequestBody @Valid <Entity>Request request) {
        service.update(publicId, request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Cập nhật thành công")
                .build();
    }

    // Xóa
    @DeleteMapping("/{publicId}")
    public APIResponse<Void> delete(@PathVariable UUID publicId) {
        service.delete(publicId);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Xóa thành công")
                .build();
    }
}
```

## Template Service Interface
```java
public interface <Entity>Service {
    Page<<Entity>Response> getAll(Pageable pageable);
    <Entity>Response getByPublicId(UUID publicId);
    void create(<Entity>Request request);
    void update(UUID publicId, <Entity>Request request);
    void delete(UUID publicId);
}
```

## Template Service Implementation

```java
@Service
@RequiredArgsConstructor
public class <Entity>ServiceImpl implements <Entity>Service {

    private final <Entity>Repository repository;
    private final <Entity>Mapping mapping;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public Page<<Entity>Response> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapping::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')")
    public <Entity>Response getByPublicId(UUID publicId) {
        <Entity> entity = repository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapping.toResponse(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void create(<Entity>Request request) {
        <Entity> entity = mapping.toEntity(request);
        repository.save(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void update(UUID publicId, <Entity>Request request) {
        <Entity> entity = repository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        mapping.toUpdate(entity, request);
        repository.save(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(UUID publicId) {
        <Entity> entity = repository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        repository.delete(entity);
    }
}
```

## Lưu ý quan trọng
1. **LUÔN dùng `publicId` (UUID)** trong URL path, KHÔNG BAO GIỜ dùng `id` (Long)
2. **Phân trang**: Dùng `Pageable` của Spring Data, client gửi `?page=0&size=10&sort=createdAt,desc`
3. **Validation**: Dùng `@Valid` trên Controller. Thuộc tính `message` trong các annotation `@NotBlank(message = "KEY")`, `@NotNull(message = "KEY")` PHẢI là **KEY** của `ErrorCode`, message tiếng Việt định nghĩa trong `ErrorCode.java`
4. **Phân quyền**: LUÔN đặt `@PreAuthorize` ở tầng **Service Implementation**, KHÔNG đặt ở Controller
5. **Không trả về `id` (Long)** trong response, chỉ trả `publicId`
