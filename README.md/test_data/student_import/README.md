# Bộ file test import sinh viên (`POST /student/import`)

- Mã SV dùng dải `2251180001`–`2251180060`. Mặc định email = `mã SV + @gmail.com`, SĐT = `09` + 8 số cuối của mã SV (VD: `2251180001` → `0951180001`).
- Ngành dùng 5 ngành trong `tlu_gpms_system/db_update/DATA_ROLE_PERMISSION.txt`.
- Import **theo đúng thứ tự file**. S07 cần dữ liệu của S01, S08 cần trạng thái sau S07 và chạy thêm SQL trước khi import.
- `T05` (giáo viên) dùng email/SĐT của sinh viên trong S01, nên phải import S01 trước T05.
- Dòng lỗi không làm hỏng cả file: các dòng hợp lệ vẫn được lưu (HTTP 200). Chỉ lỗi toàn file mới trả HTTP 400 và không lưu dòng nào.
- Thông báo trùng tài khoản luôn có dạng `<các trường bị trùng> trùng với tài khoản đã tồn tại trong hệ thống` (code `3014`), các trường xếp theo thứ tự `Mã sinh viên, Email, Số điện thoại`.

## Chạy lại từ đầu

```sql
DELETE ur FROM user_role ur JOIN account a ON a.id = ur.user_id
WHERE a.email LIKE '22511800%@gmail.com';
DELETE s FROM student s JOIN account a ON a.id = s.account_id
WHERE a.email LIKE '22511800%@gmail.com';
DELETE FROM account WHERE email LIKE '22511800%@gmail.com';
```

> Nếu đã chạy `T05` của giáo viên thì xóa giáo viên trước (xem README của `teacher_import`).

## Lỗi theo từng dòng

| ID | File | Dòng | Mã SV | Dữ liệu test | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- | :--- |
| SI01 | `S01_hop_le_them_moi.xlsx` | 2–16 | 0001–0015 | 15 SV hợp lệ, xoay vòng đủ 5 ngành | Cả 15 dòng: Thêm sinh viên thành công |
| SI02 | `S02_loi_dinh_dang_tung_o.xlsx` | 2 | `225118001` | Mã SV 9 số | Mã sinh viên phải gồm đúng 10 chữ số |
| SI03 | | 3 | 0017 | Họ `Vũ Thị 2` | Họ chỉ được chứa chữ cái và khoảng trắng |
| SI04 | | 4 | 0018 | Tên `Nhung Anh` (2 từ) | Tên chỉ được chứa chữ cái |
| SI05 | | 5 | 0019 | Email `2251180019@gmail` | Email không đúng định dạng |
| SI06 | | 6 | 0020 | SĐT `1951180020` | Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 |
| SI07 | | 7 | 0021 | Khóa `64` | Khóa học không đúng định dạng (VD: K64) |
| SI08 | | 8 | 0022 | Lớp `64CNTT@1` | Tên lớp không được chứa ký tự đặc biệt |
| SI09 | | 9 | 0023 | Ngành `Khoa học máy tính` | Ngành học không tồn tại trong hệ thống |
| SI10 | `S03_o_trong_va_nhieu_loi.xlsx` | 2 | (trống) | Trống mã SV | Mã sinh viên không được để trống |
| SI11 | | 3 | 0025 | Trống họ | Họ không được để trống |
| SI12 | | 4 | 0026 | Trống tên | Tên không được để trống |
| SI13 | | 5 | 0027 | Trống email | Email không được để trống |
| SI14 | | 6 | 0028 | Trống SĐT | Số điện thoại không được để trống |
| SI15 | | 7 | 0029 | Trống khóa | Khóa học không được để trống |
| SI16 | | 8 | 0030 | Trống lớp | Tên lớp không được để trống |
| SI17 | | 9 | 0031 | Trống ngành | Ngành học không được để trống |
| SI18 | | 10 | 0032 | SĐT `12345` + khóa `K6` (2 lỗi) | Số điện thoại và Khóa không hợp lệ, vui lòng kiểm tra lại |
| SI19 | `S04_email_duoi_gmail.xlsx` | 2 | 0033 | `2251180033@GMAIL.COM` (chữ hoa) | Thêm sinh viên thành công, email lưu chữ thường |
| SI20 | | 3 | 0034 | `@e.tlu.edu.vn` | Email sinh viên phải có đuôi @gmail.com |
| SI21 | | 4 | 0035 | `@sub.gmail.com` | Email sinh viên phải có đuôi @gmail.com |
| SI22 | | 5 | 0036 | `@gmail.com.vn` | Email sinh viên phải có đuôi @gmail.com |
| SI23 | | 6 | 0037 | `nguyen van@gmail.com` (có dấu cách) | Email không đúng định dạng |
| SI24 | `S05_trung_trong_file.xlsx` | 2, 3 | 0038 (×2) | Cùng mã SV, khác email và SĐT | Cả 2 dòng: Mã sinh viên bị trùng với dòng khác trong file |
| SI25 | | 4, 5 | 0040, 0041 | Cùng email, dòng 5 viết hoa `2251180040@GMAIL.COM` | Cả 2 dòng: Email bị trùng với dòng khác trong file |
| SI26 | | 6, 7 | 0042, 0043 | Cùng SĐT `0951180042` | Cả 2 dòng: Số điện thoại bị trùng với dòng khác trong file |
| SI27 | | 8 | 0044 | Hợp lệ | Thêm sinh viên thành công |
| SI28 | | 9, 10 | 0045 (×2) | Trùng cả mã SV và email (2 lỗi) | Cả 2 dòng: Mã sinh viên và Email không hợp lệ, vui lòng kiểm tra lại |
| SI29 | `S06_o_so_va_chuan_hoa.xlsx` | 2 | 0046 | Mã SV nhập dạng **ô số** | Thêm sinh viên thành công |
| SI30 | | 3 | 0047 | Khóa `k64`, họ `␣␣Nghiêm␣␣␣Thị␣␣` | Thêm sinh viên thành công, lưu `K64` và `Nghiêm Thị` |
| SI31 | | 4 | — | Dòng trống hoàn toàn | Bị bỏ qua, không có trong kết quả |
| SI32 | | 5 | 0048 | SĐT nhập dạng **ô số** (mất số 0 đầu → `951180048`) | Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 |
| SI33 | | 6 | 0049 | Ngành viết thường `kỹ thuật phần mềm` | Thêm sinh viên thành công |
| SI34 | `S13_tieu_de_hoa_thuong_khoa_cu.xlsx` | 2 | 0060 | Tiêu đề viết hoa/thường lẫn lộn, cột khóa ghi `Khoá` | File hợp lệ. Dòng 2: Thêm sinh viên thành công |

### Đối chiếu DB: `S07_doi_chieu_db_sau_S01.xlsx` (import S01 trước)

| ID | Dòng | Mã SV | Dữ liệu test | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| SI35 | 2 | 0001 | Y hệt S01 | Cảnh báo `1101`: Sinh viên đã có trong hệ thống, thông tin không thay đổi |
| SI36 | 3 | 0002 | Đổi lớp `64HTTT1` → `64HTTT2` | Đã cập nhật: Lớp |
| SI37 | 4 | 0003 | Đổi lớp → `64CNTT3` và SĐT → `0988180003` | Đã cập nhật: Lớp, Số điện thoại |
| SI38 | 5 | 0004 | Đổi SĐT sang `0951180015` (SĐT của SV 0015) | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| SI39 | 6 | 0005 | Đổi khóa K64 → K65 (thông tin bất biến) | Mã sinh viên, Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| SI40 | 7 | 0006 | Họ viết thường `vũ thị` (so khớp phân biệt hoa/thường) | Mã sinh viên, Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| SI41 | 8 | 0007 | Mã cũ + email `2251180050@gmail.com` + SĐT `0951180050` mới | Mã sinh viên trùng với tài khoản đã tồn tại trong hệ thống |
| SI42 | 9 | 0051 | Mã mới + email của SV 0008 + SĐT mới | Email trùng với tài khoản đã tồn tại trong hệ thống |
| SI43 | 10 | 0052 | Mã mới + email mới + SĐT của SV 0009 | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| SI44 | 11 | 0010 | Mã + email của SV 0010, đổi tên `Lan` → `Lanh`, SĐT mới | Mã sinh viên, Email trùng với tài khoản đã tồn tại trong hệ thống |
| SI45 | 12 | 0011 | Mã + SĐT của SV 0011, email mới | Mã sinh viên, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| SI46 | 13 | 0055 | Mã mới + email + SĐT của SV 0012 | Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| SI47 | 14 | 0013 | Mã của SV 0013 + email của SV 0014 + SĐT của SV 0013 | Mã sinh viên, Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| SI48 | 15 | 0056 | SV mới hợp lệ | Thêm sinh viên thành công |

### Kích hoạt lại: `S08_kich_hoat_lai_sau_S07.xlsx` (import S01, S07 trước)

Trước khi import, tắt 4 sinh viên:

```sql
UPDATE account a JOIN student s ON s.account_id = a.id
SET a.status = 0, s.status = 0
WHERE s.student_code IN ('2251180001', '2251180002', '2251180003', '2251180014');
```

| ID | Dòng | Mã SV | Dữ liệu test (so với DB sau S07) | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| SI49 | 2 | 0001 | Y hệt DB | Đã kích hoạt lại tài khoản sinh viên |
| SI50 | 3 | 0002 | Giữ lớp `64HTTT2`, đổi SĐT → `0988180002` | Đã kích hoạt lại và cập nhật: Số điện thoại |
| SI51 | 4 | 0003 | Đổi lớp `64CNTT3` → `64CNTT1`, giữ SĐT `0988180003` | Đã kích hoạt lại và cập nhật: Lớp |
| SI52 | 5 | 0014 | Đổi SĐT sang `0951180015` (của SV 0015) | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống. SV 0014 vẫn ở trạng thái tắt |

## Lỗi toàn file (HTTP 400, code 4000, không lưu dòng nào)

| ID | File | Nội dung | Message |
| :--- | :--- | :--- | :--- |
| SI53 | `S09_sai_tieu_de.xlsx` | Cột A ghi `Mã SV` (mã 0057) | Dòng tiêu đề không đúng với file mẫu |
| SI54 | `S10_co_o_gop.xlsx` | Gộp ô Lớp + Ngành ở dòng 2 (mã 0058–0059) | File import không được chứa ô bị gộp |
| SI55 | `S11_chi_co_tieu_de.xlsx` | Chỉ có dòng tiêu đề | File import không có dòng dữ liệu nào |
| SI56 | `S12_file_gia_xlsx.xlsx` | File text đổi đuôi thành `.xlsx` | File import phải có định dạng .xlsx |
| SI57 | `S14_file_rong_0_byte.xlsx` | File 0 byte | File import không được để trống |

> Đã đọc lại các file bằng `StudentExcelReader` + `StudentRowValidator` của BE, kèm bước kiểm tra trùng trong file giống service. Kết quả định dạng, trùng trong file và lỗi toàn file khớp bảng trên. Đối chiếu DB (S07, S08) được suy ra từ `UserServiceImpl` nên cần chạy API thật để xác nhận.
