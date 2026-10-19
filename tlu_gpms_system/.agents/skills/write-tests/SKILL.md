---
name: write-tests
description: >-
  Sử dụng skill này khi user yêu cầu viết test cho một feature hoặc service trong GPMS.
  Ví dụ: "viết unit test cho TopicService", "test API endpoint", "tạo integration test",
  "write unit test", "test phân quyền API".
  Skill hướng dẫn cách viết Unit Test (Mockito) và Integration Test (MockMvc) theo style hiện có của dự án.
---

# Hướng dẫn viết Test cho TLU GPMS

> Mẫu tham khảo trong dự án:
> - Unit test: `src/test/java/.../features/account/service/impl/UserServiceImplTest.java`
> - Integration test: `src/test/java/.../AuthenticationFlowTest.java`

## Cấu trúc thư mục Test
```
src/test/java/com/example/tlu_gpms_system/
├── features/
│   └── <feature>/
│       ├── service/
│       │   ├── impl/
│       │   │   └── <Entity>ServiceImplTest.java       # Unit Test
│       │   └── helper/
│       │       └── <Helper>Test.java                  # Test helper (không cần mock)
│       └── controller/
│           └── <Entity>ControllerIntegrationTest.java  # Integration Test
```

## Unit Test - Service Layer

### Template
```java
@ExtendWith(MockitoExtension.class)
class <Entity>ServiceImplTest {

    @Mock
    private <Entity>Repository repository;

    @Mock
    private <Entity>Mapping mapping;

    @InjectMocks
    private <Entity>ServiceImpl service;

    private <Entity> entity;
    private <Entity>Request request;
    private <Entity>Response response;
    private UUID publicId;

    @BeforeEach
    void setUp() {
        publicId = UUID.randomUUID();
        // Khởi tạo test data ở đây (dùng builder của entity/DTO)
    }

    @Nested
    @DisplayName("Tạo mới")
    class CreateTests {
        @Test
        @DisplayName("Tạo thành công khi dữ liệu hợp lệ")
        void create_success() {
            // Given: create() trả void nên chỉ stub những gì service thực sự gọi
            when(mapping.toEntity(request)).thenReturn(entity);

            // When
            service.create(request);

            // Then
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("Lưu đúng dữ liệu xuống repository")
        void create_success_savedEntityHasCorrectData() {
            when(mapping.toEntity(request)).thenReturn(entity);

            service.create(request);

            ArgumentCaptor<<Entity>> captor = ArgumentCaptor.forClass(<Entity>.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getPublicId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tìm theo publicId")
    class GetByPublicIdTests {
        @Test
        @DisplayName("Tìm thành công khi publicId tồn tại")
        void getByPublicId_success() {
            when(repository.findByPublicId(publicId)).thenReturn(Optional.of(entity));
            when(mapping.toResponse(entity)).thenReturn(response);

            var result = service.getByPublicId(publicId);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("Ném lỗi khi publicId không tồn tại")
        void getByPublicId_notFound_throwException() {
            when(repository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByPublicId(publicId))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.<ENTITY>_NOT_FOUND);

            verify(mapping, never()).toResponse(any());
        }
    }
}
```

### Quy tắc Unit Test
1. **Naming**: theo style hiện có `method_condition_expected`, ví dụ `getAllStudents_success`, `createTeachers_emailExist_throwException`
2. **Structure**: Given → When → Then (AAA pattern)
3. **Dùng `@Nested`** để nhóm test theo method, **`@DisplayName`** bằng tiếng Việt
4. **Assert bằng AssertJ**: `assertThat`, `assertThatThrownBy(...).extracting("errorCode").isEqualTo(ErrorCode.X)` — luôn kiểm tra đúng `ErrorCode`, không chỉ kiểm tra loại exception
5. **Mock tất cả dependency**, chỉ test logic trong service. Chỉ stub những gì method thực sự gọi (Mockito strict stubs sẽ báo `UnnecessaryStubbingException` nếu thừa)
6. **Method trả `void`**: không gán kết quả, kiểm tra bằng `verify(...)` và `ArgumentCaptor`
7. **Verify side effects**: `verify(repository).save(...)`, `verify(notificationService).pushNotification(...)`, và `verify(repository, never()).save(any())` cho nhánh lỗi
8. **Service gọi `authenticationService.getAccountInfo()`**: mock `AuthenticationService` và stub trả về `Account` test
9. **Phân quyền**: `@PreAuthorize` / `@PostAuthorize` ở ServiceImpl **KHÔNG được kích hoạt** trong unit test Mockito (không có Spring context). Muốn test phân quyền → viết integration test bên dưới
10. **Test import Excel**: tạo file bằng `XSSFWorkbook` rồi bọc trong `MockMultipartFile`:
    ```java
    private MockMultipartFile buildExcel(String[]... rows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < rows[i].length; j++) {
                    row.createCell(j).setCellValue(rows[i][j]);
                }
            }
            wb.write(out);
            return new MockMultipartFile("file", "data.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }
    ```
11. **Test helper / common utils** (`service/helper`, `common/utils/excel`): là static method → gọi trực tiếp, không cần Mockito. Test đặt mirror package, ví dụ `src/test/java/.../common/utils/excel/TextNormalizerTest.java`. Riêng `ExcelUtils.openXlsx` ném `AppException` (`FILE_EMPTY`, `FILE_INVALID_FORMAT`) → kiểm tra bằng `assertThatThrownBy(...).extracting("errorCode")`

## Integration Test - Controller Layer

### Lưu ý trước khi viết
- `@SpringBootTest` khởi động toàn bộ context → **cần MySQL đang chạy** với cấu hình trong `application.properties`. Báo user nếu không chạy được.
- Dự án **chưa có** `spring-security-test` trong `pom.xml` → **không dùng được** `@WithMockUser` / `jwt()`. Mặc định dùng Cách 1.
- Test có ghi dữ liệu → thêm `@Transactional` ở class/method để rollback sau mỗi test.
- KHÔNG hard-code email/mật khẩu thật trong test mới; lấy tài khoản test qua repository theo role hoặc tạo dữ liệu test trong `@BeforeEach`.

### Cách 1 (mặc định): Token thật + cookie `access_token`
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class <Entity>ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuthenticationService authenticationService;

    // Tạo cookie đăng nhập cho một account test
    private Cookie loginAs(Account account) {
        String token = authenticationService.createToken(account);
        return new Cookie("access_token", token);
    }

    @Test
    @DisplayName("Trả về 200 khi có quyền lấy danh sách")
    void getAll_success() throws Exception {
        Account headOfDepartment = /* account test có role HEAD_OF_DEPARTMENT */;

        mockMvc.perform(get("/<endpoint>").cookie(loginAs(headOfDepartment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("Trả về 403 khi không có quyền")
    void getAll_forbidden_return403() throws Exception {
        Account student = /* account test có role STUDENT */;

        mockMvc.perform(get("/<endpoint>").cookie(loginAs(student)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    @DisplayName("Trả về 401 khi chưa đăng nhập")
    void getAll_noToken_return401() throws Exception {
        mockMvc.perform(get("/<endpoint>"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
```

### Cách 2 (chỉ khi user đồng ý thêm dependency): `@WithMockUser`
- Hỏi user trước khi thêm `org.springframework.security:spring-security-test` (scope `test`) vào `pom.xml`.
- Sau đó có thể dùng `@WithMockUser(roles = "HEAD_OF_DEPARTMENT")`. Lưu ý: service gọi `getAccountInfo()` sẽ parse `authentication.getName()` thành `Long`, nên cần `@WithMockUser(username = "<accountId>", roles = "...")` với account id có thật trong DB.

### Quy tắc Integration Test
1. Dùng `@SpringBootTest` + `@AutoConfigureMockMvc`
2. Kiểm tra cả HTTP status và `$.code` trong body:
   - 200 / `1000` thành công
   - 401 / `2000` chưa đăng nhập hoặc token đã bị vô hiệu hóa
   - 403 / `2001` sai role/permission
   - 404 / `<ENTITY>_NOT_FOUND`
   - 400 hoặc 422 / lỗi validate (`4000`, hoặc `1003` nếu key validate chưa có trong `ErrorCode`)
3. Role dùng đúng tên trong `common/enums/Role.java`
4. Test cả happy path và error path

## Chạy test
```bash
# Một class
.\mvnw.cmd test -Dtest=<Entity>ServiceImplTest

# Một method
.\mvnw.cmd test -Dtest=<Entity>ServiceImplTest#create_success

# Toàn bộ
.\mvnw.cmd test
```
Báo lại user kết quả thật (số test pass/fail, lỗi cụ thể); không ghi "đã pass" nếu chưa chạy.
