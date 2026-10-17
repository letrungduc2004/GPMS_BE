# ĐẶC TẢ KỸ THUẬT: TÍNH NĂNG THÊM DANH SÁCH SINH VIÊN (IMPORT EXCEL & NHẬP THỦ CÔNG)

> **Mục đích tài liệu:** Tài liệu này cung cấp đặc tả nghiệp vụ và kỹ thuật chi tiết cho AI Coding Agent để triển khai tính năng thêm sinh viên đầu kì dành cho **Trưởng bộ môn** (Head of Department). Yêu cầu tuân thủ chính xác 100% các quy tắc kiểm tra dữ liệu, thông báo lỗi, cơ chế lưu một phần (Partial Import) và tối ưu hiệu năng truy vấn.

---

## 1. TỔNG QUAN NGHIỆP VỤ

Đầu mỗi học kì, **Trưởng bộ môn** thực hiện thêm danh sách sinh viên đủ điều kiện làm đồ án vào hệ thống thông qua 2 phương thức:
1. **Cách 1: Import từ file Excel (`.xlsx`)** – Hỗ trợ xử lý hàng loạt, dòng nào hợp lệ vẫn lưu vào Database bình thường, dòng nào lỗi hoặc cảnh báo sẽ trả về trạng thái kèm thông báo tại đúng dòng đó để Frontend tô màu (**Đỏ** hoặc **Vàng**).
2. **Cách 2: Nhập thủ công từng sinh viên trên Form** – Áp dụng cùng bộ quy tắc kiểm tra định dạng và đối chiếu Database như 1 dòng trong file Excel.

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

---

## 3. CẤU TRÚC CỘT TRONG FILE EXCEL & TIỀN XỬ LÝ (PRE-PROCESSING)

### 3.1. Danh sách các cột trong Template Excel
File Excel bao gồm 8 cột dữ liệu bắt buộc theo thứ tự:
1. **Mã sinh viên** (`studentCode`)
2. **Họ** (`firstName` - Họ và tên đệm)
3. **Tên** (`lastName` - Tên gọi)
4. **Email** (`email`)
5. **Số điện thoại** (`phone`)
6. **Khóa** (`batch`)
7. **Lớp** (`className`)
8. **Ngành** (`major` - Chọn từ Dropdown Mã ngành / Tên ngành trên Excel)

### 3.2. Kiểm tra mức toàn File (File-level Validation)
Trước khi đọc từng dòng, nếu vi phạm một trong các lỗi sau thì **từ chối toàn bộ file ngay lập tức** (Throw BadRequestException):
* File null, rỗng hoặc không đúng định dạng `.xlsx` thực tế (kiểm tra MIME type / Magic bytes).
* Sai cấu trúc dòng tiêu đề (Header không khớp với Template chuẩn).
* Vùng dữ liệu chứa ô bị gộp (**Merged Cells**).
* File không có dòng dữ liệu nào hoặc vượt quá giới hạn số dòng tối đa cho phép trong 1 lần import (ví dụ: 1.000 dòng).

### 3.3. Quy tắc Tiền xử lý dữ liệu từng ô (Cell Normalization)
* **Bỏ qua dòng trống hoàn toàn (Ghost Rows):** Nếu cả 8 cột trên cùng một dòng đều rỗng sau khi `trim()`, hệ thống tự động bỏ qua dòng đó, không đưa vào danh sách kiểm tra lỗi.
* **Đọc giá trị dạng Text:** Sử dụng `DataFormatter` hoặc ép kiểu đọc chuỗi để không bị mất số `0` ở đầu của `phone` / `studentCode` và không bị biến thành số khoa học (Scientific Notation).
* **Chuẩn hóa chuỗi:**
  * Thực hiện `trim()` khoảng trắng 2 đầu, xóa ký tự ẩn (*Zero-width space*, tab, xuống dòng).
  * Gộp nhiều khoảng trắng liên tiếp ở giữa thành 1 khoảng trắng đơn (`replaceAll("\\s+", " ")`).
  * Chuẩn hóa bảng mã tiếng Việt về chuẩn **Unicode dựng sẵn (NFC)** (`Normalizer.normalize(str, Normalizer.Form.NFC)`).
  * Chuyển `email` về chữ thường (`toLowerCase()`).
* **Kiểm tra lỗi công thức Excel:** Nếu ô là kiểu `CellType.FORMULA` bị lỗi (`#N/A`, `#REF!`, `#VALUE!`, `#NAME?`), coi như dữ liệu ô đó sai định dạng.

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
| **`studentCode`** | Không được để trống | Đúng chuẩn Regex Mã SV, không chứa khoảng trắng, độ dài hợp lệ | Không được trùng với dòng khác trong cùng file Excel (so sánh không phân biệt hoa/thường) |
| **`firstName`** | Không được để trống | Chỉ chứa chữ cái tiếng Việt và khoảng trắng, không chứa số hoặc ký tự đặc biệt | *Không kiểm tra trùng* |
| **`lastName`** | Không được để trống | Chỉ chứa chữ cái tiếng Việt, không chứa số hoặc ký tự đặc biệt | *Không kiểm tra trùng* |
| **`email`** | Không được để trống | Đúng chuẩn Regex Email hợp lệ | Không được trùng với dòng khác trong cùng file Excel (so sánh không phân biệt hoa/thường) |
| **`phone`** | Không được để trống | Đúng chuẩn SĐT Việt Nam (10 chữ số, bắt đầu bằng số `0`, Regex: `^0\d{9}$`) | *Không kiểm tra trùng* |
| **`batch`** | Không được để trống | Không chứa ký tự đặc biệt, nằm trong độ dài cho phép | *Không kiểm tra trùng* |
| **`className`** | Không được để trống | Không chứa ký tự đặc biệt, nằm trong độ dài cho phép | *Không kiểm tra trùng* |
| **`major`** | Không được để trống | Mã/Tên ngành phải tồn tại trong DB **VÀ** thuộc quyền quản lý của Bộ môn hiện tại | *Không kiểm tra trùng* |

#### Lưu ý về Kiểm tra trùng trong nội bộ file Excel (Rule 2):
* Chỉ kiểm tra trùng trên 2 cột là **`studentCode`** và **`email`**.
* Nếu phát hiện $\ge 2$ dòng trong cùng file Excel có cùng `studentCode` (hoặc cùng `email`), **tất cả các dòng bị trùng đó đều bị gắn lỗi** trùng tương ứng (không tự ý lấy dòng đầu tiên).

#### Quy tắc Gom lỗi cuối Bước 1 (Error Aggregation Rule):
Sau khi kiểm tra xong cả 8 cột của một dòng, gọi danh sách lỗi thu được là `rowErrors`:
* Nếu `rowErrors.size() == 1`:
  * Đánh dấu dòng là **`ERROR` (Báo Đỏ)**.
  * Message trả về: `rowErrors.get(0)` (In ra chính xác tên lỗi cụ thể đó).
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
  1. Tạo mới đối tượng `Account` với `email`, `firstName`, `lastName`, `phone`, `status = true`, gán Role `STUDENT`.
  2. Tạo mới đối tượng `Student` với `studentCode`, `batch`, `className`, `major`, `status = true`, liên kết với `Account` vừa tạo.

#### Trường hợp 2: Xung đột định danh / Trùng tài khoản khác
Xảy ra khi:
* `studentByCode == null` nhưng `accountByEmail != null` (Email đã thuộc về một SV khác hoặc Giảng viên/Admin).
* **HOẶC** `studentByCode != null` nhưng `accountByEmail == null` (Mã SV đã tồn tại nhưng Email nhập vào không khớp).
* **HOẶC** Cả hai khác `null` nhưng trỏ tới 2 tài khoản khác nhau: `!studentByCode.getAccount().getId().equals(accountByEmail.getId())`.
* **Kết quả:** Lỗi – Trạng thái **`ERROR` (Báo Đỏ)**.
* **Message trả về:** `"Email (hoặc Mã sinh viên) đã thuộc về một tài khoản khác trong hệ thống"`.
* **Hành động:** Không lưu vào Database.

#### Trường hợp 3: Trùng khớp cặp (`studentCode`, `email`) của 1 hồ sơ nhưng LỆCH thông tin gốc (Immutable Fields)
Khi `studentByCode != null`, `accountByEmail != null` và `studentByCode.getAccount().getId().equals(accountByEmail.getId())`:
Hệ thống kiểm tra đối chiếu **4 trường thông tin cứng (bất biến)** giữa file Excel và bản ghi trong Database:
1. `Account.firstName` (so sánh khớp chuỗi sau chuẩn hóa)
2. `Account.lastName` (so sánh khớp chuỗi sau chuẩn hóa)
3. `Student.batch` (so sánh khớp chuỗi không phân biệt hoa/thường)
4. `Student.major.id` (so sánh khớp ID ngành học)

* Nếu **bất kỳ 1 trong 4 trường trên không khớp** với Database:
  * **Kết quả:** Lỗi – Trạng thái **`ERROR` (Báo Đỏ)**.
  * **Message trả về:** `"Thông tin không khớp với hồ sơ gốc trong hệ thống"`.
  * **Hành động:** Không lưu vào Database.

#### Trường hợp 4: Khớp 100% hồ sơ gốc NHƯNG sinh viên đang hoạt động sẵn trong kì này
Khi cả 6 trường cứng (`studentCode`, `email`, `firstName`, `lastName`, `batch`, `major`) đều khớp 100% với Database, hệ thống kiểm tra trạng thái `status`:
* Nếu **`Account.status == true` VÀ `Student.status == true`**:
  * **Kết quả:** Cảnh báo – Trạng thái **`WARNING` (Báo Vàng)**.
  * **Message trả về:** `"Sinh viên đã tồn tại/đang hoạt động trong kì này"`.
  * **Hành động:** Bỏ qua, không cần insert/update lại vào Database.

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

1. **Query 1 (Load Majors):** Lấy danh sách tất cả `Major` thuộc Bộ môn của Trưởng bộ môn đang đăng nhập, đưa vào `Map<String, Major> majorMap` (key là mã/tên ngành đã viết thường).
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
   SELECT a FROM Account a WHERE LOWER(a.email) IN :emails
   ```
   Đưa kết quả vào `Map<String, Account> accountByEmailMap` để tra cứu với độ phức tạp $O(1)$.

### 5.2. Quản lý Transaction cho Partial Import (Rule 4)
* Quá trình phân loại các dòng thành `accountsToSave`, `studentsToSave` và `rowResults` diễn ra hoàn toàn trên bộ nhớ (In-memory) mà không ném Exception làm đứt luồng.
* Sau khi kết thúc kiểm tra toàn bộ các dòng trong file:
  * Nếu danh sách hợp lệ (`accountsToSave`, `studentsToSave`) khác rỗng, thực hiện `accountRepository.saveAll(accountsToSave)` và `studentRepository.saveAll(studentsToSave)` trong một Transaction.
  * Trả về đối tượng Response chứa thống kê tổng quan và danh sách kết quả chi tiết của từng dòng cho Frontend.

---

## 6. ĐẶC TẢ CHO CÁCH 2: NHẬP THỦ CÔNG TỪNG SINH VIÊN (MANUAL ENTRY)

Khi Trưởng bộ môn thêm 1 sinh viên qua Form nhập tay:
1. **Tiền xử lý & Kiểm tra định dạng:** Áp dụng đúng quy tắc chuẩn hóa chuỗi và kiểm tra từng trường (Trống $\rightarrow$ Định dạng/Danh mục) như Bước 1 của Excel.
   * Nếu có $1$ lỗi $\rightarrow$ Trả về lỗi cụ thể đó.
   * Nếu có $\ge 2$ lỗi $\rightarrow$ Trả về `"Thông tin nhập không hợp lệ, vui lòng kiểm tra lại"`.
2. **Đối chiếu Database:** Áp dụng giống hệt **5 trường hợp ở Bước 2 (Mục 4.2)**:
   * Nếu rơi vào **Trường hợp 1 (Mới)** hoặc **Trường hợp 5 (Kích hoạt lại)** $\rightarrow$ Lưu vào DB và trả về `200 OK`.
   * Nếu rơi vào **Trường hợp 2 (Trùng tài khoản khác)** hoặc **Trường hợp 3 (Lệch hồ sơ gốc)** $\rightarrow$ Trả về lỗi Đỏ (`400 Bad Request` hoặc `409 Conflict` kèm đúng message quy định).
   * Nếu rơi vào **Trường hợp 4 (Đang hoạt động sẵn)** $\rightarrow$ Trả về trạng thái Cảnh báo Vàng kèm message `"Sinh viên đã tồn tại/đang hoạt động trong kì này"`.

---

## 7. THIẾT KẾ CONTRACT API RESPONSE (GỢI Ý CHO BACKEND & FRONTEND)

```java
public enum RowImportStatus {
    SUCCESS, // Hợp lệ -> Đã lưu DB (Tạo mới hoặc Kích hoạt lại)
    WARNING, // Báo Vàng -> Đã active sẵn trong kì, bỏ qua không lưu lại
    ERROR    // Báo Đỏ -> Lỗi dữ liệu hoặc xung đột DB, không lưu
}

@Data
@Builder
public class StudentImportRowResult {
    private int rowNumber;            // Số thứ tự dòng trong file Excel (bắt đầu từ dòng dữ liệu)
    private String studentCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String batch;
    private String className;
    private String majorName;
    private RowImportStatus status;   // SUCCESS | WARNING | ERROR
    private String message;           // null nếu SUCCESS; chuỗi thông báo nếu WARNING hoặc ERROR
}

@Data
@Builder
public class StudentImportResponse {
    private int totalRows;            // Tổng số dòng có dữ liệu đã xử lý
    private int successCount;         // Số dòng import/active thành công
    private int warningCount;         // Số dòng báo vàng
    private int errorCount;           // Số dòng báo đỏ
    private List<StudentImportRowResult> rowResults; // Chi tiết từng dòng để FE render bảng
}
```