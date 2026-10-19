# ĐẶC TẢ KỸ THUẬT: TÍNH NĂNG THÊM DANH SÁCH SINH VIÊN (IMPORT EXCEL & NHẬP THỦ CÔNG)

> **Mục đích tài liệu:** Tài liệu này cung cấp đặc tả nghiệp vụ và kỹ thuật chi tiết cho AI Coding Agent để triển khai tính năng thêm sinh viên đầu kì dành cho **Trưởng bộ môn** (Head of Department). Yêu cầu tuân thủ chính xác 100% các quy tắc kiểm tra dữ liệu, thông báo lỗi, cơ chế lưu một phần (Partial Import) và tối ưu hiệu năng truy vấn.

> **Trạng thái:** Đã triển khai (2026-10-17). Các mục bên dưới đã được cập nhật theo code thực tế. Những chỗ khác với thiết kế ban đầu được tổng hợp ở **Mục 0**.

---

## 0. CÁC ĐIỂM THAY ĐỔI SO VỚI THIẾT KẾ BAN ĐẦU

| # | Thiết kế ban đầu | Triển khai thực tế | Lý do |
| :--- | :--- | :--- | :--- |
| 1 | Giới hạn số dòng tối đa (VD: 1.000 dòng) | **Không giới hạn** số dòng | Theo yêu cầu nghiệp vụ |
| 2 | Ngành phải thuộc quyền quản lý của Bộ môn hiện tại | Chỉ kiểm tra ngành **tồn tại trong DB**; Query 1 lấy **tất cả** `Major` | Chưa có quan hệ Bộ môn – Ngành – Trưởng bộ môn trong DB. Bổ sung sau |
| 3 | Query 3 dùng `LOWER(a.email) IN :emails` | `a.email IN :emails` (không `LOWER`) | Email luôn được lưu dạng chữ thường; collation `*_ci` của MySQL đã không phân biệt hoa/thường; bọc `LOWER()` làm mất index |
| 4 | Entity `Major` chỉ có `name` | Thêm cột `code` (unique, nullable) | Dùng làm key phụ cho `majorMap`; để nullable vì bảng `major` đã có dữ liệu cũ |
| 5 | Nhập thủ công TH2/TH3 trả `400` hoặc `409` | Chốt **`409 Conflict`** (code `3004` / `3005`) | Theo yêu cầu nghiệp vụ |
| 6 | TH4 (nhập thủ công) trả "trạng thái cảnh báo" | **HTTP 200**, mã `11xx` (hiện là `1101`, xem #11) | Mã `2001` (ví dụ ban đầu) đã bị `FORBIDDEN` dùng |
| 7 | `rowNumber` bắt đầu từ dòng dữ liệu | `rowNumber` = **số dòng thật trên Excel** (dòng tiêu đề = 1, dòng dữ liệu đầu tiên = 2) | FE/người dùng đối chiếu trực tiếp với file |
| 8 | API cũ `POST /student` nhận danh sách (seed dữ liệu mẫu) | Đã **xóa**; `POST /student` giờ nhận **1 sinh viên** (Cách 2) | Theo yêu cầu nghiệp vụ |
| 9 | Ô bị gộp trong "vùng dữ liệu" | Từ chối nếu vùng gộp bắt đầu trong **8 cột A–H**; gộp ở cột ngoài vùng (I trở đi) được bỏ qua | Làm rõ phạm vi "vùng dữ liệu" |
| 10 | Kiểm tra trùng trong file trên mọi dòng | Chỉ đếm trùng trên các ô **đã qua bước Trống và Định dạng** của cột đó | Đảm bảo fail-fast: 1 ô không sinh 2 lỗi |
| 11 | TH1/TH5 cùng báo "Thêm sinh viên thành công"; TH4 luôn báo vàng `1100`, không lưu | Message tách theo kết quả (Mục 6). Sinh viên đang hoạt động mà đổi lớp/SĐT thì **được cập nhật**; nhập y hệt thì báo vàng `1101`. Áp dụng cho **cả nhập thủ công và import Excel** (dòng thành công trong Excel cũng có message). `1100` giờ chỉ dùng cho Cách 3 | Báo "Thêm thành công" với sinh viên đã có là gây hiểu nhầm; TBM cần biết đã thêm mới, kích hoạt lại hay cập nhật field nào |

---

## 1. TỔNG QUAN NGHIỆP VỤ

Đầu mỗi học kì, **Trưởng bộ môn** thực hiện thêm danh sách sinh viên đủ điều kiện làm đồ án vào hệ thống thông qua 3 phương thức:
1. **Cách 1: Import từ file Excel (`.xlsx`)** – Hỗ trợ xử lý hàng loạt, dòng nào hợp lệ vẫn lưu vào Database bình thường, dòng nào lỗi hoặc cảnh báo sẽ trả về trạng thái kèm thông báo tại đúng dòng đó để Frontend tô màu (**Đỏ** hoặc **Vàng**).
2. **Cách 2: Nhập thủ công từng sinh viên trên Form** – Áp dụng cùng bộ quy tắc kiểm tra định dạng và đối chiếu Database như 1 dòng trong file Excel.
3. **Cách 3: Tìm trên danh sách rồi bấm Kích hoạt** – Dành cho sinh viên đã có trong hệ thống nhưng bị tắt từ kì trước; không cần nhập lại thông tin (Mục 6.1).

---

## 2. CẤU TRÚC ENTITY (DATABASE SCHEMA)

### 2.1. Entity `Account`
Lưu trữ thông tin định danh và trạng thái đăng nhập của người dùng.
* `Account.status = true`: Sinh viên được phép đăng nhập vào hệ thống.
* `Account.status = false`: Tài khoản bị thu hồi quyền đăng nhập sau khi kết thúc học kì.

```java
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String email;

    @Column(name = "first_name", nullable = false)
    String firstName; // Họ và tên đệm

    @Column(name = "last_name", nullable = false)
    String lastName; // Tên

    @Column(name = "phone", nullable = false)
    String phone; // Số điện thoại (KHÔNG unique, bắt buộc khác null)

    @Column(name = "status", nullable = false)
    Boolean status;

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

### 2.2. Entity `Student`
Lưu trữ hồ sơ học vụ của sinh viên.
* `Student.status = true`: Sinh viên đủ điều kiện tiếp tục làm đồ án trong kì hiện tại.
* `Student.status = false`: Sinh viên không làm đồ án trong kì hiện tại (đã hoàn thành, trượt hoặc tạm dừng từ kì trước).

```java
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "student_code", nullable = false, unique = true)
    String studentCode; // Mã sinh viên (Khóa tự nhiên bất biến)

    @Column(name = "batch", nullable = false)
    String batch; // Khóa học (VD: K62, K63)

    @Column(name = "class_name", nullable = false)
    String className; // Tên lớp quản lý

    @Column(name = "status", nullable = false)
    Boolean status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    Major major; // Ngành học (Thuộc Bộ môn của Trưởng bộ môn)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;
}
```

### 2.3. Entity `Major`
Danh mục ngành. Cột `Ngành` trong Excel được tra theo `name` (hoặc `code` nếu có).

```java
public class Major {
    // ... id, name, các trường hiện có

    // Mã ngành, dùng làm key khi import sinh viên
    // Để nullable vì bảng major đã có dữ liệu cũ chưa có mã
    @Column(name = "code", unique = true)
    String code;
}
```

> **Lưu ý DB (do `ddl-auto=update`):**
> * Đổi `className` → `class_name`: Hibernate chỉ **thêm** cột `class_name`, **không xóa** cột `className` cũ (đang `NOT NULL`) → phải migrate dữ liệu và xóa cột cũ thủ công, nếu không lệnh insert sẽ lỗi.
> * Tên ngành trong bảng `major` phải khớp chính xác (có dấu) với danh sách dropdown trong file mẫu. Nên bổ sung `unique` cho `Major.name` để tránh 2 ngành trùng tên bị map nhầm.

---

## 3. CẤU TRÚC CỘT TRONG FILE EXCEL & TIỀN XỬ LÝ (PRE-PROCESSING)

### 3.1. Danh sách các cột trong Template Excel
File mẫu: `tlu_gpms_system/src/main/resources/templates/student_import_template.xlsx`.

Hệ thống chỉ đọc **sheet đầu tiên**. **Dòng 1** là tiêu đề, dữ liệu bắt đầu từ **dòng 2**. File bao gồm 8 cột dữ liệu bắt buộc theo thứ tự (cột A → H):

| Cột | Tiêu đề | Field | Ghi chú |
| :--- | :--- | :--- | :--- |
| A | **Mã sinh viên** | `studentCode` | |
| B | **Họ** | `firstName` | Họ và tên đệm |
| C | **Tên** | `lastName` | Tên gọi |
| D | **Email** | `email` | |
| E | **Số điện thoại** | `phone` | |
| F | **Khóa** | `batch` | Chấp nhận cả tiêu đề "Khoá" |
| G | **Lớp** | `className` | |
| H | **Ngành** | `major` | Chọn từ Dropdown **tên ngành**; BE tra theo tên hoặc mã ngành |

Tiêu đề được so khớp sau khi chuẩn hóa (Mục 3.3) và **không phân biệt hoa/thường**.

**Cấu trúc file mẫu:**
* Sheet `Sinh viên` (sheet đầu): dòng tiêu đề cố định (Freeze), cả 8 cột định dạng **Text** (`@`) để giữ số `0` đầu.
  * Data Validation (dòng 2 → 5000): cột Ngành là dropdown; Mã SV và SĐT bắt buộc độ dài = 10. Mỗi cột có gợi ý (Input Message).
* Sheet `Hướng dẫn`: quy tắc từng cột và lưu ý khi nhập.
* Sheet `DanhMuc` (ẩn): danh sách ngành cho dropdown (named range `DanhSachNganh`):
  1. Kỹ thuật phần mềm
  2. Hệ thống thông tin
  3. Công nghệ thông tin
  4. An ninh mạng
  5. Trí tuệ nhân tạo và Khoa học dữ liệu

> Dropdown trên Excel chỉ hỗ trợ người nhập; ô vẫn lưu text và có thể bị Paste đè. Vì vậy BE **luôn** kiểm tra lại ngành với DB.

### 3.2. Kiểm tra mức toàn File (File-level Validation)
Trước khi đọc từng dòng, nếu vi phạm một trong các lỗi sau thì **từ chối toàn bộ file ngay lập tức** (`AppException`, HTTP `400`, code `4000`):

| Điều kiện | ErrorCode | Message |
| :--- | :--- | :--- |
| File null hoặc rỗng, hoặc request không có part `file` | `FILE_EMPTY` | File import không được để trống |
| File vượt `spring.servlet.multipart.max-file-size` (50MB) | `FILE_TOO_LARGE` | File import vượt quá dung lượng cho phép (50MB) |
| Không có đuôi `.xlsx` hoặc nội dung không phải OOXML thực tế (kiểm tra Magic bytes bằng `FileMagic`), hoặc POI không mở được | `FILE_INVALID_FORMAT` | File import phải có định dạng .xlsx |
| Không có sheet / dòng tiêu đề không khớp 8 cột chuẩn | `FILE_HEADER_INVALID` | Dòng tiêu đề không đúng với file mẫu |
| Có vùng gộp ô (**Merged Cells**) bắt đầu trong cột A–H | `FILE_MERGED_CELLS` | File import không được chứa ô bị gộp |
| Không còn dòng dữ liệu nào sau khi bỏ dòng trống | `FILE_NO_DATA` | File import không có dòng dữ liệu nào |

**Không giới hạn** số dòng trong 1 lần import.

### 3.3. Quy tắc Tiền xử lý dữ liệu từng ô (Cell Normalization)
* **Bỏ qua dòng trống hoàn toàn (Ghost Rows):** Nếu cả 8 cột trên cùng một dòng đều rỗng sau khi chuẩn hóa, hệ thống tự động bỏ qua dòng đó, không đưa vào danh sách kiểm tra lỗi và không tính vào `totalRows`.
* **Đọc giá trị dạng Text:** Dùng `DataFormatter` để đọc mọi ô thành chuỗi. Nhờ vậy `phone` / `studentCode` không mất số `0` ở đầu và không bị đổi sang dạng số khoa học (Scientific Notation).
* **Chuẩn hóa chuỗi** (`TextNormalizer.normalize` trong `common/utils/text`), theo thứ tự:
  1. Xóa ký tự ẩn `[​-‍⁠﻿]` (zero-width space/joiner, word joiner, BOM).
  2. Đổi NBSP (` `) thành khoảng trắng thường.
  3. Gộp mọi khoảng trắng liên tiếp (gồm tab, xuống dòng) thành 1 khoảng trắng (`replaceAll("\\s+", " ")`) rồi `trim()`.
  4. Chuẩn hóa tiếng Việt về **Unicode dựng sẵn (NFC)** (`Normalizer.normalize(str, Normalizer.Form.NFC)`).
* **Chuẩn hóa riêng theo cột:**
  * `email` → chữ thường (`toLowerCase()`).
  * `batch` → chữ hoa (VD: `k64` → `K64`).
* **Ô công thức (`CellType.FORMULA`):** Đọc **kết quả đã tính** (cached result) của công thức. Nếu công thức bị lỗi (`#N/A`, `#REF!`, `#VALUE!`, `#NAME?`, `#DIV/0!`...), giá trị ô là chuỗi lỗi đó, nên sẽ bị bắt ở bước kiểm tra định dạng (VD: Email → `EMAIL_INVALID`).

---

## 4. LUỒNG KIỂM TRA DỮ LIỆU TỪNG DÒNG (ROW-LEVEL VALIDATION PIPELINE)

Quá trình kiểm tra mỗi dòng được chia làm **2 bước tuần tự**:
* **Bước 1:** Kiểm tra Cú pháp, Định dạng và Trùng lặp nội bộ trong file Excel.
* **Bước 2:** Đối chiếu với Database (Chỉ thực hiện khi dòng đó **không có bất kỳ lỗi nào** ở Bước 1).

### 4.1. Bước 1: Kiểm tra Cú pháp, Định dạng & Trùng trong File (Fail-fast trên từng cột)

> **QUY TẮC QUAN TRỌNG (FAIL-FAST PER COLUMN):**
> Trên **cùng một cột**, hệ thống phải kiểm tra theo thứ tự ưu tiên: **(1) Kiểm tra Trống $\rightarrow$ (2) Kiểm tra Định dạng/Danh mục $\rightarrow$ (3) Kiểm tra Trùng trong file Excel**.
> Nếu một cột đã vi phạm ở bước trước (ví dụ: bị trống), hệ thống **dừng ngay việc kiểm tra các bước sau của cột đó** và chuyển sang kiểm tra cột tiếp theo. Tuyệt đối không để 1 ô bị trống sinh ra 2 lỗi (vừa lỗi trống vừa lỗi sai Regex).

| Cột dữ liệu | 1. Kiểm tra Trống | 2. Kiểm tra Định dạng & Danh mục | 3. Kiểm tra Trùng trong file Excel |
| :--- | :--- | :--- | :--- |
| **`studentCode`** | Không được để trống | Gồm **đúng 10 chữ số** (VD: `2251172489`). Regex: `^\d{10}$` | Không được trùng với dòng khác trong cùng file Excel |
| **`firstName`** | Không được để trống | Chỉ chứa chữ cái tiếng Việt, các từ cách nhau 1 khoảng trắng. Regex: `^\p{IsLatin}+( \p{IsLatin}+)*$` | *Không kiểm tra trùng* |
| **`lastName`** | Không được để trống | Một từ, chỉ chứa chữ cái tiếng Việt. Regex: `^\p{IsLatin}+$` | *Không kiểm tra trùng* |
| **`email`** | Không được để trống | Đúng định dạng email, regex (sau khi lowercase): `^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)*\.[a-z]{2,}$`. Đúng định dạng rồi mới xét đuôi: **bắt buộc kết thúc bằng `@gmail.com`** (chỉ áp dụng cho sinh viên, giáo viên không áp dụng). VD `duc@gmail.com` hợp lệ; `duc@e.tlu.edu.vn`, `duc@sub.gmail.com`, `duc@gmail.com.vn` sai đuôi; `duc@gmail` sai định dạng | Không được trùng với dòng khác trong cùng file Excel (không phân biệt hoa/thường) |
| **`phone`** | Không được để trống | SĐT Việt Nam: 10 chữ số, bắt đầu bằng `0`. Regex: `^0\d{9}$` | *Không kiểm tra trùng* |
| **`batch`** | Không được để trống | Chữ `K` + 2 chữ số (VD: `K64`, `K65`). Regex (sau khi uppercase): `^K\d{2}$` | *Không kiểm tra trùng* |
| **`className`** | Không được để trống | Chữ và số, các cụm cách nhau 1 khoảng trắng, không ký tự đặc biệt (VD: `64KTPM1`). Regex: `^[\p{L}\p{N}]+( [\p{L}\p{N}]+)*$` | *Không kiểm tra trùng* |
| **`major`** | Không được để trống | Tên ngành hoặc mã ngành phải tồn tại trong DB (so khớp không phân biệt hoa/thường). *Chưa kiểm tra thuộc Bộ môn hiện tại, xem Mục 0 #2* | *Không kiểm tra trùng* |

* Không giới hạn độ dài nghiệp vụ cho tên, lớp. BE chỉ chặn chuỗi dài quá **255 ký tự** (giới hạn `VARCHAR(255)` của DB) và coi đó là lỗi định dạng của cột, để tránh 1 dòng lỗi SQL làm hỏng cả lô.

#### Bảng ErrorCode của Bước 1 (HTTP `400`, code `4000`)

| Cột | Lỗi Trống | Lỗi Định dạng / Danh mục | Lỗi Trùng trong file |
| :--- | :--- | :--- | :--- |
| `studentCode` | `STUDENT_CODE_EMPTY` – Mã sinh viên không được để trống | `STUDENT_CODE_INVALID` – Mã sinh viên phải gồm đúng 10 chữ số | `STUDENT_CODE_DUPLICATED_IN_FILE` – Mã sinh viên bị trùng với dòng khác trong file |
| `firstName` | `FIRST_NAME_EMPTY` – Họ không được để trống | `FIRST_NAME_INVALID` – Họ chỉ được chứa chữ cái và khoảng trắng | |
| `lastName` | `LAST_NAME_EMPTY` – Tên không được để trống | `LAST_NAME_INVALID` – Tên chỉ được chứa chữ cái | |
| `email` | `EMAIL_EMPTY` – Email không được để trống | `EMAIL_INVALID` – Email không đúng định dạng<br>`STUDENT_EMAIL_DOMAIN_INVALID` – Email sinh viên phải có đuôi @gmail.com | `EMAIL_DUPLICATED_IN_FILE` – Email bị trùng với dòng khác trong file |
| `phone` | `PHONE_EMPTY` – Số điện thoại không được để trống | `PHONE_INVALID` – Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 | |
| `batch` | `BATCH_EMPTY` – Khóa học không được để trống | `BATCH_INVALID` – Khóa học không đúng định dạng (VD: K64) | |
| `className` | `CLASS_NAME_EMPTY` – Tên lớp không được để trống | `CLASS_NAME_INVALID` – Tên lớp không được chứa ký tự đặc biệt | |
| `major` | `MAJOR_EMPTY` – Ngành học không được để trống | `MAJOR_INVALID` – Ngành học không tồn tại trong hệ thống | |
| *(≥ 2 lỗi)* | `STUDENT_INFO_INVALID` – Thông tin nhập không hợp lệ, vui lòng kiểm tra lại | | |

#### Lưu ý về Kiểm tra trùng trong nội bộ file Excel (Rule 2):
* Chỉ kiểm tra trùng trên 2 cột là **`studentCode`** và **`email`**.
* Chỉ đếm trùng trên các ô **đã qua bước Trống và Định dạng** của cột đó. Ô đã có lỗi thì không tham gia đếm và không bị gắn thêm lỗi trùng.
* Nếu phát hiện $\ge 2$ dòng trong cùng file Excel có cùng `studentCode` (hoặc cùng `email`), **tất cả các dòng bị trùng đó đều bị gắn lỗi** trùng tương ứng (không tự ý lấy dòng đầu tiên).

#### Quy tắc Gom lỗi cuối Bước 1 (Error Aggregation Rule):
Sau khi kiểm tra xong cả 8 cột của một dòng, gọi tập lỗi thu được là `rowErrors` (`EnumMap<StudentColumn, ErrorCode>`, mỗi cột tối đa 1 lỗi):
* Nếu `rowErrors.size() == 1`:
  * Đánh dấu dòng là **`ERROR` (Báo Đỏ)**.
  * Message trả về: message của chính lỗi đó.
  * **Dừng xử lý dòng này** (Không chạy Bước 2).
* Nếu `rowErrors.size() >= 2`:
  * Đánh dấu dòng là **`ERROR` (Báo Đỏ)**.
  * Message trả về: `"Thông tin nhập không hợp lệ, vui lòng kiểm tra lại"`.
  * **Dừng xử lý dòng này** (Không chạy Bước 2).
* Nếu `rowErrors.isEmpty()` (0 lỗi):
  * Chuyển dòng này sang **Bước 2**.

---

### 4.2. Bước 2: Đối chiếu với Database (Database Reconciliation)

Với mỗi dòng đã vượt qua Bước 1, hệ thống thực hiện tra cứu từ Database:
* `studentByCode`: Tìm `Student` theo `studentCode`.
* `accountByEmail`: Tìm `Account` theo `email`.

Hệ thống xử lý theo **5 trường hợp rẽ nhánh loại trừ nhau** dưới đây:

#### Trường hợp 1: Sinh viên hoàn toàn mới (`studentByCode == null` VÀ `accountByEmail == null`)
* **Kết quả:** Hợp lệ – Trạng thái **`SUCCESS`**.
* **Hành động:**
  1. Tạo mới đối tượng `Account` với `email`, `firstName`, `lastName`, `phone`, `status = true`, gán Role `STUDENT` (Role được load 1 lần, nếu không có thì ném `ROLE_NOT_EXIST`).
  2. Tạo mới đối tượng `Student` với `studentCode`, `batch`, `className`, `major`, `status = true`, liên kết với `Account` vừa tạo.

#### Trường hợp 2: Xung đột định danh / Trùng tài khoản khác
Xảy ra khi:
* `studentByCode == null` nhưng `accountByEmail != null` (Email đã thuộc về một SV khác hoặc Giảng viên/Admin).
* **HOẶC** `studentByCode != null` nhưng `accountByEmail == null` (Mã SV đã tồn tại nhưng Email nhập vào không khớp).
* **HOẶC** Cả hai khác `null` nhưng trỏ tới 2 tài khoản khác nhau: `!studentByCode.getAccount().getId().equals(accountByEmail.getId())`.
* **Kết quả:** Lỗi – Trạng thái **`ERROR` (Báo Đỏ)**.
* **Message trả về:** `"Email (hoặc Mã sinh viên) đã thuộc về một tài khoản khác trong hệ thống"` (`STUDENT_IDENTITY_CONFLICT`, code `3004`, HTTP `409`).
* **Hành động:** Không lưu vào Database.

#### Trường hợp 3: Trùng khớp cặp (`studentCode`, `email`) của 1 hồ sơ nhưng LỆCH thông tin gốc (Immutable Fields)
Khi `studentByCode != null`, `accountByEmail != null` và `studentByCode.getAccount().getId().equals(accountByEmail.getId())`:
Hệ thống kiểm tra đối chiếu **4 trường thông tin cứng (bất biến)** giữa file Excel và bản ghi trong Database:
1. `Account.firstName` (chuẩn hóa giá trị trong DB rồi so khớp chính xác, **phân biệt hoa/thường**)
2. `Account.lastName` (chuẩn hóa giá trị trong DB rồi so khớp chính xác, **phân biệt hoa/thường**)
3. `Student.batch` (so sánh khớp chuỗi không phân biệt hoa/thường)
4. `Student.major.id` (so sánh khớp ID ngành học)

* Nếu **bất kỳ 1 trong 4 trường trên không khớp** với Database:
  * **Kết quả:** Lỗi – Trạng thái **`ERROR` (Báo Đỏ)**.
  * **Message trả về:** `"Thông tin không khớp với hồ sơ gốc trong hệ thống"` (`STUDENT_PROFILE_MISMATCH`, code `3005`, HTTP `409`).
  * **Hành động:** Không lưu vào Database.

#### Trường hợp 4: Khớp 100% hồ sơ gốc NHƯNG sinh viên đang hoạt động sẵn trong kì này
Khi cả 6 trường cứng (`studentCode`, `email`, `firstName`, `lastName`, `batch`, `major`) đều khớp 100% với Database, hệ thống kiểm tra trạng thái `status`:
* Nếu **`Account.status == true` VÀ `Student.status == true`**, so sánh lớp (không phân biệt hoa/thường) và SĐT với DB. **Import Excel và nhập thủ công xử lý giống nhau:**
  * Không khác gì → **`WARNING` (Báo Vàng)**, `"Sinh viên đã có trong hệ thống, thông tin không thay đổi"` (`STUDENT_NO_CHANGE`, code `1101`), không lưu.
  * Có khác → **`SUCCESS`**, cập nhật `Account.phone`, `Student.className`, message liệt kê field đã đổi (Mục 6).

#### Trường hợp 5: Khớp 100% hồ sơ gốc VÀ sinh viên cần được kích hoạt lại cho kì mới
Khi cả 6 trường cứng (`studentCode`, `email`, `firstName`, `lastName`, `batch`, `major`) đều khớp 100% với Database, và **`Account.status == false` HOẶC `Student.status == false`**:
* **Kết quả:** Hợp lệ – Trạng thái **`SUCCESS`**.
* **Hành động:**
  1. Bật `Account.status = true`.
  2. Bật `Student.status = true`.
  3. **Cho phép cập nhật thông tin thay đổi theo kì:** Cập nhật `Account.phone = excelRow.phone` và `Student.className = excelRow.className`.

---

## 5. CƠ CHẾ LƯU DỮ LIỆU & TỐI ƯU HIỆU NĂNG (BATCH PROCESSING)

### 5.1. Yêu cầu chống N+1 Query (Bắt buộc)
Tuyệt đối **KHÔNG** gọi lệnh truy vấn Database (`findByStudentCode`, `findByEmail`, `findByMajor`) bên trong vòng lặp `for` của từng dòng Excel. Phải thực hiện gom truy vấn theo Batch trước khi lặp:

1. **Query 1 (Load Majors):** `majorRepository.findAll()` lấy **tất cả** `Major` (chưa lọc theo Bộ môn, xem Mục 0 #2), đưa vào `Map<String, Major> majorMap`. Mỗi ngành có **2 key**: tên ngành viết thường, và mã ngành viết thường (nếu `code` khác rỗng). Vì vậy cột Ngành nhận được cả tên lẫn mã.
2. **Query 2 (Load Existing Students with Account & Major):**
   Thu thập tất cả `studentCode` hợp lệ ở Bước 1 vào `Set<String> studentCodes`. Thực hiện 1 câu truy vấn JPQL duy nhất có `JOIN FETCH`:
   ```sql
   SELECT s FROM Student s
   JOIN FETCH s.account
   JOIN FETCH s.major
   WHERE s.studentCode IN :studentCodes
   ```
   Đưa kết quả vào `Map<String, Student> studentByCodeMap` để tra cứu với độ phức tạp $O(1)$.
3. **Query 3 (Load Existing Accounts by Email):**
   Thu thập tất cả `email` hợp lệ ở Bước 1 vào `Set<String> emails`. Thực hiện truy vấn:
   ```sql
   SELECT a FROM Account a WHERE a.email IN :emails
   ```
   Không dùng `LOWER(a.email)`, vì 2 lý do: email đã được lưu dạng chữ thường, và collation `*_ci` của MySQL vốn không phân biệt hoa/thường. Bọc `LOWER()` sẽ làm mất index của cột `email`. Khi đưa vào map, key được chuẩn hóa lại bằng `normalizeEmail()` cho chắc.
   Đưa kết quả vào `Map<String, Account> accountByEmailMap` để tra cứu với độ phức tạp $O(1)$.
4. **Query 4 (Load Role):** `roleRepository.findById("STUDENT")`, chạy 1 lần duy nhất.

> Query 2–4 **chỉ chạy khi có ít nhất 1 dòng qua được Bước 1**. Nếu mọi dòng đều lỗi định dạng, hệ thống chỉ tốn đúng 1 query (Query 1).
>
> Repository: `StudentRepository.findAllByStudentCodeIn(Collection<String>)` và `AccountRepository.findAllByEmailIn(Collection<String>)`.

### 5.2. Quản lý Transaction cho Partial Import (Rule 4)
* Quá trình phân loại các dòng thành `accountsToSave`, `studentsToSave` và `rowResults` diễn ra hoàn toàn trên bộ nhớ (In-memory) mà không ném Exception làm đứt luồng.
* Sau khi kết thúc kiểm tra toàn bộ các dòng trong file:
  * Nếu danh sách hợp lệ (`accountsToSave`, `studentsToSave`) khác rỗng, thực hiện `accountRepository.saveAll(accountsToSave)` rồi `studentRepository.saveAll(studentsToSave)` trong cùng một Transaction (`@Transactional` trên method service).
  * TH5 (kích hoạt lại) sửa trực tiếp entity đã load, rồi cũng đưa vào 2 danh sách trên để lưu.
  * Trả về đối tượng Response chứa thống kê tổng quan và danh sách kết quả chi tiết của từng dòng cho Frontend.
* **Hạn chế đã biết:** nếu 2 lần import chạy đồng thời cùng chèn một `studentCode`/`email`, vi phạm unique constraint sẽ rollback **toàn bộ lô** và trả `500`.

---

## 6. ĐẶC TẢ CHO CÁCH 2: NHẬP THỦ CÔNG TỪNG SINH VIÊN (MANUAL ENTRY)

Khi Trưởng bộ môn thêm 1 sinh viên qua Form nhập tay, request (`StudentCreationRequest`) được chuyển thành 1 dòng (`StudentRowData.fromRequest`, `rowNumber = 0`). Dòng này đi qua **đúng cùng pipeline** với import Excel (`process()`).
* Request **không dùng** annotation `@Valid`/`@NotBlank`; mọi kiểm tra nằm trong pipeline để đảm bảo cùng thứ tự và cùng message.
* Field `major` nhận tên ngành hoặc mã ngành.
* Bước "Trùng trong file" không có tác dụng vì chỉ có 1 dòng.

Field được phép thay đổi theo kì: **Lớp** (`className`, so sánh không phân biệt hoa/thường) và **Số điện thoại** (`phone`). Message liệt kê field đã đổi theo thứ tự "Lớp, Số điện thoại".

Ví dụ: DB có SV mã `2251172489`, email `duc@gmail.com`, khóa `K64`, lớp `63KTPM1`, SĐT `0911111111`.

| Kết quả | Ví dụ TBM nhập | HTTP | `code` | Message |
| :--- | :--- | :--- | :--- | :--- |
| **TH1** (Mới) – lưu DB | Mã/email chưa có trong DB | `200` | `1000` | Thêm sinh viên thành công |
| **TH4** đang hoạt động, có thay đổi – cập nhật | Lớp `64KTPM1` | `200` | `1000` | Đã cập nhật: Lớp |
| **TH4** đang hoạt động, y hệt – Báo Vàng, không lưu | Lớp `63KTPM1`, SĐT `0911111111` | `200` | `1101` | Sinh viên đã có trong hệ thống, thông tin không thay đổi |
| **TH5** bị tắt từ kì trước, y hệt – kích hoạt lại | Lớp `63KTPM1`, SĐT `0911111111` | `200` | `1000` | Đã kích hoạt lại tài khoản sinh viên |
| **TH5** bị tắt từ kì trước, có thay đổi – kích hoạt lại + cập nhật | Lớp `64KTPM1`, SĐT `0987654321` | `200` | `1000` | Đã kích hoạt lại và cập nhật: Lớp, Số điện thoại |
| Bước 1 có đúng 1 lỗi | | `400` | `4000` | Message của lỗi cụ thể (bảng ErrorCode Mục 4.1) |
| Bước 1 có $\ge 2$ lỗi | | `400` | `4000` | Thông tin nhập không hợp lệ, vui lòng kiểm tra lại |
| **TH2** (Mã SV và email chỉ vào 2 tài khoản khác nhau) | Mã `2251172489` + email `khac@gmail.com` | `409` | `3004` | Email (hoặc Mã sinh viên) đã thuộc về một tài khoản khác trong hệ thống |
| **TH3** (Đúng mã + email nhưng lệch họ tên/khóa/ngành) | Khóa `K65` | `409` | `3005` | Thông tin không khớp với hồ sơ gốc trong hệ thống |

FE phân biệt Xanh / Vàng bằng `code` trong body (`1000` hoặc `1101`), vì cả hai đều trả HTTP `200`. Service trả `StudentCreationResult` (`code`, `message`), controller gói vào `APIResponse<Void>`.

> Import Excel dùng **cùng** các kết quả và message trên cho từng dòng: dòng `SUCCESS` có `message` "Thêm sinh viên thành công" / "Đã cập nhật: ..." / "Đã kích hoạt lại ...", dòng TH4 y hệt là `WARNING` với message `1101`. Ví dụ trong cùng 1 file: SV Đức nhập y hệt DB → Vàng "Sinh viên đã có trong hệ thống, thông tin không thay đổi"; SV An đổi lớp `63KTPM2` → `64KTPM1` → Xanh "Đã cập nhật: Lớp" và lớp mới được lưu. Thêm giáo viên **không** áp dụng.

### 6.1. Cách 3: Kích hoạt từ danh sách sinh viên

* `GET /student` trả thêm field `status` cho mỗi sinh viên: `true` khi **cả** `Account.status` và `Student.status` đều bật, ngược lại `false`. FE hiện nút **Kích hoạt** ở các dòng `status = false`.
* Bấm nút gọi `PATCH /student/{publicId}/activate`. API chỉ bật `Account.status` và `Student.status`, **giữ nguyên** lớp/SĐT. Muốn đổi lớp/SĐT thì dùng Cách 2.

| Kết quả | HTTP | `code` | Message |
| :--- | :--- | :--- | :--- |
| Sinh viên đã tắt → bật lại | `200` | `1000` | Đã kích hoạt lại tài khoản sinh viên |
| Sinh viên đang hoạt động sẵn – Báo Vàng, không lưu | `200` | `1100` | Sinh viên đã tồn tại/đang hoạt động trong kì này |
| Không tìm thấy `publicId`, hoặc `publicId` sai định dạng UUID | `404` | `3008` | Sinh viên không tồn tại |

### 6.2. Danh sách sinh viên (`GET /student`) – Giai đoạn 1

Màn hình để Trưởng bộ môn xem sinh viên làm đồ án kì này và tìm nhanh 1 sinh viên.

**Tham số (đều không bắt buộc):**

| Tham số | Ý nghĩa | Ví dụ |
| :--- | :--- | :--- |
| `keyword` | Tìm gần đúng, không phân biệt hoa/thường, theo **mã SV**, **họ tên đầy đủ** (Họ + Tên) hoặc **email**. Được trim và gộp khoảng trắng | `2251172489`, `trung đức`, `@gmail.com` |
| `batch` | Khóa, so khớp chính xác, tự viết hoa (`k63` → `K63`) | `K63` |
| `majorId` | `publicId` của ngành, lấy từ dropdown `GET /major`. Khớp chính xác | `0192f3a1-...` |
| `status` | `ACTIVE` (**mặc định**) = đang hoạt động · `INACTIVE` = đã tắt (FE hiện nút Kích hoạt) · `ALL` = tất cả | `INACTIVE` |
| `page`, `size` | Phân trang, mặc định `page=0`, `size=15` | `page=1&size=15` |
| `sort` | Chỉ nhận các key ứng với cột trên bảng: `studentCode`, `name`, `majorName`, `className`. Có thể gửi nhiều lần | `sort=className,asc&sort=name,asc` |

* `sort=name` sắp theo **Tên rồi đến Họ** (đúng thói quen tra danh sách tiếng Việt): "Nguyễn Văn An" đứng trước "Lê Trung Đức".
* Không gửi `sort`: sinh viên đang hoạt động lên trước (khi lọc `ALL`), sau đó sinh viên mới thêm lên trước.
* Mọi kiểu sort đều được chốt thêm `s.id DESC` ở cuối để phân trang không bị nhảy dòng khi nhiều sinh viên cùng lớp/cùng khóa.

**Response mỗi dòng (`StudentResponse`, bản gọn):** `publicId`, `studentCode`, `name` (Họ + Tên, ví dụ "Lê Trung Đức"), `majorName` (TBM quản lý nhiều ngành nên cần cột này), `className`, `status`. Không trả `email`, `phone`, `batch` để bảng không phình ra khi sau này thêm cột GVHD/Đề tài; các thông tin này xem ở popup. `keyword` vẫn tìm được theo email và bộ lọc `batch` vẫn dùng được.

| Lỗi | HTTP | `code` | Message |
| :--- | :--- | :--- | :--- |
| `sort` ngoài danh sách trên (ví dụ `sort=id`, `sort=password`) | `400` | `1008` | Trường sắp xếp không hợp lệ |
| `majorId` không phải UUID, hoặc `status` khác `ACTIVE`/`INACTIVE`/`ALL` | `400` | `1006` | Yêu cầu không hợp lệ |

> Ví dụ: `GET /student?keyword=đức&status=ALL&sort=name,asc` trả cả SV Đức đang hoạt động lẫn SV Đức đã tắt ở kì trước, sắp theo tên.

**Dropdown ngành – `GET /major`:** trả `List<MajorResponse>` (`publicId`, `code`, `name`), sắp theo tên ngành. Cùng quyền với `GET /student`.

**Popup chi tiết – `GET /student/{publicId}`:** FE gọi khi bấm vào 1 dòng, dùng `publicId` của dòng đó. Trả `StudentDetailResponse`: `publicId`, `studentCode`, `name`, `email`, `phone`, `majorName`, `className`, `batch`, `status`. Cùng quyền với `GET /student`.

| Lỗi | HTTP | `code` | Message |
| :--- | :--- | :--- | :--- |
| `publicId` không phải UUID hoặc không có trong DB | `404` | `3008` | Sinh viên không tồn tại |

---

## 7. THIẾT KẾ CONTRACT API

### 7.1. Endpoint

Mọi endpoint dưới đây yêu cầu quyền `hasRole('ADMIN') or hasRole('HEAD_OF_DEPARTMENT')` (`@PreAuthorize` ở `UserServiceImpl` / `MajorServiceImpl`).

| Method | URL | Request | Response `data` |
| :--- | :--- | :--- | :--- |
| `GET` | `/student` | Query `keyword`, `batch`, `majorId`, `status`, `page`, `size`, `sort` (Mục 6.2) | `Page<StudentResponse>` |
| `GET` | `/student/{publicId}` | Không có (popup chi tiết) | `StudentDetailResponse` |
| `GET` | `/major` | Không có | `List<MajorResponse>` |
| `POST` | `/student/import` | `multipart/form-data`, part `file` (.xlsx) | `StudentImportResponse` |
| `POST` | `/student` | JSON `StudentCreationRequest` (1 sinh viên) | *(không có)* |
| `PATCH` | `/student/{publicId}/activate` | Không có body | *(không có)* |

* `/student/import` luôn trả HTTP `200`, `code = 1000`, message `"Xử lý file import hoàn tất"`, kèm kết quả từng dòng. Chỉ lỗi mức toàn file (Mục 3.2) mới trả `400`.
* API cũ `POST /student` nhận `List<StudentCreationRequest>` (dùng để seed dữ liệu mẫu) **đã bị xóa**, cùng với `UserService.createStudents`.

```json
// POST /student
{
  "studentCode": "2251172489",
  "firstName": "Lê Trung",
  "lastName": "Đức",
  "email": "duc@gmail.com",
  "phone": "0987654321",
  "batch": "K64",
  "className": "64KTPM1",
  "major": "Kỹ thuật phần mềm"
}
```

### 7.2. DTO

```java
public enum RowImportStatus {
    SUCCESS, // Hợp lệ -> Đã lưu DB (Tạo mới hoặc Kích hoạt lại)
    WARNING, // Báo Vàng -> Đã active sẵn trong kì, thông tin không đổi, bỏ qua không lưu lại
    ERROR    // Báo Đỏ -> Lỗi dữ liệu hoặc xung đột DB, không lưu
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentImportRowResult {
    private int rowNumber;            // Số dòng thật trên Excel (tiêu đề = 1, dữ liệu đầu tiên = 2)
    // Các field dữ liệu bên dưới là giá trị ĐÃ CHUẨN HÓA (Mục 3.3)
    private String studentCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String batch;
    private String className;
    private String majorName;
    private RowImportStatus status;   // SUCCESS | WARNING | ERROR
    private String message;           // SUCCESS: thêm mới / kích hoạt lại / đã cập nhật field nào; WARNING, ERROR: lý do
}

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentImportResponse {
    private int totalRows;            // Tổng số dòng có dữ liệu đã xử lý (không tính dòng trống)
    private int successCount;         // Số dòng import/active thành công
    private int warningCount;         // Số dòng báo vàng
    private int errorCount;           // Số dòng báo đỏ
    private List<StudentImportRowResult> rowResults; // Chi tiết từng dòng để FE render bảng
}
```
---

## 8. CẤU TRÚC CODE ĐÃ TRIỂN KHAI

Package gốc: `com.example.tlu_gpms_system`

| File | Vai trò |
| :--- | :--- |
| `features/account/controller/UserController.java` | `GET /student`, `GET /student/{publicId}`, `POST /student/import`, `POST /student`, `PATCH /student/{publicId}/activate` |
| `features/account/dto/response/StudentResponse.java`, `repository/StudentRepository.java` (`findAllStudents`) | `GET /student`: bản gọn 6 field, họ tên đúng thứ tự Họ + Tên, lọc `keyword` / `batch` / `majorId` / `status` (Mục 6.1, 6.2) |
| `features/account/dto/response/StudentDetailResponse.java`, `repository/StudentRepository.java` (`findStudentDetail`) | `GET /student/{publicId}`: popup chi tiết, có thêm `email`, `phone`, `batch` |
| `features/account/service/impl/UserServiceImpl.java` (`toStudentPageable`) | Whitelist sort: đổi key FE (`name`, `className`...) sang đường dẫn JPQL, key lạ ném `INVALID_SORT_FIELD` |
| `common/enums/AccountStatusFilter.java` | `ACTIVE` / `INACTIVE` / `ALL` cho bộ lọc trạng thái |
| `features/account/controller/MajorController.java`, `service/MajorService.java` (+ `impl/MajorServiceImpl.java`), `dto/response/MajorResponse.java` | `GET /major` cho dropdown ngành |
| `features/account/service/UserService.java` (+ `impl/UserServiceImpl.java`) | `importStudents`, `createStudent` dùng chung pipeline `processStudentRows()`: Bước 1 → gom query → Bước 2 (`reconcileStudent`) → `saveAll` |
| `features/account/service/helper/StudentExcelReader.java` | Kiểm tra tiêu đề / ô gộp theo file mẫu sinh viên, đọc sheet đầu thành `List<StudentRowData>`, bỏ dòng trống |
| `features/account/service/helper/StudentRowValidator.java` | Bước 1: Trống → Định dạng/Danh mục cho từng cột |
| `common/utils/text/TextNormalizer.java` | Chuẩn hóa chuỗi (Mục 3.3), `toLowerKey` / `toUpperKey` |
| `common/utils/excel/ExcelUtils.java` | Dùng chung cho mọi chức năng import: mở file `.xlsx` (kiểm tra rỗng, đuôi, Magic bytes), phát hiện ô gộp, đọc dòng/ô dạng text (kể cả ô công thức) |
| `common/utils/collection/DuplicateUtils.java` | Tìm vị trí tất cả phần tử có key bị lặp (dùng cho trùng trong file) |
| `configuration/exception/GlobalException.java` | Bắt `MaxUploadSizeExceededException` → `FILE_TOO_LARGE`, `MissingServletRequestPartException` → `FILE_EMPTY`, `MethodArgumentTypeMismatchException` (tham số query sai kiểu) → `INVALID_REQUEST` |
| `features/account/service/helper/StudentColumn.java` | Enum 8 cột + tiêu đề hợp lệ |
| `features/account/service/helper/StudentRowData.java` | Dữ liệu 1 dòng đã chuẩn hóa (từ Excel hoặc từ Form) |
| `common/enums/RowImportStatus.java` | `SUCCESS` / `WARNING` / `ERROR` |
| `features/account/dto/response/StudentImportResponse.java`, `StudentImportRowResult.java` | Response Mục 7.2 |
| `features/account/dto/response/StudentCreationResult.java` | Kết quả nhập thủ công (`code`, `message`) để controller dựng `APIResponse<Void>` (Mục 6) |
| `configuration/exception/ErrorCode.java` | Các mã lỗi Mục 3.2, 4.1, 4.2 |
| `src/main/resources/templates/student_import_template.xlsx` | File Excel mẫu |
| `pom.xml` | Thêm `org.apache.poi:poi-ooxml:5.2.5` |

**Unit test:** `src/test/.../features/account/service/impl/UserServiceImplTest.java` (30 test thêm sinh viên trong các nhóm `@Nested`). Bao phủ:
* TH1–TH5, kèm message nhập thủ công: y hệt / đổi lớp / đổi lớp + SĐT / kích hoạt lại / kích hoạt lại + cập nhật.
* Import Excel với sinh viên đang hoạt động giống nhập thủ công: dòng y hệt báo vàng `1101`, dòng đổi lớp báo "Đã cập nhật: Lớp" và lưu lớp mới; dòng thêm mới có message "Thêm sinh viên thành công".
* Kích hoạt từ danh sách: đã tắt, chỉ Account tắt, đang hoạt động sẵn, không tìm thấy, `publicId` sai định dạng.
* Gom 1 lỗi và gom ≥ 2 lỗi; fail-fast trên ô trống.
* Email không có đuôi `@gmail.com` (`@e.tlu.edu.vn`, `@tlu.edu.vn`, `@sub.gmail.com`, `@gmail.com.vn`) → `STUDENT_EMAIL_DOMAIN_INVALID`; email sai định dạng (`duc@gmail`) chỉ báo `EMAIL_INVALID`.
* Trùng trong file; dòng trống; ô số và ô lỗi công thức.
* Sai tiêu đề, ô gộp, file không có dữ liệu, file giả đuôi `.xlsx`.
* Danh sách sinh viên (nhóm `StudentListTests`, 8 test): chuẩn hóa `keyword`/`batch`, tham số rỗng thành `null`, `status` ACTIVE/INACTIVE/ALL/mặc định, sort mặc định, sort theo `name` (Tên rồi Họ), nhiều sort, từ chối key ngoài whitelist (kể cả `email`, `batch`).
* Popup chi tiết (nhóm `StudentDetailTests`, 3 test): tìm thấy (có email, SĐT, khóa), không có trong DB, `publicId` sai định dạng.

---

## 9. VIỆC CÒN LẠI / CHƯA TRIỂN KHAI

* [ ] Lọc ngành theo Bộ môn của Trưởng bộ môn đang đăng nhập (cần thiết kế quan hệ Bộ môn – Ngành).
* [ ] Chốt phạm vi quyền: hiện cho phép cả `ADMIN` lẫn `HEAD_OF_DEPARTMENT`.
* [ ] Endpoint tải file mẫu, trong đó dropdown ngành được sinh động từ DB thay vì danh sách cố định trong file.
* [ ] Trả lỗi chi tiết theo từng ô (field-level errors) trong `StudentImportRowResult`. Hiện mỗi dòng chỉ có 1 `message`.
* [ ] Lưu lịch sử tham gia theo học kì (hiện chỉ bật/tắt `status`).
* [ ] Thêm `unique` cho `Major.name` và seed `Major.code`.
* [ ] Migrate/xóa cột `className` cũ trong bảng `student` (Mục 2.3).
* [ ] `GET /student` giai đoạn 2 (khi có module đề tài): thêm cột GVHD, Đề tài và bộ lọc "chưa có GVHD / chưa có đề tài" (JOIN chỉ đọc ở Repository).
* [ ] `GET /teacher` còn ghép họ tên ngược (Tên + Họ) và chưa có whitelist sort, nên sửa theo cùng cách với `GET /student`.
