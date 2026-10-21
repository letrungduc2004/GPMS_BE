# Bộ file test import giáo viên (`POST /teacher/import`)

- Email trong trường: `gv.testNN@tlu.edu.vn`, ngoài trường: `gv.ngoaiNN@gmail.com`. SĐT mặc định `03618000NN`.
- Giáo viên trong trường: đơn vị công tác phải là `Trường Đại học Thủy lợi` (không phân biệt hoa/thường) và bắt buộc có ngành. Giáo viên ngoài trường: đơn vị công tác là trường khác, bỏ trống ngành.
- Số lượng hướng dẫn mặc định: **20** cho trong trường, **10** cho ngoài trường.
- Import **theo đúng thứ tự file**. T05 cần T01 và `S01` của sinh viên. T06 cần trạng thái sau T05 và chạy thêm SQL trước khi import.
- Dòng lỗi không làm hỏng cả file: các dòng hợp lệ vẫn được lưu (HTTP 200). Chỉ lỗi toàn file mới trả HTTP 400 và không lưu dòng nào.
- Thông báo trùng tài khoản luôn có dạng `<các trường bị trùng> trùng với tài khoản đã tồn tại trong hệ thống` (code `3014`), các trường xếp theo thứ tự `Email, Số điện thoại`.

## Chạy lại từ đầu

```sql
DELETE ur FROM user_role ur JOIN account a ON a.id = ur.user_id
WHERE a.email LIKE 'gv.test%@tlu.edu.vn' OR a.email LIKE 'gv.ngoai%@gmail.com';
DELETE t FROM teacher t JOIN account a ON a.id = t.account_id
WHERE a.email LIKE 'gv.test%@tlu.edu.vn' OR a.email LIKE 'gv.ngoai%@gmail.com';
DELETE FROM account WHERE email LIKE 'gv.test%@tlu.edu.vn' OR email LIKE 'gv.ngoai%@gmail.com';
```

## Lỗi theo từng dòng

| ID | File | Dòng | Email | Dữ liệu test | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- | :--- |
| TI01 | `T01_hop_le_them_moi.xlsx` | 2, 3, 5 | gv.test01, 02, 04 | Trong trường, đủ ngành | Thêm giáo viên thành công, số lượng hướng dẫn 20 |
| TI02 | | 4 | gv.test03 | Đơn vị viết thường `trường đại học thủy lợi` | Thêm giáo viên thành công, lưu `Trường Đại học Thủy lợi`, số lượng 20 |
| TI03 | | 6, 7 | gv.ngoai05, 06 | Ngoài trường, ngành trống | Thêm giáo viên thành công, số lượng hướng dẫn 10 |
| TI04 | `T02_loi_dinh_dang_tung_o.xlsx` | 2 | gv.test07 | Họ `Nguyễn 2` | Họ chỉ được chứa chữ cái và khoảng trắng |
| TI05 | | 3 | gv.test08 | Tên `Văn An` (2 từ) | Tên chỉ được chứa chữ cái |
| TI06 | | 4 | `gv.test09@tlu` | Email sai định dạng | Email không đúng định dạng |
| TI07 | | 5 | `gv.test10@e.tlu.edu.vn` | Đuôi email sinh viên | Email giáo viên phải có đuôi @tlu.edu.vn (trong trường) hoặc @gmail.com (ngoài trường) |
| TI08 | | 6 | gv.test11 | SĐT `361800011` | Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 |
| TI09 | | 7 | gv.ngoai12 | Đơn vị `Đại học #1` | Đơn vị công tác chỉ gồm chữ, số, khoảng trắng và các dấu . , - & ( ) ' / (tối đa 255 ký tự) |
| TI10 | | 8 | gv.test13 | Email trong trường + đơn vị `Đại học Bách khoa Hà Nội` | Đơn vị công tác không khớp với email (email @tlu.edu.vn phải là Trường Đại học Thủy lợi, email @gmail.com phải là trường khác) |
| TI11 | | 9 | gv.ngoai14 | Email ngoài trường + đơn vị `Trường Đại học Thủy lợi` | Đơn vị công tác không khớp với email (...) |
| TI12 | | 10 | gv.ngoai15 | Ngoài trường nhưng điền ngành | Giáo viên ngoài trường không được điền ngành |
| TI13 | | 11 | gv.test16 | Ngành `Khoa học máy tính` | Ngành học không tồn tại trong hệ thống |
| TI14 | `T03_o_trong_va_nhieu_loi.xlsx` | 2 | gv.test17 | Trống họ | Họ không được để trống |
| TI15 | | 3 | gv.test18 | Trống tên | Tên không được để trống |
| TI16 | | 4 | (trống) | Trống email | Email không được để trống |
| TI17 | | 5 | gv.test20 | Trống SĐT | Số điện thoại không được để trống |
| TI18 | | 6 | gv.test21 | Trống đơn vị công tác | Đơn vị công tác không được để trống |
| TI19 | | 7 | gv.test22 | Trong trường, trống ngành | Ngành học không được để trống |
| TI20 | | 8 | `gv.test23@tlu` | Email sai + trống ngành (chưa biết loại GV nên bỏ qua ô ngành) | Email không đúng định dạng |
| TI21 | | 9 | gv.test24 | SĐT `0123` + trống ngành (2 lỗi) | Số điện thoại và Ngành không hợp lệ, vui lòng kiểm tra lại |
| TI22 | `T04_trung_trong_file.xlsx` | 2, 3 | gv.test25 (×2) | Cùng email, dòng 3 viết hoa `GV.TEST25@TLU.EDU.VN` | Cả 2 dòng: Email bị trùng với dòng khác trong file |
| TI23 | | 4, 5 | gv.test27, 28 | Cùng SĐT `0361800027` | Cả 2 dòng: Số điện thoại bị trùng với dòng khác trong file |
| TI24 | | 6 | gv.test29 | Hợp lệ | Thêm giáo viên thành công |
| TI25 | `T08_tieu_de_ho_viet_tat.xlsx` | 2 | gv.test35 | Tiêu đề cột A ghi `HỌ` (thay cho `Họ và tên đệm`), tiêu đề hoa/thường lẫn lộn | File hợp lệ. Dòng 2: Thêm giáo viên thành công |

### Đối chiếu DB: `T05_doi_chieu_db_sau_T01_S01.xlsx` (import T01 và S01 của sinh viên trước)

| ID | Dòng | Email | Dữ liệu test | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| TI26 | 2 | gv.test01 | Y hệt T01 | Cảnh báo `1100`: Thông tin giáo viên đã có trong hệ thống (số lượng 20) |
| TI27 | 3 | gv.test02 | Tên viết hoa `TRẦN THỊ HOA`, đổi SĐT và ngành | Cảnh báo `1100`: Thông tin giáo viên đã có trong hệ thống. DB **không** đổi SĐT/ngành |
| TI28 | 4 | gv.test03 | Email cũ nhưng tên khác (`Khôi`), SĐT mới | Email trùng với tài khoản đã tồn tại trong hệ thống |
| TI29 | 5 | gv.test31 | Email mới + SĐT của gv.test04 | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| TI30 | 6 | gv.ngoai05 | Email + SĐT của gv.ngoai05 nhưng tên khác (`Yên`) | Email, Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| TI31 | 7 | `2251180015@gmail.com` | Email của SV 0015 (tài khoản sinh viên), SĐT mới | Email trùng với tài khoản đã tồn tại trong hệ thống |
| TI32 | 8 | gv.test33 | Email mới + SĐT `0951180012` của SV 0012 | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống |
| TI33 | 9 | gv.test34 | Giáo viên mới hợp lệ | Thêm giáo viên thành công |

### Kích hoạt lại: `T06_kich_hoat_lai_sau_T05.xlsx` (import T01, T05 trước)

Trước khi import, tắt 5 giáo viên và đổi số lượng hướng dẫn của gv.test03:

```sql
UPDATE account SET status = 0
WHERE email IN ('gv.test01@tlu.edu.vn', 'gv.test02@tlu.edu.vn', 'gv.test03@tlu.edu.vn',
                'gv.test04@tlu.edu.vn', 'gv.ngoai06@gmail.com');
UPDATE teacher t JOIN account a ON a.id = t.account_id
SET t.amount = 5
WHERE a.email = 'gv.test03@tlu.edu.vn';
```

| ID | Dòng | Email | Dữ liệu test (so với DB) | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| TI34 | 2 | gv.test01 | Y hệt DB | Đã kích hoạt lại tài khoản giáo viên |
| TI35 | 3 | gv.test02 | Đổi SĐT → `0361800098`, ngành `Hệ thống thông tin` → `Công nghệ thông tin` | Đã kích hoạt lại và cập nhật: Số điện thoại, Ngành |
| TI36 | 4 | gv.test03 | Y hệt DB, nhưng DB đang có số lượng 5 | Đã kích hoạt lại và cập nhật: Số lượng hướng dẫn (đặt lại về 20) |
| TI37 | 5 | gv.ngoai06 | Đổi đơn vị → `Đại học Xây dựng` | Đã kích hoạt lại và cập nhật: Đơn vị công tác (số lượng 10) |
| TI38 | 6 | gv.test04 | Đổi SĐT sang `0361800005` của gv.ngoai05 | Số điện thoại trùng với tài khoản đã tồn tại trong hệ thống. gv.test04 vẫn ở trạng thái tắt |

## Lỗi toàn file (HTTP 400, code 4000, không lưu dòng nào)

| ID | File | Nội dung | Message |
| :--- | :--- | :--- | :--- |
| TI39 | `T07_sai_tieu_de_thua_cot.xlsx` | Thêm cột thứ 7 `Số lượng hướng dẫn` | Dòng tiêu đề không đúng với file mẫu |
| TI40 | `T09_co_o_gop.xlsx` | Gộp ô Đơn vị công tác ở dòng 2–3 | File import không được chứa ô bị gộp |
| TI41 | `T10_chi_co_tieu_de.xlsx` | Chỉ có dòng tiêu đề | File import không có dòng dữ liệu nào |
| TI42 | `T11_file_gia_xlsx.xlsx` | File text đổi đuôi thành `.xlsx` | File import phải có định dạng .xlsx |
| TI43 | `T12_file_rong_0_byte.xlsx` | File 0 byte | File import không được để trống |

> Đã đọc lại các file bằng `TeacherExcelReader` + `TeacherRowValidator` của BE, kèm bước kiểm tra trùng trong file giống service. Kết quả định dạng, trùng trong file và lỗi toàn file khớp bảng trên. Đối chiếu DB (T05, T06) được suy ra từ `UserServiceImpl` nên cần chạy API thật để xác nhận.
