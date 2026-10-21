---
name: manual-test-report-5-2-5-3
description: >-
  Sử dụng skill này khi soạn test case kiểm thử thủ công cho báo cáo 5.2 Integration Test
  (kèm sheet SecurityTest) hoặc 5.3 System Test của GPMS, hoặc khi user báo kết quả test tay.
  Ví dụ: "viết test case integration cho chức năng import sinh viên", "soạn system test luồng đăng ký đề tài",
  "viết security test", "ITC003 fail, ghi kết quả round 1".
  Skill dựa trên code thật để soạn case rồi xuất file mapping .xlsx đúng cột báo cáo.
---

# Test case thủ công GPMS → file mapping báo cáo 5.2 / 5.3

Agent soạn test case để user tự thao tác trên FE (React, repo riêng, `http://localhost:3000`), Postman hoặc DevTools, rồi xuất **file mapping .xlsx** đúng thứ tự cột của báo cáo. User tự quyết định dán gì vào báo cáo. **Không bao giờ sửa trực tiếp file báo cáo Excel của user.**

## Nguyên tắc

1. **Không ghi Passed/Failed khi user chưa báo kết quả.** Case mới để Round 1/2/3 = `Pending` (SecurityTest: Status = `Not Run`).
2. **Bám vào thứ có thật.** URL, HTTP method, role, câu thông báo đều lấy từ code BE. FE nằm ở repo khác nên tên menu/nút/màn hình → hỏi user hoặc ghi `[cần xác nhận]`, không bịa.
3. **Expected phải quan sát được:** thông báo hiển thị, dữ liệu trên màn hình, trạng thái nhìn thấy từ tài khoản khác, HTTP status + `code` + `message` trong body (DevTools F12 → Network hoặc Postman). Không ghi "dữ liệu được lưu vào DB" nếu người test không có cách kiểm tra.
4. **Dữ liệu test cụ thể** (tài khoản theo role, giá trị nhập, file Excel mẫu) để người khác làm lại được đúng như vậy. KHÔNG ghi email/mật khẩu thật của người dùng vào test case; dùng tài khoản test do user cung cấp hoặc mô tả theo role (vd "tài khoản TBM test").

## Chọn loại báo cáo

| | 5.2 Integration | 5.3 System (FRs) | SecurityTest (trong file 5.2) |
|---|---|---|---|
| Mỗi sheet | 1 Feature | 1 Workflow (quy trình nghiệp vụ) | 1 sheet chung |
| Dòng nhóm | Function | Scenario | Không có |
| Mã case | ITC001… | STC001… | TC-SEC-001… |
| Phạm vi 1 case | 1 chức năng chạy qua FE → BE → DB, thường 1 role | Trọn 1 luồng nghiệp vụ, thường qua nhiều role | Xác thực, phân quyền, lộ dữ liệu |

Mã case tiếp nối số lớn nhất đã dùng trong `../readme.md/test_report/` (Grep `ITC[0-9]+`, `STC[0-9]+`, `TC-SEC-[0-9]+`), không trùng.

## Nguồn cần đọc (GPMS)

- Controller trong `features/<feature>/controller/`: URL, HTTP method, tham số. Không có prefix chung (vd `POST /student/import`).
- DTO request: ràng buộc `@NotBlank`, `@Size`, `@Email`... → message là KEY của `ErrorCode`, tra `configuration/exception/ErrorCode.java` để lấy câu tiếng Việt và HTTP status.
- ServiceImpl: quy tắc nghiệp vụ, `AppException(ErrorCode.X)`, và `@PreAuthorize` (role nào được gọi — phân quyền chỉ đặt ở ServiceImpl).
- `configuration/security/SecurityConfiguration.java`: endpoint public (`/login`, `/login-postman`, `/refresh-token`, `/log-out`), còn lại đều cần đăng nhập.
- Nghiệp vụ: `../readme.md/` (Project_Context, work_flow, case_execution) và skill `gpms-domain-knowledge`.
- Dữ liệu mẫu: `DB_UPDATE/DATA_ROLE_PERMISSION.txt` (role/permission), `../readme.md/test_data/` (file mẫu test tay).
- Mã FR cho 5.3: tài liệu hiện **chưa có mã FR** → hỏi user, chưa có thì ghi `[cần xác nhận]` ở cột Note.

## Thiết kế case

**5.2: mỗi Function nên có**
- Luồng thành công (HTTP 200, `code = 1000`, message thành công trong Controller).
- Lỗi nhập liệu: bỏ trống trường bắt buộc, sai định dạng, quá độ dài → mã lỗi theo `ErrorCode`.
- Vi phạm nghiệp vụ: trùng, sai trạng thái, vượt giới hạn, quá hạn.
- Import Excel: file rỗng, sai đuôi, sai cột, dòng lỗi lẫn dòng hợp lệ (kết quả từng dòng trong `data`).
- Hiển thị, tìm kiếm, phân trang nếu chức năng có danh sách (mặc định `size = 10`, sort `id DESC`).

**5.3: mỗi Workflow nên có**
- Scenario luồng chính, luồng thay thế (bị từ chối, huỷ, quá hạn…) và luồng ngoại lệ.
- Mỗi case đi trọn luồng qua các role (vd TBM → GVHD → SV). Cột Note ghi mã FR liên quan.

**SecurityTest (đúng với cơ chế của GPMS)**
- Đăng nhập chỉ qua **Google OAuth2** (`POST /login`), không có mật khẩu → không có case "sai mật khẩu". Thay bằng: tài khoản Google chưa được thêm vào hệ thống / chưa kích hoạt → bị từ chối (tra `ErrorCode` trong `AuthenticationServiceImpl`, vd `ACCOUNT_INVALID`).
- Chưa đăng nhập (không có cookie `access_token`) gọi API cần đăng nhập → **401, `code = 2000`**.
- Sai role (vd SV gọi `GET /teacher`) → **403, `code = 2001`**. Role lấy đúng tên trong `common/enums/Role.java`.
- Đổi `publicId` trên URL để xem dữ liệu người khác → 403/404, không lộ dữ liệu (chỉ khi ServiceImpl có kiểm tra sở hữu; nếu chưa có thì ghi `[cần xác nhận]`).
- Sau `POST /log-out`, dùng lại token cũ → 401 (token nằm trong blacklist `InvalidateToken`).
- Token hết hạn / sửa token → 401.
- Cột Coverage Technique chỉ dùng đúng một trong: `Equivalence Partitioning (EP)`, `Boundary Value Analysis (BVA)`, `Decision Table Testing`, `State Transition Testing`, `Use Case Testing`, `Error Guessing`, `Branch / Condition Coverage`, `Input Domain Partitioning`.

**Cách viết từng ô**
- Description: 1 dòng, "Kiểm tra <hành động> khi <điều kiện>".
- Procedure: các bước đánh số, mỗi bước một thao tác, kèm dữ liệu cụ thể (`Họ tên = "Nguyễn Văn A"`, file `students_valid.xlsx`).
- Expected Results: liệt kê kết quả quan sát được, trích đúng message trong `ErrorCode`/Controller, kèm HTTP status và `code`.
- Pre-conditions: tài khoản/role, trạng thái dữ liệu, hoặc case phải chạy trước (`Đã chạy ITC001`).

## Xuất file mapping

Cột, giữ đúng thứ tự:
- **5.2 / 5.3 (A–P):** Test Case ID | Test Case Description | Test Case Procedure | Expected Results | Pre-conditions | Round 1 | Test date | Tester | Round 2 | Test date | Tester | Round 3 | Test date | Tester | Defect IDs | Note. Dòng nhóm chỉ ghi tên Function/Scenario ở cột A.
- **SecurityTest (A–J):** Test Case ID | Scenario Name | Role / Session | Coverage Technique | Given | When | Then (Expected: HTTP Status / Visibility) | Status | Defect ID | Notes.

Cách tạo file:
1. Ghi file TSV UTF-8 vào đúng thư mục (đường dẫn tính từ `tlu_gpms_system/`; ở git root là `readme.md/test_report/...`; có commit vào git), tên file không dấu (vd `QuanLySinhVien.tsv`, `SecurityTest.tsv`):
   - 5.2 Integration Test (kể cả `SecurityTest`): `../readme.md/test_report/Integration_Test_Report/`
   - 5.3 System Test: `../readme.md/test_report/System_Test_Report/`
   - Dòng 1: `#sheet: <Tên Feature/Workflow>` (tiếng Việt có dấu được, tối đa 31 ký tự).
   - Dòng 2: tiêu đề cột như trên.
   - Mỗi dòng sau là 1 hàng, các ô cách nhau bằng TAB, ô trống vẫn giữ TAB. Xuống dòng trong ô viết `\n`, dấu `\` viết `\\`.
2. Chuyển sang xlsx bằng `ReportXlsx.java` có sẵn (JDK 17 chạy thẳng file nguồn, không cần build):
   ```powershell
   java src/test/java/com/example/tlu_gpms_system/testreport/ReportXlsx.java ../readme.md/test_report/Integration_Test_Report/QuanLySinhVien.tsv ../readme.md/test_report/Integration_Test_Report/QuanLySinhVien.xlsx
   ```
   File xlsx luôn đặt cùng thư mục với file TSV (5.3 thì thay bằng `System_Test_Report`).
   Thấy dòng `Wrote N rows -> ...` là xong. KHÔNG sửa `ReportXlsx.java` (dùng chung với skill `unit-test-report-5-1`).

## Ghi kết quả test tay

Khi user báo kết quả (vd "ITC001 pass, ITC003 fail: không hiện thông báo"), tạo file mapping mới cùng bố cục, tên thêm `_roundN` (vd `QuanLySinhVien_round1.tsv/.xlsx`, cùng thư mục với file gốc), không ghi đè file gốc:
- Cột A–E giữ nguyên, chỉ điền 3 cột của vòng đó (Round N, Test date `dd/MM/yyyy`, Tester); các cột vòng khác để trống.
- Cột Note ghi mô tả lỗi user báo. Defect ID chỉ ghi khi user đưa, không tự bịa.
- Chỉ ghi kết quả user đã báo; case chưa báo để `Pending`.

## Báo lại cho user

1. Đường dẫn file mapping.
2. Nội dung phần đầu sheet: Feature/Workflow (ô B2) và Test requirement (ô B3).
3. Dòng cho sheet **Test Cases**: Function/Workflow Name | Sheet Name | Description | Pre-Condition.
4. Danh sách dữ liệu cần chuẩn bị trước khi test (tài khoản theo role, file Excel mẫu, học kỳ…).
5. Các chỗ `[cần xác nhận]` để user bổ sung.
6. Cách dán (luôn *Paste Special → Values*, Ctrl+Alt+V, để giữ định dạng template):
   - Sheet Feature/Workflow: xoá các dòng mẫu cũ, chọn ô **A11** rồi dán (dòng nhóm đầu tiên nằm ở A11 như template).
   - SecurityTest: dán vào ô **A2**.
   - Kết quả từng vòng: chỉ dán 3 cột của vòng đó (Round 1: F–H, Round 2: I–K, Round 3: L–N).
7. Lưu ý template gốc: ô Number of TCs đếm cả dòng nhóm nên ra số lớn hơn thực tế; các dòng Round 2, Round 3 ở đầu sheet đang đếm nhầm cột Round 1.
