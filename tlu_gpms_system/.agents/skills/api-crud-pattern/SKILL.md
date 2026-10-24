---
name: api-crud-pattern
description: >-
  Sử dụng skill này khi user yêu cầu viết CRUD API / REST endpoint cho một entity ĐÃ CÓ trong hệ thống GPMS.
  Ví dụ: "tạo API CRUD cho đề tài", "viết REST API quản lý hội đồng", "thêm endpoint create/update/delete",
  "add CRUD endpoint", "list/get/create/update/delete API".
  Nếu entity chưa tồn tại → dùng skill create-feature-module trước.
  Skill cung cấp template chuẩn cho Controller, Service, ServiceImpl (phân quyền ở Service).
---

# CRUD API Pattern cho TLU GPMS

## Trước khi code (BẮT BUỘC hỏi user nếu chưa rõ)
1. **Phân quyền**: role nào (lấy từ `common/enums/Role.java`) hoặc permission nào được gọi từng API? Có cần kiểm tra sở hữu không (ví dụ SUPERVISOR chỉ thấy SV mình hướng dẫn)?
2. **Xóa**: xóa cứng (`repository.delete`) hay xóa mềm (đổi `status`)?
3. **Danh sách**: có filter/tìm kiếm gì? Bao nhiêu field trả về? (quyết định dùng `findAll().map` hay DTO Projection)
4. **ErrorCode**: entity đã có `<ENTITY>_NOT_FOUND` chưa? Có lỗi nghiệp vụ nào mới?

## Template Controller (CRUD đầy đủ)
- KHÔNG đặt `@PreAuthorize` / `@PostAuthorize` ở Controller.
- Endpoint là danh từ số ít, ví dụ `/topic`.
- TUYỆT ĐỐI KHÔNG dùng `@RequestMapping` (cả cấp class lẫn method): ghi đầy đủ đường dẫn trong từng `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`.
```java
@RestController
@RequiredArgsConstructor
public class <Entity>Controller {

    private final <Entity>Service service;

    // Lấy danh sách có phân trang
    @GetMapping("/<endpoint>")
    public APIResponse<Page<<Entity>Response>> getAll(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return APIResponse.<Page<<Entity>Response>>builder()
                .code(1000)
                .data(service.getAll(pageable))
                .build();
    }

    // Lấy chi tiết theo publicId
    @GetMapping("/<endpoint>/{publicId}")
    public APIResponse<<Entity>Response> getByPublicId(@PathVariable UUID publicId) {
        return APIResponse.<<Entity>Response>builder()
                .code(1000)
                .data(service.getByPublicId(publicId))
                .build();
    }

    // Tạo mới
    @PostMapping("/<endpoint>")
    public APIResponse<Void> create(@RequestBody @Valid <Entity>Request request) {
        service.create(request);
        return APIResponse.<Void>builder()
                .code(1000)
                .message("Thêm mới thành công")
                .build();
    }

    // Cập nhật
    @PutMapping("/<endpoint>/{publicId}")
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
    @DeleteMapping("/<endpoint>/{publicId}")
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
- `@Transactional` CHỈ đặt ở cấp method, TUYỆT ĐỐI KHÔNG đặt ở cấp class: `@Transactional(readOnly = true)` trên từng method đọc, `@Transactional(rollbackFor = Exception.class)` trên từng method thay đổi dữ liệu.
- Chỉ inject Repository của **chính feature này**. Cần dữ liệu/nghiệp vụ của feature khác thì inject `Service` interface của feature đó (xem mục "Quy tắc độc lập giữa các feature" trong CLAUDE.md / GEMINI.md).
- `@PreAuthorize` / `@PostAuthorize` đặt trên từng method ở đây.
- `<AUTH_EXPR>` là biểu thức phân quyền đã hỏi user, ví dụ:
  + `hasRole('HEAD_OF_DEPARTMENT')`
  + `hasAnyRole('ADMIN', 'HEAD_OF_DEPARTMENT')`
  + `hasAuthority('<PERMISSION_NAME>')`
  (tên role không có tiền tố `ROLE_`, đúng tên trong `Role.java`)

```java
@Service
@RequiredArgsConstructor
public class <Entity>ServiceImpl implements <Entity>Service {

    private final <Entity>Repository repository;
    private final <Entity>Mapping mapping;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("<AUTH_EXPR>")
    public Page<<Entity>Response> getAll(Pageable pageable) {
        // Ít field, không filter phức tạp: findAll + map
        // Nhiều field / nhiều filter tùy chọn: gọi query DTO Projection (xem create-feature-module, Bước 2)
        return repository.findAll(pageable)
                .map(mapping::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("<AUTH_EXPR>")
    public <Entity>Response getByPublicId(UUID publicId) {
        <Entity> entity = repository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.<ENTITY>_NOT_FOUND));
        return mapping.toResponse(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("<AUTH_EXPR>")
    public void create(<Entity>Request request) {
        <Entity> entity = mapping.toEntity(request);
        repository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("<AUTH_EXPR>")
    public void update(UUID publicId, <Entity>Request request) {
        <Entity> entity = repository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.<ENTITY>_NOT_FOUND));
        mapping.toUpdate(entity, request);
        repository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("<AUTH_EXPR>")
    public void delete(UUID publicId) {
        <Entity> entity = repository.findByPublicId(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.<ENTITY>_NOT_FOUND));
        // Xóa cứng; nếu xóa mềm thì đổi status rồi save
        repository.delete(entity);
    }
}
```

### Kiểm tra sở hữu
- Kiểm tra trên đối tượng trả về → `@PostAuthorize` (đặt ở ServiceImpl):
  ```java
  @Override
  @PostAuthorize("hasRole('ADMIN') or returnObject.ownerAccountId == authentication.name")
  public <Entity>Response getByPublicId(UUID publicId) { ... }
  ```
  `authentication.name` là `account.id` dạng String, nên field so sánh trong Response cũng phải là String. Không thêm `id` nội bộ vào Response chỉ để phục vụ `@PostAuthorize` — khi đó dùng cách bên dưới.
- Logic phức tạp (GVHD chỉ sửa đề tài mình hướng dẫn, SV chỉ xem dữ liệu của mình...) → kiểm tra trong method:
  ```java
  Account current = authenticationService.getAccountInfo();
  if (!entity.getSupervisor().getAccount().getId().equals(current.getId())) {
      throw new AppException(ErrorCode.FORBIDDEN);
  }
  ```

## Lưu ý quan trọng
1. **LUÔN dùng `publicId` (UUID)** trong URL path, KHÔNG BAO GIỜ dùng `id` (Long)
2. **Phân trang**: `@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)`; client gửi `?page=0&size=10&sort=id,desc`. Field trong `sort` phải là field của entity (hoặc alias của query), sai field sẽ ra lỗi 500
   + List dùng `@Query` DTO Projection có JOIN: KHÔNG để FE sort thẳng. Service giữ whitelist `Map<key FE, đường dẫn JPQL>` (ví dụ `name` → `a.lastName`, `a.firstName`), key lạ ném `INVALID_SORT_FIELD`, chốt thêm `<alias>.id DESC`; query không viết `ORDER BY` cố định. Mẫu: `UserServiceImpl.toStudentPageable`
3. **Validation**: Dùng `@Valid` trên Controller. Thuộc tính `message` trong `@NotBlank(message = "KEY")`, `@NotNull(message = "KEY")` PHẢI là **KEY** của `ErrorCode`; message tiếng Việt định nghĩa trong `ErrorCode.java`
4. **Phân quyền**: `@PreAuthorize` / `@PostAuthorize` LUÔN ở **Service Implementation**, KHÔNG đặt ở Controller; role đúng tên trong `Role.java`
5. **Không trả về `id` (Long)** trong response, chỉ trả `publicId`
6. **Tái sử dụng**: kiểm tra `common/utils` trước khi viết hàm tiện ích mới (chuẩn hóa chuỗi, đọc Excel, tìm trùng đều nằm trong `common/utils/excel/`)

## Hoàn thành khi
- [ ] Controller không có annotation phân quyền, không có `@RequestMapping` (đường dẫn đầy đủ trong từng HTTP method mapping), trả `APIResponse<T>`, dùng `@PageableDefault`
- [ ] ServiceImpl KHÔNG có `@Transactional` ở cấp class; `@Transactional(readOnly = true)` trên mọi method đọc, `@Transactional(rollbackFor = Exception.class)` trên mọi method ghi
- [ ] ServiceImpl không inject Repository của feature khác (gọi qua `Service` interface)
- [ ] Mỗi method public của ServiceImpl có `@PreAuthorize` (hoặc `@PostAuthorize`) với role/permission đã chốt
- [ ] ErrorCode mới (`<ENTITY>_NOT_FOUND`, key validate) đã được thêm
- [ ] Permission mới (nếu có) đã thêm vào `DB_UPDATE/DATA_ROLE_PERMISSION.txt`
- [ ] Unit test theo skill `write-tests`
- [ ] `.\mvnw.cmd clean compile` và `.\mvnw.cmd test -Dtest=<Entity>ServiceImplTest` chạy qua
