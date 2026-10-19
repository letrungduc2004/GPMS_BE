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
| 3 | `amount` | **Import Excel:** không có cột, BE luôn tự gán theo email (20 / 10), kể cả khi kích hoạt lại (ghi đè giá trị kì trước). **Nhập thủ công:** FE tự điền theo đuôi email, TBM được sửa lại trong khoảng **1 – 20** (cả giáo viên trong và ngoài trường); không truyền thì BE tự gán. |
| 3b | Đơn vị công tác | Chỉ nhận đúng 2 giá trị **`Trong trường`** / **`Ngoài trường`** (dropdown trên Excel), và phải **khớp với email**: `@tlu.edu.vn` ⇔ `Trong trường`. |
| 3c | Cuối kì | Hệ thống chỉ tắt `Account.status`, **không** gỡ role. Kích hoạt lại (TH5) không cần thêm lại role. |
| 3d | Giáo viên đang hoạt động (TH4) | Báo Vàng **"Thông tin giáo viên đã có trong hệ thống"** cho cả import Excel và nhập thủ công. Không cập nhật gì, kể cả `amount`. Sửa `amount` của giáo viên đang hoạt động sẽ là chức năng "Sửa giáo viên" riêng. |
| 4 | Số dòng tối đa | **Không giới hạn** (giống addStudent). |
| 5 | Dung lượng file tối đa | **50MB** (`spring.servlet.multipart.max-file-size` / `max-request-size`). |
| 6 | Lọc ngành theo Bộ môn | **Chưa làm**: hệ thống chưa có entity Bộ môn, `Major` chỉ liên kết `Faculty`. Tạm nhận mọi ngành có trong DB (giống addStudent). |
| 7 | Quyền gọi API | `hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')`, đặt `@PreAuthorize` ở `UserServiceImpl` (giống addStudent). |
| 8 | API cũ `POST /teacher` nhận `List<TeacherCreationRequest>` | **Thay thế** bằng API nhận 1 giáo viên. Xóa `UserService.createTeachers` và 2 test cũ của nó. |
| 9 | Cách 3: bấm **Kích hoạt** trên danh sách | Bật `Account.status = true` và **đặt lại `amount` về mặc định theo email** (20 / 10). Giữ nguyên SĐT, đơn vị, ngành, role. Cho phép cả tài khoản TBM. Đang hoạt động sẵn → Báo Vàng `TEACHER_ALREADY_ACTIVE` (dùng lại câu của cách 1, 2). Chi tiết Mục 7B. |
| 10 | `GET /teacher` | **Không sửa** trong lần này (vẫn lọc theo `lastName`, `organization`, `majorName`). Response đã có `publicId` và `status` để FE hiện nút Kích hoạt. |

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
    String phone; // Số điện thoại (KHÔNG unique, cho phép cập nhật lại mỗi kì)

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
* **Giáo viên ngoài trường:** mọi tên miền khác → `amount = 10L`.
  * Lưu ý: email sinh viên `@e.tlu.edu.vn` **không** khớp `@tlu.edu.vn` (ký tự trước `tlu` là `.` chứ không phải `@`), nên được tính là ngoài trường.

Email quyết định cả đơn vị công tác lẫn `amount` mặc định, nên gom vào 1 enum `TeacherOrganization`:

```java
public enum TeacherOrganization {
    INTERNAL("Trong trường", 20L),
    EXTERNAL("Ngoài trường", 10L);

    // email đã được chuẩn hóa về chữ thường ở Mục 4.3
    public static TeacherOrganization ofEmail(String normalizedEmail) {
        return normalizedEmail.endsWith("@tlu.edu.vn") ? INTERNAL : EXTERNAL;
    }
}
```

### 3.2. Nguồn giá trị theo từng cách thêm

| Cách thêm | TH1 (Tạo mới) | TH5 (Kích hoạt lại) |
| :--- | :--- | :--- |
| **Import Excel** (không có cột `amount`) | mặc định theo email | mặc định theo email (ghi đè giá trị kì trước) |
| **Nhập thủ công**, có truyền `amount` | `request.amount` | `request.amount` |
| **Nhập thủ công**, không truyền `amount` (`null`) | mặc định theo email | mặc định theo email |
| **Kích hoạt từ danh sách** (cách 3) | *(không áp dụng)* | mặc định theo email (ghi đè giá trị kì trước) |

* Nếu file Excel có thêm cột `amount` (cột thứ 7) → sai cấu trúc tiêu đề (`FILE_HEADER_INVALID`).
* Khi nhập thủ công có truyền `amount`, giá trị phải là **số nguyên từ 1 đến 20**, áp dụng cho **cả** giáo viên trong và ngoài trường (mặc định ngoài trường vẫn là 10). Sai → `AMOUNT_INVALID`. Kiểm tra trong pipeline (Mục 5.1), không dùng annotation.
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
| E | **Đơn vị công tác** | `organization` | Dropdown `Trong trường` / `Ngoài trường` |
| F | **Ngành** | `major` | Dropdown **tên ngành**; BE tra theo tên hoặc mã ngành |

Tiêu đề được so khớp sau khi chuẩn hóa (Mục 4.3) và **không phân biệt hoa/thường**.

**Cấu trúc file mẫu** (giống file mẫu sinh viên):
* Sheet `Giáo viên` (sheet đầu): dòng tiêu đề cố định (Freeze), cả 6 cột định dạng **Text** (`@`) để giữ số `0` đầu. Data Validation: cột Đơn vị công tác và cột Ngành là dropdown, SĐT độ dài = 10.
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
* **Chuẩn hóa riêng theo cột:** `email` → chữ thường. `organization` khớp (không phân biệt hoa/thường) với `trong trường` / `ngoài trường` → đổi về đúng chuỗi chuẩn `Trong trường` / `Ngoài trường` để lưu DB.
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
| **`email`** | Không được để trống | Đúng định dạng email (không bắt buộc đuôi `@tlu.edu.vn`). Regex (sau khi lowercase): `^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,}$` | Không được trùng với dòng khác trong cùng file |
| **`phone`** | Không được để trống | SĐT Việt Nam: 10 chữ số, bắt đầu bằng `0`. Regex: `^0\d{9}$` | *Không kiểm tra trùng* |
| **`organization`** | Không được để trống | (a) Chỉ nhận `Trong trường` hoặc `Ngoài trường` (không phân biệt hoa/thường). (b) Phải khớp với email: `@tlu.edu.vn` ⇔ `Trong trường` | *Không kiểm tra trùng* |
| **`major`** | Không được để trống | Tên ngành hoặc mã ngành phải tồn tại trong DB (không phân biệt hoa/thường). *Chưa lọc theo Bộ môn, xem Mục 0 #6* | *Không kiểm tra trùng* |
| **`amount`** *(chỉ nhập thủ công)* | Được bỏ trống (→ tự gán, Mục 3) | Số nguyên từ 1 đến 20 | *Không áp dụng* |

* Mọi cột chuỗi dài quá **255 ký tự** (`VARCHAR(255)`) bị coi là lỗi định dạng của cột đó, để 1 dòng không làm lỗi SQL cả lô.
* Kiểm tra (b) của `organization` chỉ chạy khi cả ô `email` và ô `organization` đã hợp lệ ở các bước trước. Lệch → lỗi gắn vào cột `organization` (VD: `@gmail.com` mà ghi `Trong trường` → Báo Đỏ).

#### Bảng ErrorCode của Giai đoạn 1 (HTTP `400`, code `4000`)

| Cột | Lỗi Trống | Lỗi Định dạng / Danh mục | Lỗi Trùng trong file |
| :--- | :--- | :--- | :--- |
| `firstName` | `FIRST_NAME_EMPTY` – Họ không được để trống | `FIRST_NAME_INVALID` – Họ chỉ được chứa chữ cái và khoảng trắng | |
| `lastName` | `LAST_NAME_EMPTY` – Tên không được để trống | `LAST_NAME_INVALID` – Tên chỉ được chứa chữ cái | |
| `email` | `EMAIL_EMPTY` – Email không được để trống | `EMAIL_INVALID` – Email không đúng định dạng | `EMAIL_DUPLICATED_IN_FILE` – Email bị trùng với dòng khác trong file |
| `phone` | `PHONE_EMPTY` – Số điện thoại không được để trống | `PHONE_INVALID` – Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 | |
| `organization` | `ORGANIZATION_EMPTY` – Đơn vị công tác không được để trống | (a) **(mới)** `ORGANIZATION_INVALID` – Đơn vị công tác chỉ nhận Trong trường hoặc Ngoài trường<br>(b) **(mới)** `ORGANIZATION_NOT_MATCH_EMAIL` – Đơn vị công tác không khớp với email (email @tlu.edu.vn là Trong trường, email khác là Ngoài trường) | |
| `major` | `MAJOR_EMPTY` – Ngành học không được để trống | `MAJOR_INVALID` – Ngành học không tồn tại trong hệ thống | |
| `amount` | | `AMOUNT_INVALID` – Số lượng sinh viên hướng dẫn phải từ 1 đến 20 *(sửa message)* | |
| *(≥ 2 lỗi)* | **(mới)** `TEACHER_INFO_INVALID` – Thông tin nhập không hợp lệ, vui lòng kiểm tra lại | | |

#### Lưu ý về Kiểm tra trùng email trong file
* Chỉ đếm trùng trên các ô `email` **đã qua bước Trống và Định dạng**. Ô đã có lỗi thì không tham gia đếm và không bị gắn thêm lỗi trùng. Dùng `DuplicateUtils.findDuplicateIndexes`.
* Nếu một `email` xuất hiện $\ge 2$ lần trong cùng file, **tất cả các dòng mang email đó đều bị gắn lỗi trùng** (không tự ý lấy dòng đầu tiên).

#### Quy tắc Gom lỗi cuối Giai đoạn 1 (Error Aggregation Rule)
Sau khi kiểm tra xong các cột của một dòng, gọi tập lỗi thu được là `rowErrors` (`EnumMap<TeacherColumn, ErrorCode>`, mỗi cột tối đa 1 lỗi):
* `rowErrors.size() == 1` → **`ERROR` (Báo Đỏ)**, message là message của chính lỗi đó. Không chạy Giai đoạn 2.
* `rowErrors.size() >= 2` → **`ERROR` (Báo Đỏ)**, message `"Thông tin nhập không hợp lệ, vui lòng kiểm tra lại"` (`TEACHER_INFO_INVALID`). Không chạy Giai đoạn 2.
* `rowErrors.isEmpty()` → chuyển sang **Giai đoạn 2**.

---

### 5.2. Giai đoạn 2: Đối chiếu với Database (Database Reconciliation)

Với mỗi dòng đã vượt qua Giai đoạn 1, hệ thống tra cứu theo `email` (dữ liệu đã load sẵn ở Mục 6.1):
* `existingAccount`: Bản ghi `Account` tìm theo `email`.
* `existingTeacher`: Bản ghi `Teacher` liên kết với `existingAccount` (`teacher.account.id == existingAccount.id`).

Hệ thống xử lý theo **5 trường hợp rẽ nhánh loại trừ nhau** dưới đây:

#### Trường hợp 1: Giáo viên hoàn toàn mới (`existingAccount == null`)
* **Kết quả:** Hợp lệ – Trạng thái **`SUCCESS`**.
* **Hành động:**
  1. Tạo mới `Account` với `email`, `firstName`, `lastName`, `phone`, `status = true`, gán Role **`SUPERVISOR`** (Role được load 1 lần; nếu không có trong DB thì ném `ROLE_NOT_EXIST` và từ chối cả request).
  2. Tạo mới `Teacher` với `organization`, `major = resolvedMajor`, `amount` (theo Mục 3.2), `account = newAccount`.

#### Trường hợp 2: Email đã thuộc tài khoản khác (`existingAccount != null` NHƯNG `existingTeacher == null`)
Email đã tồn tại trong bảng `Account` nhưng tài khoản đó thuộc về Sinh viên, Admin hoặc người dùng khác không có hồ sơ trong bảng `Teacher`.
* **Kết quả:** Lỗi – Trạng thái **`ERROR` (Báo Đỏ)**.
* **Message:** `"Email đã thuộc về một tài khoản khác trong hệ thống"` (**(mới)** `TEACHER_IDENTITY_CONFLICT`, code `3006`, HTTP `409`).
* **Hành động:** Không lưu vào Database.

#### Trường hợp 3: Đã là Giáo viên nhưng LỆCH thông tin gốc (Immutable Fields)
Khi `existingAccount != null` VÀ `existingTeacher != null`, so sánh **2 trường cứng** giữa dữ liệu nhập và `Account` trong DB:
1. `firstName`
2. `lastName`

Cách so sánh: chuẩn hóa **cả giá trị trong DB** bằng `TextNormalizer.normalize`, rồi so khớp **không phân biệt hoa/thường** (`equalsIgnoreCase`).
> Khác addStudent (so khớp phân biệt hoa/thường). Đây là chủ ý: tên giáo viên thường được nhập tay bởi nhiều người, chỉ lệch hoa/thường thì vẫn coi là cùng một người.

* Nếu **`firstName` HOẶC `lastName` không khớp**:
  * **Kết quả:** Lỗi – Trạng thái **`ERROR` (Báo Đỏ)**.
  * **Message:** `"Thông tin không khớp với hồ sơ gốc trong hệ thống"` (**(mới)** `TEACHER_PROFILE_MISMATCH`, code `3007`, HTTP `409`).
  * **Hành động:** Không lưu vào Database.

#### Trường hợp 4: Khớp trường cứng NHƯNG đang hoạt động sẵn (`existingAccount.status == true`)
Bao gồm: giáo viên đã được active trong kì này (import lại lần 2, hoặc đã nhập thủ công trước đó), **và** dòng đó là tài khoản của **Trưởng bộ môn** (TBM có bản ghi `teacher` và luôn có `status == true`, xem Mục 0 #2).
* **Kết quả:** Cảnh báo – Trạng thái **`WARNING` (Báo Vàng)**.
* **Message:** `"Thông tin giáo viên đã có trong hệ thống"` (**(mới)** `TEACHER_ALREADY_ACTIVE`, code `1100`, HTTP `200`). Dùng chung cho import Excel và nhập thủ công.
* **Hành động:** Bỏ qua, **không** cập nhật gì vào DB, kể cả khi `phone`, `organization`, `major`, `amount` trong dữ liệu nhập khác DB. Muốn sửa giáo viên đang hoạt động thì dùng chức năng "Sửa giáo viên" (chưa làm).

#### Trường hợp 5: Khớp trường cứng VÀ cần kích hoạt lại (`existingAccount.status == false`)
* **Kết quả:** Hợp lệ – Trạng thái **`SUCCESS`**.
* **Hành động:**
  1. `existingAccount.setStatus(true)`. Không đụng tới role (cuối kì chỉ tắt `status`, role vẫn giữ nguyên).
  2. Cập nhật **4 trường mềm** cho kì mới:
     * `existingAccount.setPhone(row.getPhone())`
     * `existingTeacher.setOrganization(row.getOrganization())`
     * `existingTeacher.setMajor(resolvedMajor)`
     * `existingTeacher.setAmount(...)` theo Mục 3.2

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
4. **Query 4 (Load Role):** `roleRepository.findById("SUPERVISOR")`, chạy 1 lần.

> Query 2–4 chỉ chạy khi có ít nhất 1 dòng qua được Giai đoạn 1.

### 6.2. Quản lý Transaction cho Partial Import
* Phân loại các dòng thành `accountsToSave`, `teachersToSave`, `rowResults` hoàn toàn trên bộ nhớ, không ném Exception làm đứt luồng.
* Kết thúc vòng lặp: nếu danh sách khác rỗng thì `accountRepository.saveAll(accountsToSave)` rồi `teacherRepository.saveAll(teachersToSave)` trong cùng 1 Transaction (`@Transactional` trên method service).
* TH5 sửa trực tiếp entity đã load, rồi cũng đưa vào 2 danh sách trên.
* **Hạn chế đã biết:** 2 lần import chạy đồng thời cùng chèn một `email` → vi phạm unique, rollback **toàn bộ lô** và trả `500`.

---

## 7. ĐẶC TẢ CHO CÁCH 2: NHẬP THỦ CÔNG TỪNG GIÁO VIÊN (MANUAL ENTRY)

Request `TeacherCreationRequest` được chuyển thành 1 dòng (`TeacherRowData.fromRequest`, `rowNumber = 0`) và đi qua **đúng cùng pipeline** với import Excel.
* Request **không dùng** `@Valid`/`@NotBlank`/`@NotNull`/`@Min`: mọi kiểm tra nằm trong pipeline, để có cùng thứ tự và cùng quy tắc gom lỗi (`GlobalException` chỉ trả lỗi đầu tiên của annotation).
* Field `major` (String) nhận **tên hoặc mã ngành**, thay cho `majorId` cũ (không để lộ id nội bộ).
* Field `amount` (`Long`, không bắt buộc), xem Mục 3.2.
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
    Long amount;    // null → BE tự gán theo email
}
```

| Kết quả | HTTP | `code` | Message |
| :--- | :--- | :--- | :--- |
| **TH1** (Mới) / **TH5** (Kích hoạt lại) – lưu DB | `200` | `1000` | Thêm giáo viên thành công |
| **TH4** (Đang hoạt động sẵn / TBM) – Báo Vàng, không lưu | `200` | `1100` | Thông tin giáo viên đã có trong hệ thống |
| Giai đoạn 1 có đúng 1 lỗi | `400` | `4000` | Message của lỗi cụ thể (bảng ErrorCode Mục 5.1) |
| Giai đoạn 1 có $\ge 2$ lỗi | `400` | `4000` | Thông tin nhập không hợp lệ, vui lòng kiểm tra lại |
| **TH2** (Email thuộc tài khoản khác) | `409` | `3006` | Email đã thuộc về một tài khoản khác trong hệ thống |
| **TH3** (Lệch hồ sơ gốc) | `409` | `3007` | Thông tin không khớp với hồ sơ gốc trong hệ thống |
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

## 8. THIẾT KẾ CONTRACT API

### 8.1. Endpoint

Cả 3 endpoint yêu cầu quyền `hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')` (`@PreAuthorize` ở `UserServiceImpl`). Không có quyền → `403 FORBIDDEN`.

| Method | URL | Request | Response `data` |
| :--- | :--- | :--- | :--- |
| `POST` | `/teacher/import` | `multipart/form-data`, part `file` (.xlsx) | `TeacherImportResponse` |
| `POST` | `/teacher` | JSON `TeacherCreationRequest` (1 giáo viên) | *(không có)* |
| `PATCH` | `/teacher/{publicId}/activate` | *(không có body)* | *(không có)* |

* `/teacher/import` luôn trả HTTP `200`, `code = 1000`, message `"Xử lý file import hoàn tất"`, kèm kết quả từng dòng. Chỉ lỗi mức toàn file (Mục 4.2) hoặc `ROLE_NOT_EXIST` mới không trả `200`.
* API cũ `POST /teacher` nhận `List<TeacherCreationRequest>` **bị thay thế** (Mục 0 #8).

```json
// POST /teacher
{
  "firstName": "Nguyễn Văn",
  "lastName": "An",
  "email": "an.nv@tlu.edu.vn",
  "phone": "0987654321",
  "organization": "Trong trường",
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
    private String message;           // null nếu SUCCESS; chuỗi thông báo nếu WARNING hoặc ERROR
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
| `ORGANIZATION_INVALID` | `4000` | `400` | Đơn vị công tác chỉ nhận Trong trường hoặc Ngoài trường |
| `ORGANIZATION_NOT_MATCH_EMAIL` | `4000` | `400` | Đơn vị công tác không khớp với email (email @tlu.edu.vn là Trong trường, email khác là Ngoài trường) |
| `TEACHER_INFO_INVALID` | `4000` | `400` | Thông tin nhập không hợp lệ, vui lòng kiểm tra lại |
| `TEACHER_IDENTITY_CONFLICT` | `3006` | `409` | Email đã thuộc về một tài khoản khác trong hệ thống |
| `TEACHER_PROFILE_MISMATCH` | `3007` | `409` | Thông tin không khớp với hồ sơ gốc trong hệ thống |
| `TEACHER_ALREADY_ACTIVE` | `1100` | `200` | Thông tin giáo viên đã có trong hệ thống |
| `TEACHER_NOT_FOUND` | `3009` | `404` | Giáo viên không tồn tại |

Sửa message: `AMOUNT_INVALID` → "Số lượng sinh viên hướng dẫn phải từ 1 đến 20" (chỉ giáo viên dùng mã này).

Các mã khác dùng lại: `FILE_*`, `FIRST_NAME_*`, `LAST_NAME_*`, `EMAIL_*`, `PHONE_*`, `ORGANIZATION_EMPTY`, `MAJOR_EMPTY`, `MAJOR_INVALID`, `ROLE_NOT_EXIST`. Sau khi làm xong có thể xóa `MAJOR_ID_NULL`, `AMOUNT_NULL` nếu không còn chỗ nào dùng.

---

## 10. CẤU TRÚC CODE DỰ KIẾN

| File | Vai trò |
| :--- | :--- |
| `features/account/controller/UserController.java` | `POST /teacher/import`, `POST /teacher`, `PATCH /teacher/{publicId}/activate` |
| `features/account/service/UserService.java` (+ `impl/UserServiceImpl.java`) | `importTeachers(MultipartFile)`, `createTeacher(TeacherCreationRequest)` dùng chung `processTeacherRows()`; `activateTeacher(String publicId)` cho cách 3 |
| `features/account/service/helper/TeacherExcelReader.java` | Kiểm tra tiêu đề / ô gộp, đọc sheet đầu thành `List<TeacherRowData>` |
| `features/account/service/helper/TeacherRowValidator.java` | Giai đoạn 1 |
| `features/account/service/helper/TeacherColumn.java`, `TeacherRowData.java` | Enum 6 cột (+ `AMOUNT` cho nhập thủ công), dữ liệu 1 dòng |
| `features/account/service/helper/TeacherOrganization.java` | 2 giá trị đơn vị công tác, suy ra từ email, kèm `amount` mặc định |
| `features/account/repository/TeacherRepository.java` | `findAllByAccountIdIn` (Query 3) |
| `features/account/dto/response/TeacherImportResponse.java`, `TeacherImportRowResult.java` | Response Mục 8.2 |
| `src/main/resources/templates/teacher_import_template.xlsx` | File Excel mẫu |
| Dùng lại | `ExcelUtils`, `TextNormalizer`, `DuplicateUtils`, `RowImportStatus`, handler `FILE_TOO_LARGE` / `FILE_EMPTY` trong `GlobalException` |

**Unit test cần có:** TH1–TH5 (gồm TH4 cho tài khoản TBM, TH5 không đổi role); `amount` mặc định 20/10, `amount` nhập tay, `amount` = 0 và = 21; gom 1 lỗi và ≥ 2 lỗi; fail-fast trên ô trống; trùng email trong file (kể cả email bị lỗi định dạng không bị tính trùng); `ORGANIZATION_INVALID`, `ORGANIZATION_NOT_MATCH_EMAIL`; dòng trống; ô lỗi công thức; sai tiêu đề (gồm có thêm cột `amount`), ô gộp, file không có dữ liệu, file giả đuôi `.xlsx`; thiếu role `SUPERVISOR`. Cách 3: kích hoạt trong trường (amount về 20, giữ SĐT/đơn vị/ngành/role), ngoài trường (amount về 10), TBM đang tắt, đang hoạt động sẵn (không lưu), không tìm thấy, `publicId` sai định dạng.

---

## 11. VIỆC CÒN LẠI / CHƯA TRIỂN KHAI

* [ ] Lọc ngành theo Bộ môn của Trưởng bộ môn đang đăng nhập (cần thiết kế quan hệ Bộ môn – Ngành).
* [ ] Chức năng "Sửa giáo viên" (đổi `amount`, SĐT, ngành của giáo viên đang hoạt động).
* [ ] Chặn email miền sinh viên `@e.tlu.edu.vn` khi thêm giáo viên (hiện được tính là giáo viên `Ngoài trường` nếu chưa có tài khoản).
* [ ] Bắt `HttpMessageNotReadableException` (body JSON rỗng/sai) trong `GlobalException`, hiện đang trả `500`. Áp dụng chung cho cả addStudent.
* [ ] Endpoint tải file mẫu, dropdown ngành sinh động từ DB.
