---
name: write-tests
description: >-
  Sử dụng skill này khi user yêu cầu viết test cho một feature hoặc service trong GPMS.
  Ví dụ: "viết unit test cho TopicService", "test API endpoint", "tạo integration test",
  "write unit test", "test phân quyền API".
  Skill điều phối: unit test theo chuẩn UTC (skill unit-test-report-5-1),
  integration test tự động bằng MockMvc (hướng dẫn ngay trong skill này).
---

# Hướng dẫn viết Test cho TLU GPMS

| Loại test | Theo skill |
|---|---|
| Unit test service/helper/util (Mockito, không Spring context) | **`unit-test-report-5-1`** — BẮT BUỘC chuẩn UTC (`@UnitTestCase`, không `@Nested`) |
| Integration test tự động Controller (MockMvc, cần MySQL) | Mục **Integration Test** bên dưới |
| Test case thủ công cho báo cáo 5.2 / SecurityTest / 5.3 | **`manual-test-report-5-2-5-3`** |

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

Hạ tầng báo cáo unit test nằm ở `src/test/java/com/example/tlu_gpms_system/testreport/` (`UnitTestCase`, `UtcReportExtension`, `ReportXlsx`).

## Unit Test

Toàn bộ quy ước (thiết kế case Normal/Abnormal/Boundary, mã UTC, annotation, cách chạy theo round, xuất file mapping) nằm trong skill **`unit-test-report-5-1`**. Không viết unit test theo style `@Nested` / `method_condition_expected` cũ nữa.

## Integration Test - Controller Layer

### Lưu ý trước khi viết
- `@SpringBootTest` khởi động toàn bộ context → **cần MySQL đang chạy** với cấu hình trong `application.properties`. Báo user nếu không chạy được.
- Dự án **chưa có** `spring-security-test` trong `pom.xml` → **không dùng được** `@WithMockUser` / `jwt()`. Mặc định dùng Cách 1.
- Test cần rollback dữ liệu → thêm `@Transactional` trên **từng method test** (TUYỆT ĐỐI KHÔNG đặt `@Transactional` ở cấp class).
- KHÔNG hard-code email/mật khẩu thật trong test mới; lấy tài khoản test qua repository theo role hoặc tạo dữ liệu test trong `@BeforeEach`.

### Cách 1 (mặc định): Token thật + cookie `access_token`
```java
@SpringBootTest
@AutoConfigureMockMvc
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
    @Transactional
    @DisplayName("Trả về 200 khi có quyền lấy danh sách")
    void getAll_success() throws Exception {
        Account headOfDepartment = /* account test có role HEAD_OF_DEPARTMENT */;

        mockMvc.perform(get("/<endpoint>").cookie(loginAs(headOfDepartment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @Transactional
    @DisplayName("Trả về 403 khi không có quyền")
    void getAll_forbidden_return403() throws Exception {
        Account student = /* account test có role STUDENT */;

        mockMvc.perform(get("/<endpoint>").cookie(loginAs(student)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    @Transactional
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
```powershell
# Unit test kèm file mapping báo cáo 5.1 (xem skill unit-test-report-5-1)
.\mvnw.cmd test "-Dtest=<Entity>ServiceImplTest" "-Dutc.round=1" "-Dutc.tester=<Tên>" "-Dutc.outDir=../readme.md/test_report/Unit_Test_Report"

# Một method
.\mvnw.cmd test "-Dtest=<Entity>ServiceImplTest#UTC001_create_validRequest_saveEntity"

# Toàn bộ
.\mvnw.cmd test
```
Báo lại user kết quả thật (số test pass/fail, lỗi cụ thể); không ghi "đã pass" nếu chưa chạy.
