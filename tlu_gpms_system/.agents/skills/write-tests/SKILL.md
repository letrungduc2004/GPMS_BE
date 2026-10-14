---
name: write-tests
description: >-
  Sử dụng skill này khi user yêu cầu viết test cho một feature hoặc service.
  Ví dụ: "viết unit test cho TopicService", "test API endpoint", "tạo integration test".
  Skill hướng dẫn cách viết Unit Test (Mockito) và Integration Test cho dự án GPMS.
---

# Hướng dẫn viết Test cho TLU GPMS

## Cấu trúc thư mục Test
```
src/test/java/com/example/tlu_gpms_system/
├── features/
│   └── <feature>/
│       ├── service/
│       │   └── impl/
│       │       └── <Entity>ServiceImplTest.java       # Unit Test
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
        // Khởi tạo test data ở đây
    }

    @Nested
    @DisplayName("Tạo mới")
    class CreateTests {
        @Test
        @DisplayName("Tạo thành công khi dữ liệu hợp lệ")
        void create_whenValidRequest_shouldReturnResponse() {
            // Given
            when(mapping.toEntity(request)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);
            when(mapping.toResponse(entity)).thenReturn(response);

            // When
            var result = service.create(request);

            // Then
            assertThat(result).isNotNull();
            verify(repository).save(entity);
        }
    }

    @Nested
    @DisplayName("Tìm theo publicId")
    class GetByPublicIdTests {
        @Test
        @DisplayName("Tìm thành công khi publicId tồn tại")
        void getByPublicId_whenExists_shouldReturnResponse() {
            when(repository.findByPublicId(publicId)).thenReturn(Optional.of(entity));
            when(mapping.toResponse(entity)).thenReturn(response);

            var result = service.getByPublicId(publicId);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Throw exception khi publicId không tồn tại")
        void getByPublicId_whenNotFound_shouldThrowException() {
            when(repository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByPublicId(publicId))
                    .isInstanceOf(AppException.class);
        }
    }
}
```

### Quy tắc Unit Test
1. **Naming**: `methodName_whenCondition_shouldExpectedBehavior`
2. **Structure**: Given → When → Then (AAA pattern)
3. **Dùng `@Nested`** để nhóm test theo method
4. **Dùng `@DisplayName`** bằng tiếng Việt để dễ đọc report
5. **Mock tất cả dependency**, chỉ test logic trong service
6. **Verify side effects**: `verify(repository).save(...)`, `verify(notificationService).pushNotification(...)`

## Integration Test - Controller Layer

### Template
```java

@SpringBootTest
@AutoConfigureMockMvc
class <Entity>ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Trả về 200 khi lấy danh sách thành công")
    @WithMockUser(roles = "ADMIN")
    void getAll_shouldReturn200() throws Exception {
        mockMvc.perform(get("/<endpoint>")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("Trả về 403 khi không có quyền")
    @WithMockUser(roles = "STUDENT")
    void getAll_whenForbidden_shouldReturn403() throws Exception {
        mockMvc.perform(get("/<endpoint>")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
```

### Quy tắc Integration Test
1. Dùng `@SpringBootTest` + `@AutoConfigureMockMvc`
2. Dùng `@WithMockUser(roles = "XXX")` để giả lập user
3. Kiểm tra HTTP status code + response body structure
4. Test cả happy path và error path (403, 404, 400)
