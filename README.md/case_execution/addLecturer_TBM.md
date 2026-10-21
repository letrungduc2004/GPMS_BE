# ĐẶC TẢ KỸ THUẬT: TÍNH NĂNG THÊM DANH SÁCH GIÁO VIÊN (IMPORT EXCEL, NHẬP THỦ CÔNG & KÍCH HOẠT TỪ DANH SÁCH)

> **Mục đích tài liệu:** Tài liệu này cung cấp đầy đủ bối cảnh nghiệp vụ (Domain Context) và đặc tả kỹ thuật chi tiết cho AI Coding Agent để triển khai tính năng thêm danh sách giáo viên hướng dẫn đầu kì dành cho **Trưởng bộ môn** (Head of Department). Yêu cầu tuân thủ chính xác 100% các quy tắc kiểm tra dữ liệu, tự động gán hạn mức hướng dẫn (`amount`), quy tắc gom lỗi, cơ chế lưu một phần (Partial Import) và tối ưu hiệu năng truy vấn.
>
> Tính năng này **làm giống hệt pipeline thêm sinh viên** (`addStudent_TBM.md`). Chỗ nào tài liệu này không nói khác thì làm theo addStudent.

---

## 0. CÁC QUYẾT ĐỊNH ĐÃ CHỐT

| # | Nội dung | Quyết định |
| :--- | :--- | :--- |
| 1 | Role gán cho giáo viên mới | `SUPERVISOR` (Giáo viên hướng dẫn). Có sẵn trong `common.enums.Role` và bảng `role`. |
| 2 | Trưởng bộ môn | **Có** bản ghi trong bảng `teacher`. Vì vậy nếu file chứa email TBM → rơi vào **TH4** (Báo Vàng). |
| 3 | `amount` | **Import Excel:** không có cột, BE luôn tự gán theo email (20 / 10), kể cả khi kích hoạt lại (ghi đè giá trị kì trước). **Nhập thủ công:** FE tự điền theo đuôi email, TBM được sửa lại thành số nguyên **≥ 0** (không làm tròn: `5.5`, `5.0`, `"abc"` đều báo lỗi đỏ), **không giới hạn trên** (cả giáo viên trong và ngoài trường, `0` = không nhận hướng dẫn); không truyền thì BE tự gán. |
| 3a | Đuôi email giáo viên | Chỉ nhận **`@tlu.edu.vn`** (trong trường) và **`@gmail.com`** (ngoài trường). Đuôi khác, kể cả email sinh viên `@e.tlu.edu.vn` → lỗi `TEACHER_EMAIL_DOMAIN_INVALID`. |
| 3b | Đơn vị công tác | *(Đổi requirement 2026-10-21)* Lưu thẳng **tên trường**, không còn 2 giá trị `Trong trường` / `Ngoài trường`. Bắt buộc, chỉ gồm chữ, số, khoảng trắng và các dấu `. , - & ( ) ' /`, tối đa 255 ký tự. Phải **khớp với email**: `@tlu.edu.vn` ⇔ đúng tên **`Trường Đại học Thủy lợi`** (không phân biệt hoa/thường), `@gmail.com` ⇔ tên trường **khác** Thủy lợi. Import Excel: TBM tự gõ. Nhập tay: FE tự điền tên Thủy lợi khi email `@tlu.edu.vn`, TBM gõ tên trường khi email `@gmail.com`. Dữ liệu cũ `Trong trường` / `Ngoài trường` trong DB do user tự xóa. |
| 3e | Ngành của giáo viên ngoài trường | Giáo viên ngoài trường (`@gmail.com`) **tuyệt đối không được điền ngành** → `MAJOR_NOT_ALLOWED`; lưu `major_id = NULL`. Giáo viên trong trường vẫn bắt buộc ngành (`MAJOR_EMPTY`). Cột `teacher.major_id` phải cho phép NULL: chạy tay `db_update/TEACHER_MAJOR_NULLABLE.sql` (`ddl-auto=update` không tự bỏ NOT NULL). |
| 3c | Cuối kì | Hệ thống chỉ tắt `Account.status`, **không** gỡ role. Kích hoạt lại (TH5) không cần thêm lại role. |
| 3d | Giáo viên đang hoạt động (TH4) | Báo Vàng **"Thông tin giáo viên đã có trong hệ thống"** cho cả import Excel và nhập thủ công. Không cập nhật gì, kể cả `amount`. Sửa `amount` của giáo viên đang hoạt động dùng API riêng `PATCH /teacher/{publicId}/amount` (Mục 7C). |
| 4 | Số dòng tối đa | **Không giới hạn** (giống addStudent). |
| 5 | Dung lượng file tối đa | **50MB** (`spring.servlet.multipart.max-file-size` / `max-request-size`). |
| 6 | Lọc ngành theo Bộ môn | **Chưa làm**: hệ thống chưa có entity Bộ môn, `Major` chỉ liên kết `Faculty`. Tạm nhận mọi ngành có trong DB (giống addStudent). |
| 7 | Quyền gọi API | `hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')`, đặt `@PreAuthorize` ở `UserServiceImpl` (giống addStudent). |
| 8 | API cũ `POST /teacher` nhận `List<TeacherCreationRequest>` | **Thay thế** bằng API nhận 1 giáo viên. Xóa `UserService.createTeachers` và 2 test cũ của nó. |
| 9 | Cách 3: bấm **Kích hoạt** trên danh sách | Bật `Account.status = true` và **đặt lại `amount` về mặc định theo email** (20 / 10). Giữ nguyên SĐT, đơn vị, ngành, role. Cho phép cả tài khoản TBM. Đang hoạt động sẵn → Báo Vàng `TEACHER_ALREADY_ACTIVE` (dùng lại câu của cách 1, 2). Chi tiết Mục 7B. |
| 10 | `GET /teacher` | Bộ lọc `name` (thay `lastName` cũ, tìm theo họ tên đầy đủ), `organizationType` (`internal` / `external`, thay `organization` cũ), `majorName`, đều chuẩn hóa bằng `TextNormalizer`. Có whitelist sort như `GET /student` (Mục 8.1). Đã sửa cách ghép tên thành **Họ + Tên** (`TRIM(CONCAT(COALESCE(firstName,''), ' ', COALESCE(lastName,'')))`). Response có `publicId` và `status` để FE hiện nút Kích hoạt. Thêm `GET /teacher/{publicId}` cho popup chi tiết (Mục 8.1). |
| 11 | Sửa số lượng hướng dẫn (`PATCH /teacher/{publicId}/amount`) | **Chỉ giáo viên đang hoạt động** (`Account.status = true`, kể cả TBM); giáo viên bị tắt → `TEACHER_INACTIVE`. `amount` bắt buộc, cùng quy tắc với nhập thủ công (số nguyên ≥ 0, không làm tròn, không giới hạn trên). Số mới bằng số cũ **vẫn báo cập nhật thành công**. Chi tiết Mục 7C. |
| 12 | Số điện thoại không trùng | *(Requirement 2026-10-21)* Một SĐT chỉ thuộc về **1 tài khoản duy nhất**, áp dụng chung cho giáo viên và sinh viên (luật trên bảng `account`, tính cả tài khoản đã tắt và mọi role). Trùng với dòng khác trong file → `PHONE_DUPLICATED_IN_FILE` (Giai đoạn 1). Trùng với tài khoản khác trong DB → `ACCOUNT_DUPLICATED` (3014, 409), message liệt kê trường trùng, chỉ kiểm tra khi SĐT sắp được ghi (TH1/TH2, TH5). DB **chưa** đặt `unique` cho cột `phone` vì dữ liệu cũ có thể đang trùng (Mục 11). Chi tiết Mục 5.2. |

---

## 1. BỐI CẢNH NGHIỆP VỤ (DOMAIN CONTEXT)

Bạn đang phát triển Backend (Java 17, Spring Boot 3, Spring Data JPA, Spring Security, MySQL) cho **Hệ thống Quản lý Đồ án Tốt nghiệp**.

* **Tác nhân (Actor):** Trưởng bộ môn (Head of Department).
* **Vòng đời tài khoản theo học kì (Lifecycle):**
  * Đầu mỗi học kì, Trưởng bộ môn thêm danh sách giáo viên tham gia hướng dẫn đồ án của bộ môn mình bằng 2 cách:
    1. **Cách 1: Import từ file Excel (`.xlsx`)** – Dòng nào hợp lệ vẫn được lưu vào Database bình thường (Partial Import); dòng nào lỗi hoặc cảnh báo sẽ trả về trạng thái kèm thông báo tại đúng dòng đó để Frontend tô màu (**Đỏ** hoặc **Vàng**).
    2. **Cách 2: Nhập thủ công từng giáo viên trên Form** – Áp dụng cùng bộ quy tắc kiểm tra và đối chiếu Database như 1 dòng trong file Excel.
  * Khi kết thúc học kì, tài khoản của các giáo viên thông thường sẽ bị thu hồi quyền truy cập (`Account.status = false`). Riêng tài khoản của **Trưởng bộ môn** không bao giờ bị deactive (nhờ cơ chế xử lý tự động của hệ thống).
  * Ở đầu kì mới, khi import danh sách giáo viên:
    * Với **giáo viên mới hoàn toàn** (chưa có trong DB): Hệ thống tạo mới `Account` và `Teacher`.
    * Với **giáo viên cũ từ kì trước** (đã có trong DB và đang có `Account.status = false`): Hệ thống kích hoạt lại tài khoản (`Account.status = true`) và cập nhật các thông tin mềm theo kì mới.
    * Với **giáo viên đang hoạt động sẵn trong kì** (hoặc chính là **Trưởng bộ môn** có tên trong file Excel, tức `Account.status == true`): Hệ thống trả về cảnh báo **Vàng** và bỏ qua không ghi đè.

---

## 2. CẤU TRÚC ENTITY (DATABASE SCHEMA)

Hệ thống tách thông tin định danh đăng nhập (`Account`) và thông tin hồ sơ giáo viên (`Teacher`) thành 2 bảng có quan hệ `1-1`. Lưu ý: Bảng `Teacher` **không có mã định danh riêng** (như `studentCode`) và **không có cột `status` riêng**, mà sử dụng duy nhất `Account.email` làm khóa tra cứu tự nhiên và `Account.status` để quản lý trạng thái hoạt động.

### 2.1. Entity `Account`

```java
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Không cần publicId vì thông tin đã lưu trong Security Context Holder
    @Column(nullable = false, unique = true)
    String email; // Khóa tự nhiên duy nhất dùng để tra cứu

    @Column(name = "first_name", nullable = false)
    String firstName; // Họ và tên đệm (Trường cứng - Immutable)

    @Column(name = "last_name", nullable = false)
    String lastName; // Tên (Trường cứng - Immutable)

    @Column(name = "phone", nullable = false)
    String phone; // Số điện thoại (cho phép cập nhật lại mỗi kì). Không trùng giữa các tài khoản: chặn ở Service (Mục 5.2), DB chưa đặt unique

    @Column(name = "status", nullable = false)
    Boolean status; // true = Đang hoạt động; false = Đã thu hồi sau khi hết kì

    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_name")
    )
    Set<Role> roles = new HashSet<>();
}
```

### 2.2. Entity `Teacher`

```java
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // publicId dùng để truy xuất thông tin
    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "organization", nullable = false)
    String organization; // Đơn vị công tác (Cho phép cập nhật lại mỗi kì)

    // Số lượng sinh viên tối đa hướng dẫn
    @Column(name = "amount", nullable = false)
    Long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    Major major; // Ngành học (Cho phép cập nhật lại mỗi kì)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;
}
```

---

## 3. QUY TẮC GÁN SỐ LƯỢNG SINH VIÊN HƯỚNG DẪN (`Teacher.amount`)

### 3.1. Giá trị mặc định theo tên miền email
* **Giáo viên trong trường:** `email` kết thúc bằng **`@tlu.edu.vn`** → `amount = 20L`.
* **Giáo viên ngoài trường:** `email` kết thúc bằng **`@gmail.com`** → `amount = 10L`.
* **Đuôi khác** (VD `@outlook.com`, email sinh viên `@e.tlu.edu.vn`): bị chặn ở Giai đoạn 1 với lỗi `TEACHER_EMAIL_DOMAIN_INVALID` (Mục 5.1), không tạo tài khoản.
  * Riêng cách 3 (kích hoạt từ danh sách): tài khoản cũ tạo trước khi có luật đuôi email mà không thuộc 2 đuôi trên → coi là ngoài trường (`amount = 10L`) như luật cũ. Các tài khoản này không đi qua được cách 1 / cách 2 nữa vì bị chặn ở Giai đoạn 1.

Email quyết định loại giáo viên (trong / ngoài trường), từ đó suy ra `amount` mặc định và tên trường bắt buộc, nên gom vào 1 enum `TeacherOrganization` (`features/account/service/helper`):

```java
public enum TeacherOrganization {
    INTERNAL(20L),
    EXTERNAL(10L);

    public static final String TLU_NAME = "Trường Đại học Thủy lợi";

    // email đã được chuẩn hóa về chữ thường ở Mục 4.3. Đuôi khác 2 đuôi cho phép -> null
    public static TeacherOrganization ofEmail(String normalizedEmail) {
        if (normalizedEmail.endsWith("@tlu.edu.vn")) return INTERNAL;
        return normalizedEmail.endsWith("@gmail.com") ? EXTERNAL : null;
    }

    // So khớp tên Thủy lợi không phân biệt hoa/thường; canonicalize đổi về đúng TLU_NAME, tên trường khác giữ nguyên
    public static boolean isTluName(String value) { ... }
    public static String canonicalize(String value) { ... }
}
```

### 3.2. Nguồn giá trị theo từng cách thêm

| Cách thêm | TH1 (Tạo mới) | TH5 (Kích hoạt lại) |
| :--- | :--- | :--- |
| **Import Excel** (không có cột `amount`) | mặc định theo email | mặc định theo email (ghi đè giá trị kì trước) |
| **Nhập thủ công**, có truyền `amount` | `request.amount` | `request.amount` |
| **Nhập thủ công**, không truyền `amount` (`null` hoặc chuỗi rỗng) | mặc định theo email | mặc định theo email |
| **Kích hoạt từ danh sách** (cách 3) | *(không áp dụng)* | mặc định theo email (ghi đè giá trị kì trước) |

* Nếu file Excel có thêm cột `amount` (cột thứ 7) → sai cấu trúc tiêu đề (`FILE_HEADER_INVALID`).
* Khi nhập thủ công có truyền `amount`, giá trị phải là **số nguyên ≥ 0**, **không giới hạn trên** (TBM được nhập số khác mặc định 20 / 10 cho từng giáo viên), áp dụng cho **cả** giáo viên trong và ngoài trường. `0` hợp lệ (giáo viên không nhận hướng dẫn, chỉ phản biện / hội đồng). Không làm tròn: số âm, số thập phân (`5.5`, kể cả `5.0`), dạng mũ (`1e2`), chữ (`"abc"`) → `AMOUNT_INVALID`. Chấp nhận cả số JSON (`15`) lẫn chuỗi số (`"15"`). Tối đa 18 chữ số chỉ để nằm trong phạm vi `Long`. Kiểm tra trong pipeline (Mục 5.1), không dùng annotation.
* FE tự điền `amount` theo đuôi email ngay sau khi nhập email; TBM có thể sửa lại trước khi gửi.
* TH4 (đang hoạt động) **không** cập nhật `amount`, kể cả khi nhập thủ công với giá trị khác DB (Mục 5.2).

---

## 4. CẤU TRÚC FILE EXCEL (6 CỘT) & TIỀN XỬ LÝ (GIAI ĐOẠN 0)

### 4.1. Danh sách 6 cột bắt buộc trong Template Excel
File mẫu: `tlu_gpms_system/src/main/resources/templates/teacher_import_template.xlsx`.

Hệ thống chỉ đọc **sheet đầu tiên**. **Dòng 1** là tiêu đề, dữ liệu bắt đầu từ **dòng 2**. Đúng 6 cột theo thứ tự (cột A → F):

| Cột | Tiêu đề | Field | Ghi chú |
| :--- | :--- | :--- | :--- |
| A | **Họ và tên đệm** | `firstName` | Chấp nhận cả tiêu đề "Họ" |
| B | **Tên** | `lastName` | |
| C | **Email** | `email` | **Unique** trong file |
| D | **Số điện thoại** | `phone` | |
| E | **Đơn vị công tác** | `organization` | Ô chữ: tên trường. Email `@tlu.edu.vn` phải ghi `Trường Đại học Thủy lợi` |
| F | **Ngành** | `major` | Dropdown **tên ngành**; BE tra theo tên hoặc mã ngành. Giáo viên ngoài trường để trống |

Tiêu đề được so khớp sau khi chuẩn hóa (Mục 4.3) và **không phân biệt hoa/thường**.

**Cấu trúc file mẫu** (giống file mẫu sinh viên):
* Sheet `Giáo viên` (sheet đầu): dòng tiêu đề cố định (Freeze), cả 6 cột định dạng **Text** (`@`) để giữ số `0` đầu. Data Validation: cột Ngành là dropdown, SĐT độ dài = 10. Cột Đơn vị công tác **không còn dropdown** (file mẫu cũ có dropdown Trong trường / Ngoài trường cần làm lại).
* Sheet `Hướng dẫn`: quy tắc từng cột, ghi rõ hạn mức `amount` được BE tự gán.
* Sheet `DanhMuc` (ẩn): danh sách ngành cho dropdown (named range `DanhSachNganh`).

### 4.2. Kiểm tra mức toàn File (File-level Validation)
Trước khi đọc từng dòng, nếu vi phạm một trong các lỗi sau thì **từ chối toàn bộ file ngay lập tức** (`AppException`, HTTP `400`, code `4000`). Dùng lại các ErrorCode `FILE_*` và `ExcelUtils` đã có:

| Điều kiện | ErrorCode | Message |
| :--- | :--- | :--- |
| File null hoặc rỗng, hoặc request không có part `file` | `FILE_EMPTY` | File import không được để trống |
| File vượt `spring.servlet.multipart.max-file-size` (50MB) | `FILE_TOO_LARGE` | File import vượt quá dung lượng cho phép (50MB) |
| Không có đuôi `.xlsx` hoặc nội dung không phải OOXML thực tế (Magic bytes bằng `FileMagic`), hoặc POI không mở được (file hỏng) | `FILE_INVALID_FORMAT` | File import phải có định dạng .xlsx |
| Không có sheet / dòng tiêu đề không khớp đúng 6 cột chuẩn (thiếu, thừa, sai thứ tự) | `FILE_HEADER_INVALID` | Dòng tiêu đề không đúng với file mẫu |
| Có vùng gộp ô (**Merged Cells**) bắt đầu trong cột A–F | `FILE_MERGED_CELLS` | File import không được chứa ô bị gộp |
| Không còn dòng dữ liệu nào sau khi bỏ dòng trống | `FILE_NO_DATA` | File import không có dòng dữ liệu nào |

**Không giới hạn** số dòng trong 1 lần import.

### 4.3. Quy tắc Tiền xử lý dữ liệu từng ô (Cell Normalization)
Dùng lại `TextNormalizer.normalize` và `ExcelUtils` (giống addStudent Mục 3.3):
* **Bỏ qua dòng trống hoàn toàn (Ghost Rows):** cả 6 cột đều rỗng sau khi chuẩn hóa → bỏ qua, không tính vào `totalRows`.
* **Đọc giá trị dạng Text:** `DataFormatter` để `phone` không mất số `0` đầu và không thành số khoa học.
* **Chuẩn hóa chuỗi**, theo thứ tự: xóa ký tự ẩn (zero-width, BOM) → NBSP thành khoảng trắng → gộp mọi khoảng trắng (gồm tab, xuống dòng) thành 1 rồi `trim()` → NFC.
* **Chuẩn hóa riêng theo cột:** `email` → chữ thường. `organization` khớp (không phân biệt hoa/thường) với `trường đại học thủy lợi` → đổi về đúng `Trường Đại học Thủy lợi` để lưu DB; tên trường khác giữ nguyên như người dùng nhập.
* **Ô công thức:** đọc kết quả đã tính. Công thức lỗi (`#N/A`, `#REF!`, `#VALUE!`, `#NAME?`, `#DIV/0!`...) cho ra chuỗi lỗi đó, nên bị bắt ở bước kiểm tra định dạng của cột.

---

## 5. LUỒNG KIỂM TRA DỮ LIỆU TỪNG DÒNG (ROW-LEVEL VALIDATION PIPELINE)

Quá trình kiểm tra mỗi dòng được chia làm **2 giai đoạn tuần tự**:
* **Giai đoạn 1:** Kiểm tra Cú pháp, Định dạng và Trùng lặp nội bộ trong file Excel.
* **Giai đoạn 2:** Đối chiếu với Database (Chỉ thực hiện khi dòng đó **không có bất kỳ lỗi nào** ở Giai đoạn 1).

### 5.1. Giai đoạn 1: Kiểm tra Cú pháp, Định dạng & Trùng trong File (Fail-fast trên từng cột)

> **QUY TẮC BẮT BUỘC (FAIL-FAST PER COLUMN):**
> Trên **cùng một cột**, hệ thống phải kiểm tra theo thứ tự ưu tiên: **(1) Kiểm tra Trống** $\rightarrow$ **(2) Kiểm tra Định dạng/Danh mục** $\rightarrow$ **(3) Kiểm tra Trùng trong file Excel**.
> Nếu một cột đã vi phạm ở bước trước (ví dụ: bị trống), hệ thống **dừng ngay việc kiểm tra các bước sau của cột đó** và chuyển sang kiểm tra cột tiếp theo. Tuyệt đối không để 1 ô bị trống sinh ra 2 lỗi (vừa lỗi trống vừa lỗi sai Regex).

| Cột dữ liệu | 1. Kiểm tra Trống | 2. Kiểm tra Định dạng & Danh mục | 3. Kiểm tra Trùng trong file Excel |
| :--- | :--- | :--- | :--- |
| **`firstName`** | Không được để trống | Chữ cái tiếng Việt, các từ cách nhau 1 khoảng trắng. Regex: `^\p{IsLatin}+( \p{IsLatin}+)*$` | *Không kiểm tra trùng* |
| **`lastName`** | Không được để trống | Một từ, chỉ chứa chữ cái tiếng Việt. Regex: `^\p{IsLatin}+$` | *Không kiểm tra trùng* |
| **`email`** | Không được để trống | (a) Đúng định dạng email. Regex (sau khi lowercase): `^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,}$`. (b) Đúng định dạng rồi mới xét đuôi: chỉ nhận `@tlu.edu.vn` hoặc `@gmail.com` | Không được trùng với dòng khác trong cùng file |
| **`phone`** | Không được để trống | SĐT Việt Nam: 10 chữ số, bắt đầu bằng `0`. Regex: `^0\d{9}$` | Không được trùng với dòng khác trong cùng file |
| **`organization`** | Không được để trống | (a) Tên trường: chữ, số, khoảng trắng và các dấu `. , - & ( ) ' /`. Regex: `^[\p{IsLatin}\d.,&()'/-]+( [\p{IsLatin}\d.,&()'/-]+)*$`. (b) Phải khớp với email: `@tlu.edu.vn` ⇔ `Trường Đại học Thủy lợi`, `@gmail.com` ⇔ tên khác | *Không kiểm tra trùng* |
| **`major`** | **Trong trường:** không được để trống. **Ngoài trường:** phải để trống. Email lỗi mà ngành trống → không báo lỗi ngành | Trong trường: tên ngành hoặc mã ngành phải tồn tại trong DB (không phân biệt hoa/thường). *Chưa lọc theo Bộ môn, xem Mục 0 #6*. Ngoài trường mà có điền → `MAJOR_NOT_ALLOWED` | *Không kiểm tra trùng* |
| **`amount`** *(chỉ nhập thủ công)* | Được bỏ trống (→ tự gán, Mục 3) | Số nguyên ≥ 0, không làm tròn, không giới hạn trên | *Không áp dụng* |

* Mọi cột chuỗi dài quá **255 ký tự** (`VARCHAR(255)`) bị coi là lỗi định dạng của cột đó, để 1 dòng không làm lỗi SQL cả lô.
* Kiểm tra (b) của `organization` chỉ chạy khi cả ô `email` và ô `organization` đã hợp lệ ở các bước trước. Lệch → lỗi gắn vào cột `organization` (VD: `@gmail.com` mà ghi `Trường Đại học Thủy lợi`, hoặc `@tlu.edu.vn` mà ghi trường khác → Báo Đỏ).

#### Bảng ErrorCode của Giai đoạn 1 (HTTP `400`, code `4000`)

| Cột | Lỗi Trống | Lỗi Định dạng / Danh mục | Lỗi Trùng trong file |
| :--- | :--- | :--- | :--- |
| `firstName` | `FIRST_NAME_EMPTY` – Họ không được để trống | `FIRST_NAME_INVALID` – Họ chỉ được chứa chữ cái và khoảng trắng | |
| `lastName` | `LAST_NAME_EMPTY` – Tên không được để trống | `LAST_NAME_INVALID` – Tên chỉ được chứa chữ cái | |
| `email` | `EMAIL_EMPTY` – Email không được để trống | `EMAIL_INVALID` – Email không đúng định dạng<br>**(mới)** `TEACHER_EMAIL_DOMAIN_INVALID` – Email giáo viên phải có đuôi @tlu.edu.vn (trong trường) hoặc @gmail.com (ngoài trường) | `EMAIL_DUPLICATED_IN_FILE` – Email bị trùng với dòng khác trong file |
| `phone` | `PHONE_EMPTY` – Số điện thoại không được để trống | `PHONE_INVALID` – Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 | **(mới)** `PHONE_DUPLICATED_IN_FILE` – Số điện thoại bị trùng với dòng khác trong file |
| `organization` | `ORGANIZATION_EMPTY` – Đơn vị công tác không được để trống | (a) **(mới)** `ORGANIZATION_INVALID` – Đơn vị công tác chỉ gồm chữ, số, khoảng trắng và các dấu . , - & ( ) ' / (tối đa 255 ký tự)<br>(b) **(mới)** `ORGANIZATION_NOT_MATCH_EMAIL` – Đơn vị công tác không khớp với email (email @tlu.edu.vn phải là Trường Đại học Thủy lợi, email @gmail.com phải là trường khác) | |
| `major` | `MAJOR_EMPTY` – Ngành học không được để trống *(chỉ trong trường)* | `MAJOR_INVALID` – Ngành học không tồn tại trong hệ thống<br>**(mới)** `MAJOR_NOT_ALLOWED` – Giáo viên ngoài trường không được điền ngành | |
| `amount` | | `AMOUNT_INVALID` – Số lượng sinh viên hướng dẫn phải là số nguyên không âm *(sửa message)* | |
| *(≥ 2 lỗi)* | `ROW_INFO_INVALID` – `<các trường sai> không hợp lệ, vui lòng kiểm tra lại` (VD: Tên và Số điện thoại không hợp lệ, vui lòng kiểm tra lại) *(dùng chung với sinh viên)* | | |

#### Lưu ý về Kiểm tra trùng email / số điện thoại trong file
* Áp dụng cho 2 cột `email` và `phone`, mỗi cột đếm riêng.
* Chỉ đếm trùng trên các ô **đã qua bước Trống và Định dạng**. Ô đã có lỗi thì không tham gia đếm và không bị gắn thêm lỗi trùng. Dùng `DuplicateUtils.findDuplicateIndexes`.
* Nếu một giá trị xuất hiện $\ge 2$ lần trong cùng file, **tất cả các dòng mang giá trị đó đều bị gắn lỗi trùng** (không tự ý lấy dòng đầu tiên).
* Một dòng trùng cả email lẫn SĐT với dòng khác → có 2 lỗi → gom thành `ROW_INFO_INVALID`: "Email và Số điện thoại không hợp lệ, vui lòng kiểm tra lại".

#### Quy tắc Gom lỗi cuối Giai đoạn 1 (Error Aggregation Rule)
Sau khi kiểm tra xong các cột của một dòng, gọi tập lỗi thu được là `rowErrors` (`EnumMap<TeacherColumn, ErrorCode>`, mỗi cột tối đa 1 lỗi):
* `rowErrors.size() == 1` → **`ERROR` (Báo Đỏ)**, message là message của chính lỗi đó. Không chạy Giai đoạn 2.
* `rowErrors.size() >= 2` → **`ERROR` (Báo Đỏ)**, message (`ROW_INFO_INVALID`) liệt kê **tất cả** các trường bị lỗi theo thứ tự Họ, Tên, Email, Số điện thoại, Đơn vị công tác, Ngành, Số lượng hướng dẫn; ngăn cách bằng dấu phẩy, trường cuối nối bằng "và". VD: `"Tên và Số điện thoại không hợp lệ, vui lòng kiểm tra lại"`. Không chạy Giai đoạn 2.
* `rowErrors.isEmpty()` → chuyển sang **Giai đoạn 2**.

---

### 5.2. Giai đoạn 2: Đối chiếu với Database (Database Reconciliation)

Với mỗi dòng đã vượt qua Giai đoạn 1, hệ thống tra cứu theo `email` (dữ liệu đã load sẵn ở Mục 6.1):
* `existingAccount`: Bản ghi `Account` tìm theo `email`.
* `existingTeacher`: Bản ghi `Teacher` liên kết với `existingAccount` (`teacher.account.id == existingAccount.id`).
* `accountsByPhone`: các `Account` đang giữ SĐT của dòng (`Map<String, List<Account>>`, dùng `List` vì dữ liệu cũ có thể đã trùng). Dùng cho luật SĐT không trùng ở cuối mục này.

Đầu tiên hệ thống xác định dòng có phải **chính giáo viên cũ quay lại** hay không: `existingTeacher != null` (email thuộc một tài khoản có hồ sơ `Teacher`) **VÀ** khớp **2 trường cứng** giữa dữ liệu nhập và `Account` trong DB:
1. `firstName`
2. `lastName`

Cách so sánh: chuẩn hóa **cả giá trị trong DB** bằng `TextNormalizer.normalize`, rồi so khớp **không phân biệt hoa/thường** (`equalsIgnoreCase`).
> Khác addStudent (so khớp phân biệt hoa/thường). Đây là chủ ý: tên giáo viên thường được nhập tay bởi nhiều người, chỉ lệch hoa/thường thì vẫn coi là cùng một người.

* Đúng là giáo viên cũ → đi tiếp **TH4 / TH5**.
* Không phải → đi **TH2** (có trường trùng) hoặc **TH1** (không trùng gì).

#### Trường hợp 2: Trùng định danh với tài khoản đã có
Gom các trường định danh của dòng đang trùng với tài khoản đã có trong DB, **theo thứ tự cố định**:
1. `Email`: `existingAccount != null` (email của sinh viên, admin, hoặc giáo viên nhưng lệch họ / tên).
2. `Số điện thoại`: SĐT đã thuộc **bất kỳ** tài khoản nào (xem *Luật SĐT không trùng*), kể cả chính tài khoản trùng email.

Có ít nhất 1 trường trùng:
* **Kết quả:** Lỗi – Trạng thái **`ERROR` (Báo Đỏ)**. Không lưu vào Database.
* **ErrorCode:** `ACCOUNT_DUPLICATED`, code `3014`, HTTP `409` *(dùng chung với sinh viên)*.
* **Message:** `"<các trường trùng> trùng với tài khoản đã tồn tại trong hệ thống"`:

| Trường bị trùng | Message |
|---|---|
| Email | Email trùng với tài khoản đã tồn tại trong hệ thống |
| SĐT | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| Email + SĐT | Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |

#### Trường hợp 1: Giáo viên hoàn toàn mới (không trường nào trùng)
* **Kết quả:** Hợp lệ – Trạng thái **`SUCCESS`**.
* **Hành động:**
  1. Tạo mới `Account` với `email`, `firstName`, `lastName`, `phone`, `status = true`, gán Role **`SUPERVISOR`** (Role được load 1 lần; nếu không có trong DB thì ném `ROLE_NOT_EXIST` và từ chối cả request).
  2. Tạo mới `Teacher` với `organization`, `major = resolvedMajor`, `amount` (theo Mục 3.2), `account = newAccount`.

#### Trường hợp 3: (đã gộp vào TH2)
Trước đây TH3 (đã là giáo viên nhưng lệch họ / tên) báo `"Thông tin không khớp với hồ sơ gốc trong hệ thống"` (`TEACHER_PROFILE_MISMATCH` 3007). Nay báo theo TH2, message `"Email trùng với tài khoản đã tồn tại trong hệ thống"` (thêm `Số điện thoại` nếu SĐT cũng đã có người giữ).

#### Trường hợp 4: Khớp trường cứng NHƯNG đang hoạt động sẵn (`existingAccount.status == true`)
Bao gồm: giáo viên đã được active trong kì này (import lại lần 2, hoặc đã nhập thủ công trước đó), **và** dòng đó là tài khoản của **Trưởng bộ môn** (TBM có bản ghi `teacher` và luôn có `status == true`, xem Mục 0 #2).
* **Kết quả:** Cảnh báo – Trạng thái **`WARNING` (Báo Vàng)**.
* **Message:** `"Thông tin giáo viên đã có trong hệ thống"` (**(mới)** `TEACHER_ALREADY_ACTIVE`, code `1100`, HTTP `200`). Dùng chung cho import Excel và nhập thủ công.
* **Hành động:** Bỏ qua, **không** cập nhật gì vào DB, kể cả khi `phone`, `organization`, `major`, `amount` trong dữ liệu nhập khác DB. Muốn sửa `amount` của giáo viên đang hoạt động thì dùng `PATCH /teacher/{publicId}/amount` (Mục 7C); sửa SĐT, ngành chưa có API.
* Vì không ghi gì nên **không** kiểm tra luật SĐT không trùng: SĐT trong dòng trùng với tài khoản khác vẫn chỉ báo Vàng.

#### Trường hợp 5: Khớp trường cứng VÀ cần kích hoạt lại (`existingAccount.status == false`)
* **Kiểm tra SĐT trước:** SĐT đã thuộc về một tài khoản **khác** (không tính chính tài khoản này) → **`ERROR` (Báo Đỏ)** `ACCOUNT_DUPLICATED`, message `"Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống"`, không kích hoạt, không lưu.
* **Kết quả:** Hợp lệ – Trạng thái **`SUCCESS`**.
* **Hành động:**
  1. `existingAccount.setStatus(true)`. Không đụng tới role (cuối kì chỉ tắt `status`, role vẫn giữ nguyên).
  2. Cập nhật **4 trường mềm** cho kì mới:
     * `existingAccount.setPhone(row.getPhone())`
     * `existingTeacher.setOrganization(row.getOrganization())`
     * `existingTeacher.setMajor(resolvedMajor)` (ngoài trường: `null`; so sánh id ngành null-safe khi dựng message)
     * `existingTeacher.setAmount(...)` theo Mục 3.2
  3. So sánh 4 trường mềm với DB **trước khi ghi đè** để dựng message, theo thứ tự: `Số điện thoại`, `Đơn vị công tác`, `Ngành`, `Số lượng hướng dẫn`:
     * Không trường nào khác DB → `"Đã kích hoạt lại tài khoản giáo viên"`
     * Có trường khác DB → `"Đã kích hoạt lại và cập nhật: <các trường cách nhau dấu phẩy>"`, VD `"Đã kích hoạt lại và cập nhật: Số điện thoại, Số lượng hướng dẫn"`
     * Dùng chung cho nhập thủ công (message của response) và import Excel (cột `message` của dòng xanh). TH1 trả `"Thêm giáo viên thành công"`.

#### Luật SĐT không trùng (dùng chung với thêm sinh viên)
* Một SĐT chỉ thuộc về **1 tài khoản duy nhất** trong bảng `account`, tính cả tài khoản đã tắt (`status = false`) và mọi role (sinh viên, giáo viên, admin...). VD: SĐT đang là của 1 sinh viên → không gán được cho giáo viên.
* Chỉ kiểm tra khi SĐT **sắp được ghi vào DB**: TH1/TH2 (không phải giáo viên cũ) và TH5 (kích hoạt lại). TH4 không ghi gì nên bỏ qua.
* So sánh SĐT đã chuẩn hóa (Mục 4.3). TH5 bỏ qua chính tài khoản đang kích hoạt, nên giữ nguyên SĐT cũ vẫn hợp lệ.
* Vi phạm → **`ERROR` (Báo Đỏ)**, `ACCOUNT_DUPLICATED` (code `3014`, HTTP `409`), message có `Số điện thoại` (TH2 có thể kèm `Email`, VD `"Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống"`). Nhập thủ công trả `409`.
* Map `accountsByPhone` chỉ load **trước** khi lưu, nên 2 giáo viên trong cùng file đổi SĐT cho nhau (A lấy số cũ của B, B lấy số cũ của A) vẫn bị chặn, dù sau khi lưu xong không còn trùng.
* Cách 3 (Kích hoạt từ danh sách, Mục 7B) và API sửa số lượng (Mục 7C) không đổi SĐT nên không kiểm tra.

---

## 6. CƠ CHẾ LƯU DỮ LIỆU & TỐI ƯU HIỆU NĂNG (BATCH PROCESSING)

### 6.1. Yêu cầu chống N+1 Query (Bắt buộc)
Tuyệt đối **KHÔNG** gọi query (`findByEmail`, `findByAccountId`, `findByMajor`...) bên trong vòng lặp từng dòng. Gom truy vấn theo batch trước khi lặp:

1. **Query 1 (Load Majors):** `majorRepository.findAll()` (chưa lọc theo Bộ môn, Mục 0 #6) → `Map<String, Major> majorMap`. Mỗi ngành có 2 key: tên ngành viết thường, và mã ngành viết thường (nếu `code` khác rỗng). **Key tên ngành được ưu tiên**: mã ngành không được ghi đè key đã có (dùng `putIfAbsent` khi thêm key mã ngành, sau khi đã thêm hết key tên).
2. **Query 2 (Load Accounts by Email):** gom `email` của các dòng qua Giai đoạn 1 vào `Set<String> emails`, gọi `accountRepository.findAllByEmailIn(emails)` (đã có sẵn).
   ```sql
   SELECT a FROM Account a WHERE a.email IN :emails
   ```
   **Không** dùng `LOWER(a.email)`: email đã lưu dạng chữ thường, collation `*_ci` của MySQL vốn không phân biệt hoa/thường, và `LOWER()` làm mất index. Đưa vào `Map<String, Account> accountByEmailMap`.
3. **Query 3 (Load Teachers by Account):** gom `id` của các account tìm được ở Query 2, gọi 1 câu JPQL có `JOIN FETCH`:
   ```sql
   SELECT t FROM Teacher t
   JOIN FETCH t.account
   JOIN FETCH t.major
   WHERE t.account.id IN :accountIds
   ```
   Đưa vào `Map<Long, Teacher> teacherByAccountIdMap`. Bỏ qua query này nếu Query 2 không trả về account nào.
4. **Query 4 (Load Accounts by Phone):** gom `phone` của các dòng qua Giai đoạn 1, gọi `accountRepository.findAllByPhoneIn(phones)`:
   ```sql
   SELECT a FROM Account a WHERE a.phone IN :phones
   ```
   Gom theo SĐT đã chuẩn hóa vào `Map<String, List<Account>> accountsByPhone` (dùng cho luật SĐT không trùng, Mục 5.2).
5. **Query 5 (Load Role):** `roleRepository.findById("SUPERVISOR")`, chạy 1 lần.

> Query 2–5 chỉ chạy khi có ít nhất 1 dòng qua được Giai đoạn 1.

### 6.2. Quản lý Transaction cho Partial Import
* Phân loại các dòng thành `accountsToSave`, `teachersToSave`, `rowResults` hoàn toàn trên bộ nhớ, không ném Exception làm đứt luồng.
* Kết thúc vòng lặp: nếu danh sách khác rỗng thì `accountRepository.saveAll(accountsToSave)` rồi `teacherRepository.saveAll(teachersToSave)` trong cùng 1 Transaction (`@Transactional` trên method service).
* TH5 sửa trực tiếp entity đã load, rồi cũng đưa vào 2 danh sách trên.
* **Hạn chế đã biết:** 2 lần import chạy đồng thời cùng chèn một `email` → vi phạm unique, rollback **toàn bộ lô** và trả `500`.
* **Hạn chế đã biết:** 2 lần import / nhập tay chạy đồng thời cùng ghi một SĐT → cả 2 đều qua kiểm tra và cùng lưu được, vì cột `phone` chưa có `unique` (Mục 11).

---

## 7. ĐẶC TẢ CHO CÁCH 2: NHẬP THỦ CÔNG TỪNG GIÁO VIÊN (MANUAL ENTRY)

Request `TeacherCreationRequest` được chuyển thành 1 dòng (`TeacherRowData.fromRequest`, `rowNumber = 0`) và đi qua **đúng cùng pipeline** với import Excel.
* Request **không dùng** `@Valid`/`@NotBlank`/`@NotNull`/`@Min`: mọi kiểm tra nằm trong pipeline, để có cùng thứ tự và cùng quy tắc gom lỗi (`GlobalException` chỉ trả lỗi đầu tiên của annotation).
* Field `major` (String) nhận **tên hoặc mã ngành**, thay cho `majorId` cũ (không để lộ id nội bộ).
* Field `amount` (`String`, không bắt buộc), xem Mục 3.2. Nhận dạng chuỗi thô vì nếu khai báo `Long` thì Jackson tự làm tròn `5.5` → `5` và lỗi parse `"abc"` chỉ ra lỗi chung `INVALID_REQUEST` (1006, 400), không gom được với lỗi các trường khác; nay validator tự kiểm tra rồi trả `AMOUNT_INVALID` và gom chung với lỗi các trường khác.
* Bước "Trùng trong file" không có tác dụng vì chỉ có 1 dòng.

```java
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherCreationRequest {
    String firstName;
    String lastName;
    String email;
    String phone;
    String organization;
    String major;   // Tên hoặc mã ngành
    String amount;  // Số nguyên >= 0 dạng số hoặc chuỗi; null / "" → BE tự gán theo email
}
```

| Kết quả | HTTP | `code` | Message |
| :--- | :--- | :--- | :--- |
| **TH1** (Mới) – lưu DB | `200` | `1000` | Thêm giáo viên thành công |
| **TH5** (Kích hoạt lại), thông tin nhập trùng hết với DB | `200` | `1000` | Đã kích hoạt lại tài khoản giáo viên |
| **TH5** (Kích hoạt lại), có trường mềm khác DB | `200` | `1000` | Đã kích hoạt lại và cập nhật: Số điện thoại, Số lượng hướng dẫn *(liệt kê đúng các trường thay đổi, Mục 5.2 TH5)* |
| **TH4** (Đang hoạt động sẵn / TBM) – Báo Vàng, không lưu | `200` | `1100` | Thông tin giáo viên đã có trong hệ thống |
| Giai đoạn 1 có đúng 1 lỗi | `400` | `4000` | Message của lỗi cụ thể (bảng ErrorCode Mục 5.1) |
| Giai đoạn 1 có $\ge 2$ lỗi | `400` | `4000` | `<các trường sai> không hợp lệ, vui lòng kiểm tra lại` |
| **TH2** (Email thuộc tài khoản khác, hoặc giáo viên lệch họ / tên) | `409` | `3014` | Email trùng với tài khoản đã tồn tại trong hệ thống |
| **TH2** (Email + SĐT trùng) | `409` | `3014` | Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| **TH1 / TH5**, SĐT đã thuộc tài khoản khác | `409` | `3014` | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| Role `SUPERVISOR` không có trong DB | `404` | `2007` | Role không tồn tại |

FE phân biệt Xanh / Vàng bằng `code` trong body (`1000` hoặc `1100`), vì cả hai đều trả HTTP `200`.

---

## 7B. ĐẶC TẢ CHO CÁCH 3: KÍCH HOẠT TỪ DANH SÁCH

TBM tìm giáo viên trên màn hình danh sách (`GET /teacher`), rồi bấm **Kích hoạt** ở cột thao tác của giáo viên đang tắt (`status = false`). FE gọi `PATCH /teacher/{publicId}/activate`, với `publicId` lấy từ dòng trong danh sách.

**Xử lý ở BE** (`UserServiceImpl.activateTeacher`, `@Transactional(rollbackFor = Exception.class)`):
1. Parse `publicId`. Sai định dạng UUID → `TEACHER_NOT_FOUND`, không query DB.
2. `teacherRepository.findByPublicId` không có → `TEACHER_NOT_FOUND`.
3. `Account.status == true` (gồm cả TBM) → trả `false`, **không cập nhật gì**.
4. Ngược lại:
   * Bật `Account.status = true`.
   * Đặt lại `Teacher.amount = TeacherOrganization.ofEmail(email).getDefaultAmount()` (20 trong trường / 10 ngoài trường).
   * Giữ nguyên SĐT, đơn vị công tác, ngành, role. Muốn đổi thì dùng nhập thủ công (cách 2, TH5).
   * Lưu `Account` và `Teacher`.

Giáo viên không có cột `status` riêng (khác sinh viên), nên chỉ dựa vào `Account.status`.

| Trường hợp | HTTP | `code` | `message` |
| :--- | :--- | :--- | :--- |
| Kích hoạt thành công | `200` | `1000` | Đã kích hoạt lại tài khoản giáo viên |
| Đang hoạt động sẵn (kể cả TBM) – Báo Vàng | `200` | `1100` | Thông tin giáo viên đã có trong hệ thống |
| `publicId` không tồn tại hoặc sai định dạng | `404` | `3009` | Giáo viên không tồn tại |
| Chưa đăng nhập | `401` | | |
| Không phải ADMIN / TBM | `403` | `2001` | |

---

## 7C. SỬA SỐ LƯỢNG SINH VIÊN HƯỚNG DẪN CỦA GIÁO VIÊN ĐANG HOẠT ĐỘNG

TBM sửa `amount` của 1 giáo viên trên màn hình danh sách. FE gọi `PATCH /teacher/{publicId}/amount` với body `{"amount": 15}` (`TeacherAmountRequest`). `amount` có kiểu `String` giống Mục 7 để không bị Jackson làm tròn; FE gửi số `15` hay chuỗi `"15"` đều được.

**Xử lý ở BE** (`UserServiceImpl.updateTeacherAmount`, `@Transactional(rollbackFor = Exception.class)`). Kiểm tra dữ liệu nhập trước, rồi mới truy vấn DB:
1. Chuẩn hóa `amount` (`TextNormalizer.normalize`). Rỗng / `null` → `AMOUNT_NULL`.
2. Không phải số nguyên ≥ 0 (`TeacherRowValidator.isValidAmount`, dùng chung với nhập thủ công) → `AMOUNT_INVALID`. Không làm tròn: `5.5`, `5.0`, `-1`, `"abc"` đều lỗi.
3. Parse `publicId`. Sai định dạng UUID → `TEACHER_NOT_FOUND`, không query DB.
4. `teacherRepository.findByPublicId` không có → `TEACHER_NOT_FOUND`.
5. `Account.status != true` → `TEACHER_INACTIVE`. Lý do: kích hoạt lại (cách 3) hoặc import lại sẽ đặt `amount` về mặc định, nên sửa trước khi kích hoạt cũng không giữ được.
6. Gán `Teacher.amount` mới rồi lưu. Số mới bằng số cũ vẫn lưu và báo thành công. Không đổi SĐT, đơn vị, ngành, role.

| Trường hợp | HTTP | `code` | `message` |
| :--- | :--- | :--- | :--- |
| Cập nhật thành công (kể cả số không đổi) | `200` | `1000` | Cập nhật số lượng hướng dẫn thành công |
| `amount` trống / `null` | `400` | `4000` | Số lượng sinh viên hướng dẫn không được để trống |
| `amount` không phải số nguyên ≥ 0 | `400` | `4000` | Số lượng sinh viên hướng dẫn phải là số nguyên không âm |
| `publicId` không tồn tại hoặc sai định dạng | `404` | `3009` | Giáo viên không tồn tại |
| Giáo viên bị tắt | `409` | `3012` | Giáo viên không hoạt động trong kì này, không thể cập nhật số lượng hướng dẫn |
| Chưa đăng nhập | `401` | | |
| Không phải ADMIN / TBM | `403` | `2001` | |

---

## 8. THIẾT KẾ CONTRACT API

### 8.1. Endpoint

Cả 6 endpoint yêu cầu quyền `hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')` (`@PreAuthorize` ở `UserServiceImpl`). Không có quyền → `403 FORBIDDEN`.

| Method | URL | Request | Response `data` |
| :--- | :--- | :--- | :--- |
| `GET` | `/teacher` | Query `name`, `organizationType`, `majorName` (không bắt buộc) + `page`, `size` (mặc định `10`), `sort` (whitelist) | `Page<TeacherResponse>` |
| `GET` | `/teacher/{publicId}` | *(không có)* | `TeacherDetailResponse` |
| `POST` | `/teacher/import` | `multipart/form-data`, part `file` (.xlsx) | `TeacherImportResponse` |
| `POST` | `/teacher` | JSON `TeacherCreationRequest` (1 giáo viên) | *(không có)* |
| `PATCH` | `/teacher/{publicId}/activate` | *(không có body)* | *(không có)* |
| `PATCH` | `/teacher/{publicId}/amount` | JSON `TeacherAmountRequest` `{"amount": 15}` | *(không có)* |

* `GET /teacher` (màn hình "Giáo viên hướng dẫn" của TBM) trả cả giáo viên đang hoạt động và đã tắt. Mỗi dòng `TeacherResponse`: `publicId`, `name` (Họ + Tên), `majorName` (Khoa phụ trách, có thể `null`), `organization`, `phone`, `amount`, `status`.
  * **Bộ lọc** (tìm gần đúng, không phân biệt hoa/thường; Service chuẩn hóa bằng `TextNormalizer.normalize`, chuỗi rỗng / toàn khoảng trắng coi như không lọc):
    * `name`: tìm trên chuỗi **Họ + Tên** ghép lại, nên gõ `An`, `Nguyễn` hay `Nguyễn Văn An` đều ra "Nguyễn Văn An". Thay cho tham số `lastName` cũ (chỉ tìm theo Tên).
    * `majorName`: tên khoa / ngành.
    * `organizationType` (thay cho `organization` cũ, **không** tìm gần đúng): `internal` → chỉ giáo viên có `organization = 'Trường Đại học Thủy lợi'` (`TeacherOrganization.TLU_NAME`); `external` → mọi trường khác. Không phân biệt hoa/thường, trống → không lọc. Giá trị khác → `400`, `1006` `INVALID_REQUEST` (không query DB). Service đổi sang `Boolean internal` (`TeacherOrganization.fromFilter`) rồi truyền xuống `TeacherRepository.findAllTeachers`.
  * **Sắp xếp** (`?sort=<key>,asc|desc`, gửi được nhiều lần): chỉ nhận các key trong whitelist, key khác → `400`, `1008` `INVALID_SORT_FIELD` "Trường sắp xếp không hợp lệ" (không query DB).

    | Key FE gửi | Sắp theo |
    | :--- | :--- |
    | `name` | Tên rồi đến Họ (`a.lastName`, `a.firstName`) |
    | `organization` | `t.organization` |
    | `majorName` | `m.name` |
    | `amount` | `t.amount` |

    Không gửi `sort` → mặc định `a.status DESC, t.id DESC` (đang hoạt động trước, mới thêm trước). Có gửi `sort` → luôn chốt thêm `t.id DESC` để phân trang ổn định khi trùng giá trị.
* `GET /teacher/{publicId}` (popup chi tiết khi bấm 1 dòng) trả `TeacherDetailResponse`: các field trên + `email`. `publicId` sai định dạng UUID hoặc không có → `404`, `3009` `TEACHER_NOT_FOUND` (sai định dạng thì không query DB).
* `/teacher/import` luôn trả HTTP `200`, `code = 1000`, message `"Xử lý file import hoàn tất"`, kèm kết quả từng dòng. Chỉ lỗi mức toàn file (Mục 4.2) hoặc `ROLE_NOT_EXIST` mới không trả `200`.
* API cũ `POST /teacher` nhận `List<TeacherCreationRequest>` **bị thay thế** (Mục 0 #8).

```json
// POST /teacher
{
  "firstName": "Nguyễn Văn",
  "lastName": "An",
  "email": "an.nv@tlu.edu.vn",
  "phone": "0987654321",
  "organization": "Trường Đại học Thủy lợi",
  "major": "Kỹ thuật phần mềm",
  "amount": 15
}
```

### 8.2. DTO

```java
public enum RowImportStatus {  // Dùng lại common/enums/RowImportStatus
    SUCCESS, // Hợp lệ -> Đã lưu DB (Tạo mới hoặc Kích hoạt lại)
    WARNING, // Báo Vàng -> Đã active sẵn trong kì (hoặc là Trưởng bộ môn), bỏ qua không lưu
    ERROR    // Báo Đỏ -> Lỗi dữ liệu hoặc xung đột DB, không lưu
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherImportRowResult {
    private int rowNumber;            // Số dòng thật trên Excel (tiêu đề = 1, dữ liệu đầu tiên = 2)
    // Các field dữ liệu bên dưới là giá trị ĐÃ CHUẨN HÓA (Mục 4.3)
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String organization;
    private String majorName;
    private Long amount;              // SUCCESS: giá trị vừa gán; WARNING: giá trị hiện có trong DB; ERROR: null
    private RowImportStatus status;   // SUCCESS | WARNING | ERROR
    private String message;           // SUCCESS: thêm mới / kích hoạt lại (kèm trường cập nhật); WARNING, ERROR: nội dung cảnh báo / lỗi
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherImportResponse {
    private int totalRows;            // Tổng số dòng có dữ liệu đã xử lý (không tính dòng trống)
    private int successCount;         // Số dòng tạo mới / kích hoạt lại thành công
    private int warningCount;         // Số dòng báo vàng
    private int errorCount;           // Số dòng báo đỏ
    private List<TeacherImportRowResult> rowResults; // Chi tiết từng dòng để FE render bảng
}
```

---

## 9. ERRORCODE CẦN THÊM

| ErrorCode | code | HTTP | Message |
| :--- | :--- | :--- | :--- |
| `ORGANIZATION_INVALID` | `4000` | `400` | Đơn vị công tác chỉ gồm chữ, số, khoảng trắng và các dấu . , - & ( ) ' / (tối đa 255 ký tự) |
| `TEACHER_EMAIL_DOMAIN_INVALID` | `4000` | `400` | Email giáo viên phải có đuôi @tlu.edu.vn (trong trường) hoặc @gmail.com (ngoài trường) |
| `ORGANIZATION_NOT_MATCH_EMAIL` | `4000` | `400` | Đơn vị công tác không khớp với email (email @tlu.edu.vn phải là Trường Đại học Thủy lợi, email @gmail.com phải là trường khác) |
| `MAJOR_NOT_ALLOWED` | `4000` | `400` | Giáo viên ngoài trường không được điền ngành |
| `ROW_INFO_INVALID` | `4000` | `400` | `<các trường sai> không hợp lệ, vui lòng kiểm tra lại`, các trường theo thứ tự cột *(dùng chung với sinh viên)* |
| `PHONE_DUPLICATED_IN_FILE` | `4000` | `400` | Số điện thoại bị trùng với dòng khác trong file *(dùng chung với sinh viên)* |
| `ACCOUNT_DUPLICATED` | `3014` | `409` | `<các trường trùng> trùng với tài khoản đã tồn tại trong hệ thống`, các trường theo thứ tự Email, Số điện thoại *(dùng chung với sinh viên)* |
| `TEACHER_ALREADY_ACTIVE` | `1100` | `200` | Thông tin giáo viên đã có trong hệ thống |
| `TEACHER_NOT_FOUND` | `3009` | `404` | Giáo viên không tồn tại |
| `TEACHER_INACTIVE` | `3012` | `409` | Giáo viên không hoạt động trong kì này, không thể cập nhật số lượng hướng dẫn |

Sửa message: `AMOUNT_INVALID` → "Số lượng sinh viên hướng dẫn phải là số nguyên không âm" (chỉ giáo viên dùng mã này).

Các mã khác dùng lại: `FILE_*`, `FIRST_NAME_*`, `LAST_NAME_*`, `EMAIL_*`, `PHONE_*`, `ORGANIZATION_EMPTY`, `MAJOR_EMPTY`, `MAJOR_INVALID`, `ROLE_NOT_EXIST`, `AMOUNT_NULL` (API sửa số lượng, Mục 7C). Sau khi làm xong có thể xóa `MAJOR_ID_NULL` nếu không còn chỗ nào dùng.

---

## 10. CẤU TRÚC CODE DỰ KIẾN

| File | Vai trò |
| :--- | :--- |
| `features/account/controller/UserController.java` | `GET /teacher`, `GET /teacher/{publicId}`, `POST /teacher/import`, `POST /teacher`, `PATCH /teacher/{publicId}/activate`, `PATCH /teacher/{publicId}/amount` |
| `features/account/dto/response/TeacherDetailResponse.java` | Popup chi tiết (`GET /teacher/{publicId}`), lấy bằng DTO projection `TeacherRepository.findTeacherDetail` |
| `features/account/service/UserService.java` (+ `impl/UserServiceImpl.java`) | `importTeachers(MultipartFile)`, `createTeacher(TeacherCreationRequest)` dùng chung `processTeacherRows()`; `activateTeacher(String publicId)` cho cách 3; `updateTeacherAmount(String publicId, TeacherAmountRequest)` (Mục 7C) |
| `features/account/service/helper/TeacherExcelReader.java` | Kiểm tra tiêu đề / ô gộp, đọc sheet đầu thành `List<TeacherRowData>` |
| `features/account/service/helper/TeacherRowValidator.java` | Giai đoạn 1; `isValidAmount` dùng chung cho nhập thủ công và API sửa số lượng |
| `features/account/dto/request/TeacherAmountRequest.java` | Body của `PATCH /teacher/{publicId}/amount` |
| `features/account/service/helper/TeacherColumn.java`, `TeacherRowData.java` | Enum 6 cột (+ `AMOUNT` cho nhập thủ công), dữ liệu 1 dòng |
| `features/account/service/helper/TeacherOrganization.java` | 2 giá trị đơn vị công tác, suy ra từ email, kèm `amount` mặc định |
| `features/account/repository/TeacherRepository.java` | `findAllByAccountIdIn` (Query 3); `findAllTeachers` dùng chung điều kiện `TEACHER_LIST_FILTER` cho query danh sách và query đếm, không có `ORDER BY` cố định |
| `features/account/repository/AccountRepository.java` | `findAllByEmailIn` (Query 2), `findAllByPhoneIn` (Query 4, luật SĐT không trùng) |
| `UserServiceImpl` – `groupAccountsByPhone`, `isPhoneTakenByOther` | Luật SĐT không trùng (Mục 5.2), dùng chung với thêm sinh viên |
| `UserServiceImpl` – `TEACHER_SORT_FIELDS`, `toWhitelistedPageable` | Whitelist sort `GET /teacher`; `toWhitelistedPageable` dùng chung với `GET /student` |
| `features/account/dto/response/TeacherImportResponse.java`, `TeacherImportRowResult.java` | Response Mục 8.2 |
| `features/account/dto/response/TeacherCreationResult.java` | Kết quả nhập thủ công (`code` + `message`), controller gói vào `APIResponse<Void>` |
| `src/main/resources/templates/teacher_import_template.xlsx` | File Excel mẫu |
| Dùng lại | `ExcelUtils`, `TextNormalizer`, `DuplicateUtils`, `RowImportStatus`, handler `FILE_TOO_LARGE` / `FILE_EMPTY` / lỗi tầng request (`INVALID_REQUEST`, `OPERATION_NOT_ALLOWED`, `RESOURCE_NOT_FOUND`) trong `GlobalException` |

**Unit test cần có:** TH1–TH5 (gồm TH4 cho tài khoản TBM, TH5 không đổi role); `amount` mặc định 20/10, `amount` nhập tay, `amount` = 0 và = 21; gom 1 lỗi và ≥ 2 lỗi; fail-fast trên ô trống; trùng email trong file (kể cả email bị lỗi định dạng không bị tính trùng); trùng SĐT trong file, trùng cả email lẫn SĐT (gom 2 lỗi); `ACCOUNT_DUPLICATED` có `Số điện thoại` ở TH1/TH2 và TH5, message gộp `Email, Số điện thoại` (SĐT của tài khoản khác, kể cả tài khoản sinh viên / đã tắt), TH5 giữ SĐT cũ của chính mình vẫn thành công, TH4 trùng SĐT vẫn chỉ báo Vàng; `ORGANIZATION_INVALID`, `ORGANIZATION_NOT_MATCH_EMAIL` (cả 2 chiều), `MAJOR_NOT_ALLOWED`, giáo viên ngoài trường không ngành (TH1 và TH5); `TEACHER_EMAIL_DOMAIN_INVALID` (`@e.tlu.edu.vn`, `@outlook.com`...); message TH5 (không đổi gì / đổi SĐT + số lượng / đổi ngành), message dòng xanh trên Excel; dòng trống; ô lỗi công thức; sai tiêu đề (gồm có thêm cột `amount`), ô gộp, file không có dữ liệu, file giả đuôi `.xlsx`; thiếu role `SUPERVISOR`. Cách 3: kích hoạt trong trường (amount về 20, giữ SĐT/đơn vị/ngành/role), ngoài trường (amount về 10), TBM đang tắt, email cũ không thuộc 2 đuôi cho phép (coi là ngoài trường), đang hoạt động sẵn (không lưu), không tìm thấy, `publicId` sai định dạng. Sửa số lượng (UTC089–UTC097): lưu số mới, `0`, số bằng số cũ vẫn thành công, `null`, số âm, `5.5`, giáo viên bị tắt (không lưu), không tìm thấy, `publicId` sai định dạng. Chi tiết giáo viên (UTC098–UTC100): tìm thấy, không tìm thấy, `publicId` sai định dạng (không query DB). Danh sách giáo viên (UTC012, UTC101–UTC105): bộ lọc được chuẩn hóa, bộ lọc rỗng truyền `null`, sort mặc định, sort `name` + `amount`, sort `majorName` + `organization`, key ngoài whitelist (`id`, `password`, `a.status`, `status`, `email`, `phone`) ném `INVALID_SORT_FIELD`.

---

## 11. VIỆC CÒN LẠI / CHƯA TRIỂN KHAI

* [ ] Lọc ngành theo Bộ môn của Trưởng bộ môn đang đăng nhập (cần thiết kế quan hệ Bộ môn – Ngành).
* [x] Sửa `amount` của giáo viên đang hoạt động (`PATCH /teacher/{publicId}/amount`, Mục 7C).
* [ ] Sửa SĐT, ngành của giáo viên đang hoạt động.
* [x] `GET /teacher/{publicId}` cho popup chi tiết; `GET /teacher` ghép tên Họ + Tên.
* [x] `GET /teacher`: whitelist sort (như `GET /student`), chuẩn hóa filter bằng `TextNormalizer`, tìm theo họ tên đầy đủ qua tham số `name` (thay `lastName`).
* [ ] `GET /teacher`: tìm thêm theo email (chưa chốt nghiệp vụ).
* [x] `GlobalException` bắt thêm lỗi tầng request, áp dụng chung cho mọi API (cả addStudent): `HttpMessageNotReadableException` (body thiếu / JSON hỏng / field sai kiểu), `HttpMediaTypeNotSupportedException`, `MultipartException` → `INVALID_REQUEST` (1006, 400); `HttpRequestMethodNotSupportedException` → `OPERATION_NOT_ALLOWED` (1007, 405); `NoResourceFoundException` / `NoHandlerFoundException` → `RESOURCE_NOT_FOUND` (1005, 404). Trước đây các lỗi này trả `500`.
* [ ] Ghi đồng thời cùng email (2 request tạo / import chạy song song) vẫn trả `500` do `DataIntegrityViolationException` từ unique constraint.
* [ ] Endpoint tải file mẫu, dropdown ngành sinh động từ DB.
* [x] Luật SĐT không trùng giữa các tài khoản (Mục 0 #12, Mục 5.2), chặn ở Service cho cả import và nhập tay.
* [ ] Dọn các SĐT đang trùng trong DB, rồi đặt `unique` cho `Account.phone` để chặn cả trường hợp ghi đồng thời (Mục 6.2). Dùng chung với addStudent.
