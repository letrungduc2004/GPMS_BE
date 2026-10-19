# Bộ file test import sinh viên (`POST /student/import`)

- Mã SV tăng dần qua các file, từ `2251170001` đến `2251170037`. Email mặc định = `mã SV + @gmail.com`, SĐT = `09` + 8 số cuối của mã SV.
- Ngành dùng 5 ngành trong `DB_UPDATE/DATA_ROLE_PERMISSION.txt`.
- Nên import **theo thứ tự**. File 06 dùng lại sinh viên của file 01 nên phải import file 01 trước.
- Muốn chạy lại từ đầu: xóa các SV `2251170001`–`2251170031` trong DB (cả `student` và `account`).

## Lỗi theo từng dòng

| File | Dòng | Mã SV | Nội dung | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| `01_hop_le_them_moi.xlsx` | 2–6 | 0001–0005 | 5 SV hợp lệ, đủ 5 ngành | Cả 5 dòng: Thêm sinh viên thành công |
| `02_loi_dinh_dang_1_o.xlsx` | 2 | `225117000` | Mã SV 9 số | Mã sinh viên phải gồm đúng 10 chữ số |
| | 3 | 0007 | Họ `Vũ Thị 2` | Họ chỉ được chứa chữ cái và khoảng trắng |
| | 4 | 0008 | SĐT `1912345678` | Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 |
| | 5 | 0009 | Khóa `64` | Khóa học không đúng định dạng (VD: K64) |
| | 6 | 0010 | Lớp `63CNTT@1` | Tên lớp không được chứa ký tự đặc biệt |
| `03_o_trong_nganh_sai_nhieu_loi.xlsx` | 2 | 0011 | Trống email | Email không được để trống |
| | 3 | 0012 | Trống ngành | Ngành học không được để trống |
| | 4 | 0013 | Ngành `Khoa học máy tính` | Ngành học không tồn tại trong hệ thống |
| | 5 | 0014 | SĐT `12345` + khóa `K6` (2 lỗi) | Thông tin nhập không hợp lệ, vui lòng kiểm tra lại |
| `04_email_duoi_gmail.xlsx` | 2 | 0015 | `2251170015@GMAIL.COM` (chữ hoa) | Hợp lệ, lưu dạng chữ thường |
| | 3 | 0016 | `@e.tlu.edu.vn` | Email sinh viên phải có đuôi @gmail.com |
| | 4 | 0017 | `@sub.gmail.com` | Email sinh viên phải có đuôi @gmail.com |
| | 5 | 0018 | `@gmail.com.vn` | Email sinh viên phải có đuôi @gmail.com |
| | 6 | 0019 | `aulien@gmail` | Email không đúng định dạng |
| `05_trung_trong_file.xlsx` | 2, 3 | 0020 (×2) | Cùng mã SV, khác email (`caothi@gmail.com`) | Cả 2 dòng: Mã sinh viên bị trùng với dòng khác trong file |
| | 4, 5 | 0021, 0022 | Khác mã, cùng email `2251170021@gmail.com` | Cả 2 dòng: Email bị trùng với dòng khác trong file |
| | 6 | 0023 | Hợp lệ | Thêm sinh viên thành công |
| `06_doi_chieu_db_sau_file_01.xlsx` | 2 | 0001 | Y hệt file 01 | Cảnh báo vàng `1101`: Sinh viên đã có trong hệ thống, thông tin không thay đổi |
| | 3 | 0002 | Đổi lớp sang `64HTTT2` | Đã cập nhật: Lớp |
| | 4 | 0003 | Đổi lớp `63CNTT3` + SĐT `0988888888` | Đã cập nhật: Lớp, Số điện thoại |
| | 5 | 0004 | Đổi khóa K64 → K65 | `3005` Thông tin không khớp với hồ sơ gốc trong hệ thống |
| | 6 | 0005 | Mã cũ + email `trangkhac@gmail.com` | `3004` Email (hoặc Mã sinh viên) đã thuộc về một tài khoản khác trong hệ thống |
| `07_lan_hop_le_va_loi.xlsx` | 2, 4, 6 | 0024, 0026, 0028 | Hợp lệ | Thêm sinh viên thành công (vẫn lưu dù file có dòng lỗi) |
| | 3 | 0025 | Tên `Nhung Anh` (2 từ) | Tên chỉ được chứa chữ cái |
| | 5 | 0027 | Trống SĐT | Số điện thoại không được để trống |
| `08_o_so_va_chuan_hoa.xlsx` | 2 | 0029 | Mã SV nhập dạng **ô số** | Hợp lệ |
| | 3 | 0030 | Khóa `k64`, họ `␣␣Nghiêm␣␣␣Thị␣␣` | Hợp lệ, lưu `K64` và `Nghiêm Thị` |
| | 4 | 0031 | SĐT nhập dạng **ô số** (mất số 0 đầu) | Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0 |

## Lỗi toàn file (HTTP 400, code 4000, không lưu dòng nào)

| File | Nội dung | Message |
| :--- | :--- | :--- |
| `09_sai_tieu_de.xlsx` | Cột A ghi `Mã SV` (mã 0032–0034) | Dòng tiêu đề không đúng với file mẫu |
| `10_co_o_gop.xlsx` | Gộp ô Lớp + Ngành ở dòng 2 (mã 0035–0037) | File import không được chứa ô bị gộp |
| `11_chi_co_tieu_de.xlsx` | Chỉ có dòng tiêu đề | File import không có dòng dữ liệu nào |
| `12_file_gia_xlsx.xlsx` | File text đổi đuôi thành `.xlsx` | File import phải có định dạng .xlsx |

> Đã đọc lại các file bằng `StudentExcelReader` + `StudentRowValidator` của BE: kết quả kiểm tra định dạng và lỗi toàn file khớp bảng trên. Trùng trong file (05) và đối chiếu DB (06) nằm ở service nên cần chạy API thật để xem.
