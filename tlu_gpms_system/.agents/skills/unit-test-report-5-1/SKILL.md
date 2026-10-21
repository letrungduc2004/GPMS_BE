---
name: unit-test-report-5-1
description: >-
  Sử dụng skill này khi viết unit test (JUnit 5 + Mockito) cho service/helper trong GPMS,
  chạy test theo vòng (round) hoặc lấy kết quả cho báo cáo 5.1 Unit Test.
  Ví dụ: "viết unit test cho TopicServiceImpl", "chạy unit test round 2", "xuất báo cáo 5.1".
  Mọi unit test mới đều theo chuẩn UTC của skill này; extension tự xuất file mapping .xlsx đúng cột A–P.
---

# Unit test GPMS → file mapping báo cáo 5.1

Mọi unit test trong dự án viết theo chuẩn này: mỗi `@Test` gắn `@UnitTestCase` (thông tin cột A–E), extension `UtcReportExtension` lấy kết quả Passed/Failed **thật** từ JUnit và ghi file mapping `.xlsx` sau khi test class chạy xong. Người dùng tự dán vào báo cáo. **Không bao giờ sửa trực tiếp file báo cáo Excel của người dùng.**

> Mẫu tham khảo: `src/test/java/com/example/tlu_gpms_system/features/account/service/impl/UserServiceImplTest.java`
> Integration test (MockMvc) KHÔNG thuộc skill này → xem skill `write-tests`.

## Nguyên tắc bắt buộc

1. **Kết quả chỉ lấy từ lần chạy thật.** Không tự viết "Passed"/"Failed", không báo test đạt khi chưa chạy.
2. **Expected lấy từ nghiệp vụ, không chép lại từ code đang chạy.** Nguồn: `readme.md/` (Project_Context, work_flow, case_execution), mô tả của user, skill `gpms-domain-knowledge`, annotation validation trên DTO (`@NotBlank`, `@Size`...), `ErrorCode`. Quy tắc nào chưa rõ → liệt kê giả định và HỎI user trước khi viết test.
3. **Test fail là kết quả hợp lệ của báo cáo.** Không sửa expected/assertion, không thêm `@Disabled`, không sửa code production để test xanh, trừ khi user đồng ý. Báo lại case fail kèm phân tích nguyên nhân.
4. **Annotation là đặc tả.** Assertion phải kiểm tra đúng những gì ghi trong `returns` / `exception` / `log`. Sửa bên này thì sửa luôn bên kia.
5. **Đúng phạm vi unit test:** `<X>ServiceImpl`, helper (`service/helper`), util (`common/utils`), validator. Mọi dependency đều mock. Không dùng `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`, không chạm DB/mạng. Controller, phân quyền `@PreAuthorize` và query Repository thuộc báo cáo 5.2 (skill `manual-test-report-5-2-5-3` / `write-tests`).

## Bước 0 — Hạ tầng (đã cài sẵn)

3 file trong `src/test/java/com/example/tlu_gpms_system/testreport/`:
- `UnitTestCase.java` — annotation (id, method, type, precondition, input, returns, exception, log).
- `UtcReportExtension.java` — ghi `<Module>_round<N>.xlsx` vào `../readme.md/test_report/Unit_Test_Report/` (mặc định trong code, tức `readme.md/test_report/Unit_Test_Report/` ở git root; đổi tạm bằng `-Dutc.outDir`).
- `ReportXlsx.java` — ghi xlsx không cần thư viện ngoài; cũng chạy được từ dòng lệnh để đổi TSV → xlsx (skill 5.2/5.3 dùng lại).

KHÔNG sửa 3 file này trừ khi user yêu cầu. Không thêm thư viện test nào ngoài `spring-boot-starter-test`.

## Bước 1 — Thiết kế test case trước khi viết code

1. Module = lớp được test (vd `TopicServiceImpl`). Mỗi module là một sheet trong báo cáo.
2. Với mỗi method public có logic, liệt kê case theo 3 loại:
   - **Normal**: đầu vào hợp lệ, luồng chính. Mỗi nhánh xử lý chính có ít nhất 1 case.
   - **Abnormal**: đầu vào không hợp lệ hoặc vi phạm nghiệp vụ → `AppException` (không tìm thấy, trùng, sai trạng thái, sai định dạng, quá hạn…).
   - **Boundary**: giá trị đúng tại biên (min, max, độ dài tối đa, danh sách rỗng, đúng hạn chót). Vượt biên (max + 1) xếp vào Abnormal.
3. Đánh mã UTC tiếp nối số lớn nhất đã có trong toàn project, dạng `UTC` + 3 chữ số, không trùng. Tìm bằng Grep pattern `id = "UTC[0-9]+"` trong `src/test/java`, hoặc bash:
   `grep -rhoE 'id *= *"UTC[0-9]+"' src/test/java | grep -oE '[0-9]+' | sort -n | tail -1`
4. Trình bày bảng case ngắn gọn (ID | method | Type | Input | Expected) cho user TRƯỚC khi viết code, nêu rõ giả định nghiệp vụ.

## Bước 2 — Viết test

Quy ước:
- Một module = một test class `<LớpĐượcTest>Test`, đặt mirror package (vd `features/topic/service/impl/TopicServiceImplTest.java`). Tên module trong file mapping = tên class bỏ hậu tố `Test`.
- Class gắn `@ExtendWith({MockitoExtension.class, UtcReportExtension.class})`.
- Một `@Test` = một mã UTC. **Không** dùng `@Nested`, `@ParameterizedTest`, `@RepeatedTest`. Không cần `@DisplayName` (thông tin nằm trong annotation).
- Tên method test: `UTC004_<method>_<tình huống>_<kết quả>` (vd `UTC012_createTeachers_emailExist_throwAppException`).
- Test sắp theo thứ tự mã UTC; các test của cùng một method đứng liền nhau, có thể ngăn bằng comment `// ===== <method> =====`.
- Cách điền annotation (tiếng Việt có dấu, file UTF-8):
  - `precondition`: trạng thái chuẩn bị trước khi gọi, gồm cả dữ liệu mock (vd `"accountRepository.existsByEmail trả về true"`). Không có thì bỏ qua (mặc định N/A).
  - `input`: dạng `tên = giá trị`, cách nhau `; `. Object thì liệt kê trường quan trọng: `request = {email: "gv01@tlu.edu.vn", fullName: ""}`.
  - `returns` / `exception` / `log` → 3 dòng `Return / Exception / Log Message` trong cột Expected.
    - `exception` ghi theo dạng `AppException: <ERROR_CODE> ("<message trong ErrorCode>")`.
    - Method `void` kiểm tra bằng `verify` → ghi vào `returns` dạng mô tả (vd `"void; gọi accountRepository.save 1 lần với role SUPERVISOR"`).
    - `log`: thông báo trả về/hiển thị cho user mà test có kiểm tra (vd message trong `TeacherImportRowResult`); không kiểm tra thì bỏ qua.
- Kỹ thuật dùng chung (giữ từ style cũ):
  - Given → When → Then; assert bằng AssertJ.
  - Lỗi nghiệp vụ: `assertThatThrownBy(...).isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.X)` — luôn kiểm tra đúng `ErrorCode`.
  - Chỉ stub những gì method thực sự gọi (strict stubs báo `UnnecessaryStubbingException` nếu thừa).
  - Side effect: `verify(repo).save(...)`, `ArgumentCaptor`, và `verify(repo, never()).save(any())` cho nhánh lỗi.
  - Service gọi `authenticationService.getAccountInfo()` → mock `AuthenticationService`, stub trả `Account` test.
  - `@PreAuthorize` KHÔNG chạy trong unit test → không viết case phân quyền ở đây.
  - Test import Excel: tạo file bằng `XSSFWorkbook` bọc trong `MockMultipartFile` (xem helper `buildExcel` trong `UserServiceImplTest`).
  - Helper/util static: gọi trực tiếp, không cần Mockito, nhưng class vẫn gắn `@ExtendWith(UtcReportExtension.class)`.

Mẫu (tên lớp, method, ErrorCode chỉ là ví dụ — phải dùng đúng tên có trong project):
```java
@ExtendWith({MockitoExtension.class, UtcReportExtension.class})
class TopicServiceImplTest {

    @Mock TopicRepository topicRepository;
    @InjectMocks TopicServiceImpl topicService;

    // ===== registerTopic =====

    @Test
    @UnitTestCase(id = "UTC010", method = "registerTopic", type = UnitTestCase.Type.ABNORMAL,
            precondition = "topicRepository.findByPublicId trả về đề tài đã đủ 3/3 sinh viên",
            input = "topicId = <uuid đề tài>",
            exception = "AppException: TOPIC_FULL (\"Đề tài đã đủ số lượng sinh viên\")")
    void UTC010_registerTopic_topicFull_throwAppException() {
        given(topicRepository.findByPublicId(topicId)).willReturn(Optional.of(fullTopic));

        assertThatThrownBy(() -> topicService.registerTopic(topicId))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOPIC_FULL);
        verify(topicRepository, never()).save(any());
    }
}
```

## Bước 3 — Chạy test

Hỏi user một lần: vòng test (1/2/3, mặc định 1) và tên người test (vd `HanhNH`) nếu chưa biết.

PowerShell (mỗi tham số `-D...` phải nằm trong nháy kép):
```powershell
.\mvnw.cmd test "-Dtest=TopicServiceImplTest" "-Dutc.round=1" "-Dutc.tester=HanhNH" "-Dutc.outDir=../readme.md/test_report/Unit_Test_Report"
```
Bash: `./mvnw test -Dtest=TopicServiceImplTest -Dutc.round=1 -Dutc.tester=HanhNH -Dutc.outDir=../readme.md/test_report/Unit_Test_Report`. Nhiều class: `-Dtest=AServiceImplTest,BServiceImplTest`.

Sau khi chạy:
- Tìm dòng `[UTC-REPORT] <Module> | round N | x Passed, y Failed, z Pending | <đường dẫn>`. File mapping: `../readme.md/test_report/Unit_Test_Report/<Module>_round<N>.xlsx` (có commit). Đây là mặc định trong code nên không truyền `-Dutc.outDir` vẫn ghi đúng chỗ; tham số trong lệnh chỉ để ghi rõ đường dẫn.
- `BUILD FAILURE` vì có test fail là bình thường, file mapping vẫn được ghi. Lỗi biên dịch → sửa code test (không sửa code production) rồi chạy lại.
- Cột Note có `[CẢNH BÁO]` (thiếu annotation, trùng mã, mã không bắt đầu bằng UTC) → sửa test và chạy lại trước khi báo user.

## Bước 4 — Báo lại cho user

1. Đường dẫn file mapping.
2. Bảng tóm tắt: ID | Method | Type | Kết quả | Ghi chú.
3. Mỗi case Failed: nguyên nhân có thể (lỗi code nghiệp vụ hay giả định sai) và đề xuất cách sửa, KHÔNG tự sửa. Nhắc user đặt Defect ID nếu cần, không tự bịa.
4. Nếu là module mới: thông tin cho sheet **Test Cases** (Module Name, Sheet Name, Description, Pre-Condition).
5. Cách dán vào báo cáo 5.1 (luôn *Paste Special → Values*, Ctrl+Alt+V, để giữ định dạng template):
   - **Round 1**: trong file mapping chọn từ ô A2 đến hết cột P, sang sheet module của báo cáo (đã xoá các dòng mẫu cũ), chọn ô **A12** rồi dán. Ô B2 của sheet ghi tên module.
   - **Round 2/3**: chỉ copy 3 cột của vòng đó (Round 2: I–K, Round 3: L–N) dán vào đúng các cột đó. Thứ tự dòng giống vòng trước nếu không thêm/bớt test; nếu có thay đổi thì liệt kê các mã UTC mới/bị xoá để user chèn dòng.
   - Lưu ý template gốc: danh sách chọn ở cột Round chưa có "Failed" (dán Values vẫn được); sheet Test Statistics đang lấy số liệu Round 3.
